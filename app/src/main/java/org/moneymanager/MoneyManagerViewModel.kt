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
import org.moneymanager.data.MoneyManagerApi
import org.moneymanager.data.SessionStore
import org.moneymanager.model.Category
import org.moneymanager.model.Transaction
import org.moneymanager.model.TransactionRequest
import org.moneymanager.model.TransactionSummary

data class CategoryTotal(
    val category: String,
    val amount: BigDecimal,
)

data class DayBucket(
    val date: LocalDate,
    val balanceChange: BigDecimal,
    val transactions: List<Transaction>,
)

data class MonthSnapshot(
    val summary: TransactionSummary,
    val transactions: List<Transaction>,
)

enum class AppTab {
    Dashboard,
    Transactions,
    Investments,
    Profile,
}

enum class MonthLoadPhase {
    Idle,
    Loading,
    Refreshing,
    Content,
    Failure,
}

enum class ConnectionStatus {
    Checking,
    Connected,
    Offline,
}

data class MoneyManagerUiState(
    val token: String? = null,
    val email: String = "",
    val signedInEmail: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val selectedTab: AppTab = AppTab.Dashboard,
    val isRegisterMode: Boolean = false,
    val isAuthLoading: Boolean = false,
    val authError: String? = null,
    val month: String = YearMonth.now().format(monthFormatter),
    val loadedMonth: String? = null,
    val monthLoadPhase: MonthLoadPhase = MonthLoadPhase.Idle,
    val monthError: String? = null,
    val summary: TransactionSummary? = null,
    val transactions: List<Transaction> = emptyList(),
    val filterType: String? = null,
    val filterCategory: String? = null,
    val searchQuery: String = "",
    val selectedExpenseCategory: String? = null,
    val isTransactionFormOpen: Boolean = false,
    val editingId: Int? = null,
    val formType: String = "expense",
    val formCategory: String = "food",
    val formAmount: String = "",
    val formCurrency: String = "EUR",
    val formDescription: String = "",
    val formOccurredAt: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val formBaseline: String = "",
    val formError: String? = null,
    val isFormSaving: Boolean = false,
    val isTransactionMutating: Boolean = false,
    val isCategoryPickerOpen: Boolean = false,
    val expenseCategories: List<Category> = emptyList(),
    val incomeCategories: List<Category> = emptyList(),
    val newCategoryName: String = "",
    val categoryError: String? = null,
    val isCategoryMutating: Boolean = false,
    val isExportDialogOpen: Boolean = false,
    val exportFrom: String = LocalDate.now().withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
    val exportTo: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val exportCsvContent: String? = null,
    val exportFileName: String? = null,
    val exportError: String? = null,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val importMessage: String? = null,
    val profileError: String? = null,
    val isAccountDeleting: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Checking,
    val connectionMessage: String? = null,
) {
    val hasMonthContent: Boolean
        get() = loadedMonth == month && summary != null

    val isMonthLoading: Boolean
        get() = monthLoadPhase == MonthLoadPhase.Loading

    val isMonthRefreshing: Boolean
        get() = monthLoadPhase == MonthLoadPhase.Refreshing

    val currentCurrency: String
        get() = if (hasMonthContent) summary?.currency ?: formCurrency else formCurrency

    val expenseCategoryTotals: List<CategoryTotal>
        get() = if (!hasMonthContent) {
            emptyList()
        } else {
            transactions
                .filter { it.type == "expense" }
                .groupBy { it.category }
                .map { (category, transactions) ->
                    CategoryTotal(
                        category = category,
                        amount = transactions.sumAmounts(),
                    )
                }
                .filter { it.amount > BigDecimal.ZERO }
                .sortedByDescending { it.amount }
        }

    val dayBuckets: List<DayBucket>
        get() = makeDayBuckets(filteredTransactions)

    val dashboardDayBuckets: List<DayBucket>
        get() = makeDayBuckets(dashboardTransactions)

    private fun makeDayBuckets(source: List<Transaction>): List<DayBucket> =
        source
            .groupBy { it.occurredAt.toLocalDateOrNull() ?: LocalDate.MIN }
            .map { (date, transactions) ->
                val sortedTransactions = transactions.sortedByDescending { it.occurredAt }
                DayBucket(
                    date = date,
                    balanceChange = sortedTransactions.fold(BigDecimal.ZERO) { total, transaction ->
                        if (transaction.type == "income") {
                            total + transaction.amount.toMoney()
                        } else {
                            total - transaction.amount.toMoney()
                        }
                    },
                    transactions = sortedTransactions,
                )
            }
            .sortedByDescending { it.date }

    val canGoNextMonth: Boolean
        get() = runCatching { YearMonth.parse(month).isBefore(YearMonth.now()) }.getOrDefault(false)

    val hasActiveTransactionFilters: Boolean
        get() = filterType != null || filterCategory != null || searchQuery.isNotBlank()

    val availableFilterCategories: List<String>
        get() = (transactions.map { it.category } + expenseCategories.map { it.name } + incomeCategories.map { it.name })
            .distinct()
            .sortedBy(::categoryTitle)

    val formCategoryOptions: List<Category>
        get() {
            val categories = if (formType == "income") incomeCategories else expenseCategories
            return if (formCategory.isBlank() || categories.any { it.name == formCategory }) {
                categories
            } else {
                categories + Category(id = 0, type = formType, name = formCategory, isDefault = false)
            }
        }

    val isFormValid: Boolean
        get() = parseLocalizedDecimal(formAmount)?.let { it > BigDecimal.ZERO && it.scale() <= 2 } == true &&
            formCategory.isNotBlank() &&
            formOccurredAt.toLocalDateOrNull() != null &&
            formDescription.length <= MAX_DESCRIPTION_LENGTH

    val hasUnsavedFormChanges: Boolean
        get() = isTransactionFormOpen && formFingerprint() != formBaseline

    private val filteredTransactions: List<Transaction>
        get() {
            if (!hasMonthContent) return emptyList()
            var result = transactions
            filterType?.let { type -> result = result.filter { it.type == type } }
            filterCategory?.let { category -> result = result.filter { it.category == category } }
            val query = searchQuery.trim()
            if (query.isNotBlank()) {
                result = result.filter {
                    it.description.contains(query, ignoreCase = true) ||
                        it.category.contains(query, ignoreCase = true) ||
                        it.amount.contains(query)
                }
            }
            return result
        }

    private val dashboardTransactions: List<Transaction>
        get() {
            if (!hasMonthContent) return emptyList()
            val selected = selectedExpenseCategory ?: return transactions
            return transactions.filter { it.type == "expense" && it.category == selected }
        }

    private fun formFingerprint(): String = formFingerprint(
        type = formType,
        category = formCategory,
        amount = formAmount,
        currency = formCurrency,
        description = formDescription,
        occurredAt = formOccurredAt,
    )
}

class MoneyManagerViewModel(
    private val apiClient: MoneyManagerApi,
    private val tokenStore: SessionStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
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
        ),
    )
    val state: StateFlow<MoneyManagerUiState> = _state.asStateFlow()

    init {
        refreshHealth()
        if (_state.value.token != null) {
            loadCategories()
            loadMonth(_state.value.month)
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
                        connectionStatus = it.connectionStatus,
                        connectionMessage = it.connectionMessage,
                    )
                }
                loadCategories()
                loadMonth(state.value.month)
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
        _state.update { it.copy(selectedTab = tab, profileError = null) }
        if (tab == AppTab.Profile) refreshHealth()
    }

    fun previousMonth() = moveMonth(-1)

    fun nextMonth() {
        if (state.value.canGoNextMonth) moveMonth(1)
    }

    fun refresh() = loadMonth(state.value.month, forceRefresh = true)

    fun selectExpenseCategory(category: String) {
        _state.update {
            it.copy(selectedExpenseCategory = if (it.selectedExpenseCategory == category) null else category)
        }
    }

    fun clearSelectedExpenseCategory() {
        _state.update { it.copy(selectedExpenseCategory = null) }
    }

    fun updateFilterType(value: String?) = _state.update {
        it.copy(filterType = value, filterCategory = null)
    }

    fun updateFilterCategory(value: String?) = _state.update { it.copy(filterCategory = value) }

    fun updateSearchQuery(value: String) = _state.update { it.copy(searchQuery = value) }

    fun clearTransactionFilters() = _state.update {
        it.copy(filterType = null, filterCategory = null, searchQuery = "")
    }

    fun showAllTransactions() = _state.update {
        it.copy(
            selectedTab = AppTab.Transactions,
            filterType = if (it.selectedExpenseCategory == null) null else "expense",
            filterCategory = it.selectedExpenseCategory,
            searchQuery = "",
        )
    }

    fun updateFormType(value: String) {
        if (state.value.isFormSaving) return
        _state.update {
            val categories = if (value == "income") it.incomeCategories else it.expenseCategories
            it.copy(
                formType = value,
                formCategory = categories.firstOrNull()?.name ?: if (value == "income") "salary" else "food",
                formError = null,
            )
        }
    }

    fun updateFormCategory(value: String) {
        if (state.value.isFormSaving) return
        _state.update { it.copy(formCategory = value, formError = null) }
    }

    fun chooseFormCategory(value: String) {
        if (state.value.isFormSaving || state.value.isCategoryMutating) return
        _state.update {
            it.copy(
                formCategory = value,
                isCategoryPickerOpen = false,
                newCategoryName = "",
                categoryError = null,
                formError = null,
            )
        }
    }

    fun updateFormAmount(value: String) {
        if (state.value.isFormSaving) return
        if (value.length <= MAX_AMOUNT_LENGTH && value.all { it.isDigit() || it == '.' || it == ',' || it.isWhitespace() }) {
            _state.update { it.copy(formAmount = value, formError = null) }
        }
    }

    fun updateFormDescription(value: String) {
        if (state.value.isFormSaving) return
        if (value.length <= MAX_DESCRIPTION_LENGTH) {
            _state.update { it.copy(formDescription = value, formError = null) }
        }
    }

    fun updateFormOccurredAt(value: String) {
        if (state.value.isFormSaving) return
        _state.update { it.copy(formOccurredAt = value, formError = null) }
    }

    fun updateNewCategoryName(value: String) {
        if (state.value.isCategoryMutating) return
        _state.update { it.copy(newCategoryName = value, categoryError = null) }
    }

    fun openCategoryPicker() = _state.update {
        it.copy(isCategoryPickerOpen = true, categoryError = null)
    }

    fun closeCategoryPicker() {
        if (state.value.isCategoryMutating) return
        _state.update {
            it.copy(isCategoryPickerOpen = false, newCategoryName = "", categoryError = null)
        }
    }

    fun addCategory() {
        val current = state.value
        val token = current.token ?: return
        if (current.isCategoryMutating || current.isFormSaving) return
        val name = current.newCategoryName.trim()
        if (name.isBlank()) {
            _state.update { it.copy(categoryError = "Enter a category name") }
            return
        }
        sessionScope.launch {
            _state.update { it.copy(isCategoryMutating = true, categoryError = null) }
            try {
                val category = withContext(ioDispatcher) {
                    apiClient.createCategory(token, current.formType, name)
                }
                loadCategoriesNow(token)
                if (!sessionIsCurrent(token) || state.value.formType != current.formType || !state.value.isCategoryPickerOpen) return@launch
                _state.update {
                    it.copy(
                        formCategory = category.name,
                        isCategoryPickerOpen = false,
                        newCategoryName = "",
                    )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!handleSessionExpiry(error, token) && sessionIsCurrent(token)) {
                    _state.update { it.copy(categoryError = userMessage(error)) }
                }
            } finally {
                if (sessionIsCurrent(token)) {
                    _state.update { it.copy(isCategoryMutating = false) }
                }
            }
        }
    }

    fun deleteCategory(category: Category) {
        val current = state.value
        val token = current.token ?: return
        if (current.isCategoryMutating || current.isFormSaving) return
        if (category.isDefault || category.id == 0) return
        sessionScope.launch {
            _state.update { it.copy(isCategoryMutating = true, categoryError = null) }
            try {
                withContext(ioDispatcher) { apiClient.deleteCategory(token, category.id) }
                loadCategoriesNow(token)
                if (!sessionIsCurrent(token)) return@launch
                _state.update {
                    val categories = if (it.formType == "income") it.incomeCategories else it.expenseCategories
                    it.copy(
                        formCategory = if (it.formCategory == category.name) {
                            categories.firstOrNull()?.name ?: if (it.formType == "income") "salary" else "food"
                        } else {
                            it.formCategory
                        },
                    )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!handleSessionExpiry(error, token) && sessionIsCurrent(token)) {
                    _state.update { it.copy(categoryError = userMessage(error)) }
                }
            } finally {
                if (sessionIsCurrent(token)) {
                    _state.update { it.copy(isCategoryMutating = false) }
                }
            }
        }
    }

    fun openNewTransactionForm() {
        val current = state.value
        val category = current.expenseCategories.firstOrNull()?.name ?: "food"
        val currency = current.summary?.takeIf { current.hasMonthContent }?.currency ?: "EUR"
        val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val baseline = formFingerprint("expense", category, "", currency, "", date)
        _state.update {
            it.copy(
                isTransactionFormOpen = true,
                editingId = null,
                formType = "expense",
                formCategory = category,
                formAmount = "",
                formCurrency = currency,
                formDescription = "",
                formOccurredAt = date,
                formBaseline = baseline,
                formError = null,
                categoryError = null,
            )
        }
    }

    fun openPhysicalPurchaseForm() {
        openNewTransactionForm()
        _state.update {
            val baseline = formFingerprint("expense", "shopping", "", it.formCurrency, "Physical purchase", it.formOccurredAt)
            it.copy(
                formType = "expense",
                formCategory = "shopping",
                formDescription = "Physical purchase",
                formBaseline = baseline,
            )
        }
    }

    fun closeTransactionForm() {
        _state.update(::resetEditorState)
    }

    fun saveTransaction() {
        val current = state.value
        val token = current.token ?: return
        if (current.isFormSaving) return
        val sourceMonth = current.month
        validateTransactionForm(current)?.let { message ->
            _state.update { it.copy(formError = message) }
            return
        }
        val amount = parseLocalizedDecimal(current.formAmount) ?: return
        val request = TransactionRequest(
            type = current.formType,
            category = current.formCategory,
            description = current.formDescription.trim(),
            amount = amount.stripTrailingZeros().toPlainString(),
            currency = current.formCurrency,
            occurredAt = current.formOccurredAt,
        )

        sessionScope.launch {
            _state.update { it.copy(isFormSaving = true, formError = null) }
            try {
                withContext(ioDispatcher) {
                    if (current.editingId == null) {
                        apiClient.createTransaction(token, request)
                    } else {
                        apiClient.updateTransaction(token, current.editingId, request)
                    }
                }
                if (!sessionIsCurrent(token)) return@launch
                val transactionMonth = YearMonth.from(LocalDate.parse(current.formOccurredAt)).format(monthFormatter)
                monthCache.remove(sourceMonth)
                monthCache.remove(transactionMonth)
                _state.update { resetEditorState(it).copy(month = transactionMonth) }
                loadMonth(transactionMonth, forceRefresh = true)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!handleSessionExpiry(error, token) && sessionIsCurrent(token)) {
                    _state.update { it.copy(formError = userMessage(error)) }
                }
            } finally {
                if (sessionIsCurrent(token)) {
                    _state.update { it.copy(isFormSaving = false) }
                }
            }
        }
    }

    fun editTransaction(transaction: Transaction) {
        val baseline = formFingerprint(
            transaction.type,
            transaction.category,
            transaction.amount,
            transaction.currency,
            transaction.description,
            transaction.occurredAt.dateOnly(),
        )
        _state.update {
            it.copy(
                editingId = transaction.id,
                formType = transaction.type,
                formCategory = transaction.category,
                formAmount = transaction.amount,
                formCurrency = transaction.currency,
                formDescription = transaction.description,
                formOccurredAt = transaction.occurredAt.dateOnly(),
                formBaseline = baseline,
                formError = null,
                isTransactionFormOpen = true,
            )
        }
    }

    fun deleteTransaction(id: Int) {
        val current = state.value
        val token = current.token ?: return
        val sourceMonth = current.month
        sessionScope.launch {
            _state.update { it.copy(isTransactionMutating = true, monthError = null) }
            try {
                withContext(ioDispatcher) { apiClient.deleteTransaction(token, id) }
                if (!sessionIsCurrent(token)) return@launch
                monthCache.remove(sourceMonth)
                if (state.value.month == sourceMonth) {
                    loadMonth(sourceMonth, forceRefresh = true)
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!handleSessionExpiry(error, token) && sessionIsCurrent(token)) {
                    _state.update { it.copy(monthError = userMessage(error)) }
                }
            } finally {
                if (sessionIsCurrent(token)) {
                    _state.update { it.copy(isTransactionMutating = false) }
                }
            }
        }
    }

    fun openExportDialog() {
        val selectedMonth = runCatching { YearMonth.parse(state.value.month) }.getOrDefault(YearMonth.now())
        val today = LocalDate.now()
        val end = minOf(selectedMonth.atEndOfMonth(), today)
        _state.update {
            it.copy(
                isExportDialogOpen = true,
                exportFrom = selectedMonth.atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
                exportTo = end.format(DateTimeFormatter.ISO_LOCAL_DATE),
                exportError = null,
            )
        }
    }

    fun closeExportDialog() {
        if (state.value.isExporting) return
        _state.update {
            it.copy(isExportDialogOpen = false, exportError = null)
        }
    }

    fun updateExportFrom(value: String) {
        if (state.value.isExporting) return
        _state.update { it.copy(exportFrom = value, exportError = null) }
    }

    fun updateExportTo(value: String) {
        if (state.value.isExporting) return
        _state.update { it.copy(exportTo = value, exportError = null) }
    }

    fun exportTransactions() {
        val current = state.value
        val token = current.token ?: return
        if (current.isExporting) return
        val fromDate = current.exportFrom.toLocalDateOrNull()
        val toDate = current.exportTo.toLocalDateOrNull()
        val validationError = when {
            fromDate == null -> "Choose a valid start date"
            toDate == null -> "Choose a valid end date"
            fromDate.isAfter(toDate) -> "Start date must be before end date"
            else -> null
        }
        if (validationError != null) {
            _state.update { it.copy(exportError = validationError) }
            return
        }

        sessionScope.launch {
            _state.update { it.copy(isExporting = true, exportError = null) }
            try {
                val csv = withContext(ioDispatcher) {
                    apiClient.exportTransactionsCsv(token, current.exportFrom, current.exportTo)
                }
                if (!sessionIsCurrent(token)) return@launch
                _state.update {
                    it.copy(
                        isExportDialogOpen = false,
                        exportCsvContent = csv,
                        exportFileName = "money-manager-${current.exportFrom}-to-${current.exportTo}.csv",
                    )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!handleSessionExpiry(error, token) && sessionIsCurrent(token)) {
                    _state.update { it.copy(exportError = userMessage(error)) }
                }
            } finally {
                if (sessionIsCurrent(token)) {
                    _state.update { it.copy(isExporting = false) }
                }
            }
        }
    }

    fun clearExportResult() = _state.update {
        it.copy(exportCsvContent = null, exportFileName = null)
    }

    fun importRevolutCsv(contents: ByteArray) {
        val current = state.value
        val token = current.token ?: return
        if (current.isImporting) return
        sessionScope.launch {
            _state.update { it.copy(isImporting = true, profileError = null) }
            try {
                val result = withContext(ioDispatcher) { apiClient.importRevolutCsv(token, contents) }
                if (!sessionIsCurrent(token)) return@launch
                _state.update {
                    it.copy(importMessage = "Imported ${result.imported}. Skipped ${result.skipped} duplicates and ${result.ignored} unsupported rows.")
                }
                refresh()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!handleSessionExpiry(error, token) && sessionIsCurrent(token)) {
                    _state.update { it.copy(profileError = userMessage(error)) }
                }
            } finally {
                if (sessionIsCurrent(token)) _state.update { it.copy(isImporting = false) }
            }
        }
    }

    fun clearImportMessage() = _state.update { it.copy(importMessage = null) }

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
                    else -> expenses.firstOrNull()?.name ?: "food"
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
        _state.value = MoneyManagerUiState(
            authError = message,
            connectionStatus = connectionStatus,
            connectionMessage = connectionMessage,
        )
    }

    private fun sessionIsCurrent(token: String): Boolean = state.value.token == token
}

class MoneyManagerViewModelFactory(
    private val apiClient: MoneyManagerApi,
    private val tokenStore: SessionStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MoneyManagerViewModel(apiClient, tokenStore) as T
}

private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
private const val MAX_DESCRIPTION_LENGTH = 200
private const val MAX_AMOUNT_LENGTH = 18
private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

private fun List<Transaction>.sumAmounts(): BigDecimal =
    fold(BigDecimal.ZERO) { total, transaction -> total + transaction.amount.toMoney() }

private fun String.toMoney(): BigDecimal = BigDecimal(this)

private fun String.toLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(dateOnly()) }.getOrNull()

private fun String.dateOnly(): String = take(10)

private fun validateAuth(state: MoneyManagerUiState): String? = when {
    state.email.isBlank() -> "Enter your email address"
    !emailPattern.matches(state.email.trim()) -> "Enter a valid email address"
    state.password.isBlank() -> "Enter your password"
    state.isRegisterMode && state.password.length < 8 -> "Use at least 8 characters for your password"
    state.isRegisterMode && state.password != state.confirmPassword -> "Passwords do not match"
    else -> null
}

private fun validateTransactionForm(state: MoneyManagerUiState): String? {
    val amount = parseLocalizedDecimal(state.formAmount)
    return when {
        amount == null || amount <= BigDecimal.ZERO -> "Enter an amount greater than 0"
        amount.scale() > 2 -> "Use no more than two decimal places"
        state.formCategory.isBlank() -> "Choose a category"
        state.formOccurredAt.toLocalDateOrNull() == null -> "Choose a valid date"
        state.formDescription.length > MAX_DESCRIPTION_LENGTH -> "Description is too long"
        else -> null
    }
}

private fun resetEditorState(state: MoneyManagerUiState): MoneyManagerUiState = state.copy(
    isTransactionFormOpen = false,
    editingId = null,
    formType = "expense",
    formCategory = state.expenseCategories.firstOrNull()?.name ?: "food",
    formAmount = "",
    formDescription = "",
    formOccurredAt = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    formBaseline = "",
    formError = null,
    isCategoryPickerOpen = false,
    newCategoryName = "",
    categoryError = null,
)

private fun formFingerprint(
    type: String,
    category: String,
    amount: String,
    currency: String,
    description: String,
    occurredAt: String,
): String = listOf(type, category, amount, currency, description, occurredAt).joinToString("\u0000")

private fun userMessage(error: Exception): String = when (error) {
    is ApiException -> error.message ?: "The server could not complete the request"
    is IOException -> "Could not connect. Check your connection and try again."
    else -> error.message ?: "Something went wrong. Try again."
}
