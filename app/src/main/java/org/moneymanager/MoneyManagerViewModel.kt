package org.moneymanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.moneymanager.data.ApiException
import org.moneymanager.data.GrowthApi
import org.moneymanager.data.MoneyManagerApi
import org.moneymanager.data.SessionStore
import org.moneymanager.model.Budget
import org.moneymanager.model.BudgetRequest
import org.moneymanager.model.Category
import org.moneymanager.model.InvestmentPortfolio
import org.moneymanager.model.InvestmentPriceRequest
import org.moneymanager.model.InvestmentSchedule
import org.moneymanager.model.InvestmentScheduleRequest
import org.moneymanager.model.InvestmentTrade
import org.moneymanager.model.InvestmentTradeRequest
import org.moneymanager.model.NotificationPreferences
import org.moneymanager.model.Transaction
import org.moneymanager.model.TransactionRequest
import org.moneymanager.model.TransactionSchedule
import org.moneymanager.model.TransactionScheduleRequest
import org.moneymanager.model.TransactionSummary

class MoneyManagerViewModel(
    private val apiClient: MoneyManagerApi,
    private val tokenStore: SessionStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val growthApi: GrowthApi? = apiClient as? GrowthApi,
) : ViewModel() {
    private val monthCache = mutableMapOf<String, MonthSnapshot>()
    private var authJob: Job? = null
    private var monthLoadJob: Job? = null
    private var categoryLoadJob: Job? = null
    private var sessionSupervisor = SupervisorJob(viewModelScope.coroutineContext[Job])
    private var sessionScope = CoroutineScope(viewModelScope.coroutineContext + sessionSupervisor)

    private val _state = MutableStateFlow(
        MoneyManagerUiState(
            token = tokenStore.getToken(),
            signedInEmail = tokenStore.getEmail(),
            hidePortfolioBalances = tokenStore.getHidePortfolioBalances(),
            appearance = AppAppearance.fromStorage(tokenStore.getAppearanceMode()),
        ),
    )
    val state: StateFlow<MoneyManagerUiState> = _state.asStateFlow()

    private val growthActions = GrowthViewModelActions(
        api = growthApi,
        tokenStore = tokenStore,
        ioDispatcher = ioDispatcher,
        appScope = viewModelScope,
        sessionScope = { sessionScope },
        state = { state.value },
        updateState = { transform -> _state.update(transform) },
        handleSessionExpiry = ::handleSessionExpiry,
        sessionIsCurrent = ::sessionIsCurrent,
    )

    private val transactionActions = TransactionViewModelActions(
        api = apiClient,
        ioDispatcher = ioDispatcher,
        sessionScope = { sessionScope },
        state = { state.value },
        updateState = { transform -> _state.update(transform) },
        refreshCategories = ::loadCategoriesNow,
        reloadMonth = { month, forceRefresh -> loadMonth(month, forceRefresh) },
        invalidateMonth = { month -> monthCache.remove(month) },
        refreshMonth = ::refresh,
        handleSessionExpiry = ::handleSessionExpiry,
        sessionIsCurrent = ::sessionIsCurrent,
    )

    init {
        refreshHealth()
        if (_state.value.token != null) {
            loadCategories()
            loadMonth(_state.value.month)
            growthActions.refreshInvestments()
        }
    }

    fun updateEmail(value: String) = _state.update { it.copy(email = value, authError = null) }

    fun updatePassword(value: String) = _state.update { it.copy(password = value, authError = null) }

    fun updateConfirmPassword(value: String) = _state.update { it.copy(confirmPassword = value, authError = null) }

    fun toggleAuthMode() {
        if (state.value.isAuthLoading) return
        _state.update {
            it.copy(
                isRegisterMode = !it.isRegisterMode,
                password = "",
                confirmPassword = "",
                authError = null,
            )
        }
    }

    fun submitAuth() {
        val current = state.value
        if (current.isAuthLoading || authJob?.isActive == true) return
        validateAuth(current)?.let { message ->
            _state.update { it.copy(authError = message) }
            return
        }

        authJob = viewModelScope.launch {
            _state.update { it.copy(isAuthLoading = true, authError = null) }
            try {
                val email = current.email.trim()
                val result = withContext(ioDispatcher) {
                    if (current.isRegisterMode) {
                        apiClient.register(email, current.password)
                    } else {
                        apiClient.login(email, current.password)
                    }
                }
                tokenStore.saveSession(result.token, result.user.email)
                _state.update {
                    MoneyManagerUiState(
                        token = result.token,
                        signedInEmail = result.user.email,
                        hidePortfolioBalances = it.hidePortfolioBalances,
                        appearance = it.appearance,
                        connectionStatus = it.connectionStatus,
                        connectionMessage = it.connectionMessage,
                    )
                }
                loadCategories()
                loadMonth(state.value.month)
                growthActions.refreshInvestments()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _state.update { it.copy(authError = userMessage(error)) }
            } finally {
                _state.update { it.copy(isAuthLoading = false) }
                authJob = null
            }
        }
    }

    fun logout() {
        unregisterPushDevice()
        clearSession()
    }

    fun deleteAccount() {
        val token = state.value.token ?: return
        sessionScope.launch {
            _state.update { it.copy(isAccountDeleting = true, profileError = null) }
            try {
                withContext(ioDispatcher) { apiClient.deleteAccount(token) }
                if (sessionIsCurrent(token)) clearSession()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!handleSessionExpiry(error, token) && sessionIsCurrent(token)) {
                    _state.update { it.copy(profileError = userMessage(error)) }
                }
            } finally {
                if (sessionIsCurrent(token)) {
                    _state.update { it.copy(isAccountDeleting = false) }
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _state.update { it.copy(selectedTab = tab, growthDestination = null, profileError = null) }
        if (tab == AppTab.Profile) refreshHealth()
        if (tab == AppTab.Investments) growthActions.refreshInvestments()
    }

    fun setHidePortfolioBalances(hidden: Boolean) {
        tokenStore.saveHidePortfolioBalances(hidden)
        _state.update { it.copy(hidePortfolioBalances = hidden) }
    }

    fun setAppearance(appearance: AppAppearance) {
        tokenStore.saveAppearanceMode(appearance.storageValue)
        _state.update { it.copy(appearance = appearance) }
    }

    fun openGrowthDestination(destination: GrowthDestination) = growthActions.openDestination(destination)

    fun closeGrowthDestination() = growthActions.closeDestination()

    fun refreshPlanning() = growthActions.refreshPlanning()

    fun refreshInvestments() = growthActions.refreshInvestments()

    fun setInvestmentHistoryRange(range: String) = growthActions.setInvestmentHistoryRange(range)

    fun createSchedule(request: TransactionScheduleRequest) = growthActions.createSchedule(request)

    fun toggleSchedule(schedule: TransactionSchedule) = growthActions.toggleSchedule(schedule)

    fun deleteSchedule(id: Int) = growthActions.deleteSchedule(id)

    fun createBudget(request: BudgetRequest) = growthActions.createBudget(request)

    fun deleteBudget(id: Int) = growthActions.deleteBudget(id)

    fun updateNotificationPreferences(preferences: NotificationPreferences) =
        growthActions.updateNotificationPreferences(preferences)

    fun registerPushDevice(deviceToken: String) = growthActions.registerPushDevice(deviceToken)

    fun openPushEvent(eventType: String) = growthActions.openPushEvent(eventType)

    private fun unregisterPushDevice() = growthActions.unregisterPushDevice()

    fun createInvestmentTrade(request: InvestmentTradeRequest) =
        growthActions.createInvestmentTrade(request)

    fun deleteInvestmentTrade(id: Int) = growthActions.deleteInvestmentTrade(id)

    fun setInvestmentPrice(request: InvestmentPriceRequest) =
        growthActions.setInvestmentPrice(request)

    fun createInvestmentSchedule(request: InvestmentScheduleRequest) =
        growthActions.createInvestmentSchedule(request)

    fun toggleInvestmentSchedule(schedule: InvestmentSchedule) =
        growthActions.toggleInvestmentSchedule(schedule)

    fun deleteInvestmentSchedule(id: Int) = growthActions.deleteInvestmentSchedule(id)

    fun exportInvestments(from: String, to: String) = growthActions.exportInvestments(from, to)

    fun clearInvestmentExport() = growthActions.clearInvestmentExport()

    fun clearGrowthError() = growthActions.clearError()

    fun previousMonth() = moveMonth(-1)

    fun nextMonth() {
        if (state.value.canGoNextMonth) moveMonth(1)
    }

    fun refresh() {
        loadMonth(state.value.month, forceRefresh = true)
        growthActions.refreshInvestments()
    }

    fun selectExpenseCategory(category: String) = transactionActions.selectExpenseCategory(category)

    fun clearSelectedExpenseCategory() = transactionActions.clearSelectedExpenseCategory()

    fun updateFilterType(value: String?) = transactionActions.updateFilterType(value)

    fun updateFilterCategory(value: String?) = transactionActions.updateFilterCategory(value)

    fun updateSearchQuery(value: String) = transactionActions.updateSearchQuery(value)

    fun clearTransactionFilters() = transactionActions.clearTransactionFilters()

    fun showAllTransactions() = transactionActions.showAllTransactions()

    fun updateFormType(value: String) = transactionActions.updateFormType(value)

    fun updateFormCategory(value: String) = transactionActions.updateFormCategory(value)

    fun chooseFormCategory(value: String) = transactionActions.chooseFormCategory(value)

    fun updateFormAmount(value: String) = transactionActions.updateFormAmount(value)

    fun updateFormDescription(value: String) = transactionActions.updateFormDescription(value)

    fun updateFormOccurredAt(value: String) = transactionActions.updateFormOccurredAt(value)

    fun updateNewCategoryName(value: String) = transactionActions.updateNewCategoryName(value)

    fun openCategoryPicker() = transactionActions.openCategoryPicker()

    fun closeCategoryPicker() = transactionActions.closeCategoryPicker()

    fun addCategory() = transactionActions.addCategory()

    fun deleteCategory(category: Category) = transactionActions.deleteCategory(category)

    fun openNewTransactionForm() = transactionActions.openNewTransactionForm()

    fun openPhysicalPurchaseForm() = transactionActions.openPhysicalPurchaseForm()

    fun closeTransactionForm() = transactionActions.closeTransactionForm()

    fun saveTransaction() = transactionActions.saveTransaction()

    fun editTransaction(transaction: Transaction) = transactionActions.editTransaction(transaction)

    fun deleteTransaction(id: Int) = transactionActions.deleteTransaction(id)

    fun openExportDialog() = transactionActions.openExportDialog()

    fun closeExportDialog() = transactionActions.closeExportDialog()

    fun updateExportFrom(value: String) = transactionActions.updateExportFrom(value)

    fun updateExportTo(value: String) = transactionActions.updateExportTo(value)

    fun exportTransactions() = transactionActions.exportTransactions()

    fun clearExportResult() = transactionActions.clearExportResult()

    fun importRevolutCsv(contents: ByteArray) = transactionActions.importRevolutCsv(contents)

    fun clearImportMessage() = transactionActions.clearImportMessage()

    fun refreshHealth() {
        viewModelScope.launch {
            _state.update { it.copy(connectionStatus = ConnectionStatus.Checking, connectionMessage = null) }
            try {
                val healthy = withContext(ioDispatcher) { apiClient.checkHealth() }
                _state.update {
                    it.copy(
                        connectionStatus = if (healthy) ConnectionStatus.Connected else ConnectionStatus.Offline,
                        connectionMessage = if (healthy) null else "The server returned an unexpected response",
                    )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _state.update {
                    it.copy(connectionStatus = ConnectionStatus.Offline, connectionMessage = userMessage(error))
                }
            }
        }
    }

    private fun moveMonth(delta: Long) {
        val target = runCatching { YearMonth.parse(state.value.month) }
            .getOrElse { YearMonth.now() }
            .plusMonths(delta)
            .format(monthFormatter)
        _state.update {
            it.copy(
                month = target,
                selectedExpenseCategory = null,
                filterCategory = null,
                monthError = null,
            )
        }
        loadMonth(target)
    }

    private fun loadMonth(month: String, forceRefresh: Boolean = false) {
        val token = state.value.token ?: return
        monthLoadJob?.cancel()
        val cached = monthCache[month]
        if (cached != null) {
            _state.update {
                if (it.month == month) {
                    it.copy(
                        loadedMonth = month,
                        summary = cached.summary,
                        transactions = cached.transactions,
                        monthLoadPhase = if (forceRefresh) MonthLoadPhase.Refreshing else MonthLoadPhase.Content,
                        monthError = null,
                    )
                } else {
                    it
                }
            }
            if (!forceRefresh) return
        } else {
            _state.update {
                if (it.month == month) {
                    it.copy(
                        loadedMonth = null,
                        summary = null,
                        transactions = emptyList(),
                        monthLoadPhase = MonthLoadPhase.Loading,
                        monthError = null,
                    )
                } else {
                    it
                }
            }
        }

        monthLoadJob = sessionScope.launch {
            try {
                val snapshot = withContext(ioDispatcher) {
                    coroutineScope {
                        val summary = async { apiClient.getSummary(token, month) }
                        val transactions = async {
                            apiClient.getTransactions(token, month, type = null, category = null)
                        }
                        MonthSnapshot(summary.await(), transactions.await())
                    }
                }
                if (!sessionIsCurrent(token)) return@launch
                monthCache[month] = snapshot
                _state.update {
                    if (it.month == month) {
                        it.copy(
                            loadedMonth = month,
                            summary = snapshot.summary,
                            transactions = snapshot.transactions,
                            formCurrency = snapshot.summary.currency,
                            monthLoadPhase = MonthLoadPhase.Content,
                            monthError = null,
                        )
                    } else {
                        it
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!handleSessionExpiry(error, token) && sessionIsCurrent(token)) {
                    _state.update {
                        if (it.month == month) {
                            it.copy(monthLoadPhase = MonthLoadPhase.Failure, monthError = userMessage(error))
                        } else {
                            it
                        }
                    }
                }
            }
        }
    }

    private fun loadCategories() {
        val token = state.value.token ?: return
        categoryLoadJob?.cancel()
        categoryLoadJob = sessionScope.launch {
            try {
                loadCategoriesNow(token)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!handleSessionExpiry(error, token) && sessionIsCurrent(token)) {
                    _state.update { it.copy(profileError = userMessage(error)) }
                }
            }
        }
    }

    private suspend fun loadCategoriesNow(token: String) {
        val (expenses, income) = withContext(ioDispatcher) {
            coroutineScope {
                val expenses = async { apiClient.getCategories(token, "expense") }
                val income = async { apiClient.getCategories(token, "income") }
                expenses.await() to income.await()
            }
        }
        if (!sessionIsCurrent(token)) return
        _state.update {
            it.copy(
                expenseCategories = expenses,
                incomeCategories = income,
                formCategory = when {
                    it.formCategory.isNotBlank() -> it.formCategory
                    else -> expenses.firstOrNull()?.name ?: "groceries"
                },
            )
        }
    }

    private fun handleSessionExpiry(error: Exception, token: String): Boolean {
        if (error is ApiException && error.status == 401) {
            if (sessionIsCurrent(token)) {
                clearSession("Your session expired. Log in again.")
            }
            return true
        }
        return false
    }

    private fun clearSession(message: String? = null) {
        authJob?.cancel()
        authJob = null
        sessionSupervisor.cancel()
        sessionSupervisor = SupervisorJob(viewModelScope.coroutineContext[Job])
        sessionScope = CoroutineScope(viewModelScope.coroutineContext + sessionSupervisor)
        monthLoadJob?.cancel()
        categoryLoadJob?.cancel()
        monthLoadJob = null
        categoryLoadJob = null
        monthCache.clear()
        tokenStore.clearToken()
        val connectionStatus = state.value.connectionStatus
        val connectionMessage = state.value.connectionMessage
        val hidePortfolioBalances = state.value.hidePortfolioBalances
        val appearance = state.value.appearance
        _state.value = MoneyManagerUiState(
            authError = message,
            hidePortfolioBalances = hidePortfolioBalances,
            appearance = appearance,
            connectionStatus = connectionStatus,
            connectionMessage = connectionMessage,
        )
    }

    private fun sessionIsCurrent(token: String): Boolean = state.value.token == token
}
