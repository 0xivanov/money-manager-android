package org.moneymanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.moneymanager.data.ApiException
import org.moneymanager.data.ApiClient
import org.moneymanager.data.TokenStore
import org.moneymanager.model.Category
import org.moneymanager.model.Transaction
import org.moneymanager.model.TransactionRequest
import org.moneymanager.model.TransactionSummary
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class CategoryTotal(
    val category: String,
    val amount: BigDecimal,
)

data class DayBucket(
    val date: LocalDate,
    val balanceChange: BigDecimal,
    val transactions: List<Transaction>,
)

enum class AppTab {
    Dashboard,
    Transactions,
    Profile,
}

data class MoneyManagerUiState(
    val token: String? = null,
    val email: String = "",
    val signedInEmail: String = "",
    val password: String = "",
    val selectedTab: AppTab = AppTab.Dashboard,
    val isRegisterMode: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val month: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")),
    val filterType: String? = null,
    val filterCategory: String? = null,
    val summary: TransactionSummary? = null,
    val transactions: List<Transaction> = emptyList(),
    val selectedExpenseCategory: String? = null,
    val isTransactionFormOpen: Boolean = false,
    val editingId: Int? = null,
    val formType: String = "expense",
    val formCategory: String = "food",
    val formAmount: String = "",
    val formDescription: String = "",
    val formOccurredAt: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val isCategoryPickerOpen: Boolean = false,
    val expenseCategories: List<Category> = emptyList(),
    val incomeCategories: List<Category> = emptyList(),
    val newCategoryName: String = "",
    val isExportDialogOpen: Boolean = false,
    val exportFrom: String = LocalDate.now().withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
    val exportTo: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val exportCsvContent: String? = null,
    val exportFileName: String? = null,
) {
    val expenseCategoryTotals: List<CategoryTotal>
        get() = transactions
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

    val dayBuckets: List<DayBucket>
        get() = filteredTransactions
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

    private val filteredTransactions: List<Transaction>
        get() {
            var result = transactions
            filterType?.let { type -> result = result.filter { it.type == type } }
            filterCategory?.let { category -> result = result.filter { it.category == category } }
            selectedExpenseCategory?.let { category ->
                result = result.filter { it.type == "expense" && it.category == category }
            }
            return result
        }

    val formCategoryOptions: List<Category>
        get() {
            val categories = if (formType == "income") incomeCategories else expenseCategories
            return if (formCategory.isBlank() || categories.any { it.name == formCategory }) {
                categories
            } else {
                categories + Category(id = 0, type = formType, name = formCategory, isDefault = false)
            }
        }
}

class MoneyManagerViewModel(
    private val apiClient: ApiClient,
    private val tokenStore: TokenStore,
) : ViewModel() {
    private val _state = MutableStateFlow(
        MoneyManagerUiState(
            token = tokenStore.getToken(),
            signedInEmail = tokenStore.getEmail(),
        ),
    )
    val state: StateFlow<MoneyManagerUiState> = _state.asStateFlow()

    init {
        if (_state.value.token != null) {
            loadInitialData()
        }
    }

    fun updateEmail(value: String) = _state.update { it.copy(email = value) }

    fun updatePassword(value: String) = _state.update { it.copy(password = value) }

    fun toggleAuthMode() = _state.update { it.copy(isRegisterMode = !it.isRegisterMode, error = null) }

    fun submitAuth() = runRequest {
        val current = state.value
        val email = current.email.trim()
        if (email.isBlank() || current.password.isBlank()) {
            throw IllegalArgumentException("Email and password are required")
        }
        val result = withContext(Dispatchers.IO) {
            if (current.isRegisterMode) {
                apiClient.register(email, current.password)
            } else {
                apiClient.login(email, current.password)
            }
        }
        tokenStore.saveSession(result.token, result.user.email)
        _state.update {
            it.copy(
                token = result.token,
                signedInEmail = result.user.email,
                password = "",
                error = null,
            )
        }
        loadCategories(result.token)
        refreshDashboard(result.token, state.value.month)
    }

    fun logout() {
        tokenStore.clearToken()
        _state.value = MoneyManagerUiState()
    }

    fun selectTab(tab: AppTab) = _state.update { it.copy(selectedTab = tab) }

    fun updateMonth(value: String) = _state.update { it.copy(month = value) }

    fun previousMonth() = moveMonth(-1)

    fun nextMonth() {
        if (state.value.canGoNextMonth) {
            moveMonth(1)
        }
    }

    fun selectExpenseCategory(category: String) {
        _state.update {
            it.copy(selectedExpenseCategory = if (it.selectedExpenseCategory == category) null else category)
        }
    }

    fun clearSelectedExpenseCategory() {
        _state.update { it.copy(selectedExpenseCategory = null) }
    }

    fun updateFilterType(value: String?) = _state.update { it.copy(filterType = value, filterCategory = null) }

    fun updateFilterCategory(value: String?) = _state.update { it.copy(filterCategory = value) }

    fun updateFormType(value: String) = _state.update {
        val categories = if (value == "income") it.incomeCategories else it.expenseCategories
        it.copy(
            formType = value,
            formCategory = categories.firstOrNull()?.name ?: if (value == "income") "salary" else "food",
        )
    }

    fun updateFormCategory(value: String) = _state.update { it.copy(formCategory = value) }

    fun chooseFormCategory(value: String) = _state.update {
        it.copy(formCategory = value, isCategoryPickerOpen = false, newCategoryName = "")
    }

    fun updateFormAmount(value: String) = _state.update { it.copy(formAmount = value) }

    fun updateFormDescription(value: String) = _state.update { it.copy(formDescription = value) }

    fun updateFormOccurredAt(value: String) = _state.update { it.copy(formOccurredAt = value) }

    fun updateNewCategoryName(value: String) = _state.update { it.copy(newCategoryName = value) }

    fun openCategoryPicker() = _state.update { it.copy(isCategoryPickerOpen = true, error = null) }

    fun closeCategoryPicker() = _state.update { it.copy(isCategoryPickerOpen = false, newCategoryName = "") }

    fun addCategory() = runRequest {
        val current = state.value
        val token = current.token ?: return@runRequest
        val name = current.newCategoryName.trim()
        if (name.isBlank()) {
            throw IllegalArgumentException("Category name is required")
        }
        val category = withContext(Dispatchers.IO) {
            apiClient.createCategory(token, current.formType, name)
        }
        loadCategories(token)
        _state.update { it.copy(formCategory = category.name, isCategoryPickerOpen = false, newCategoryName = "") }
    }

    fun deleteCategory(category: Category) = runRequest {
        val token = state.value.token ?: return@runRequest
        if (category.isDefault || category.id == 0) {
            throw IllegalArgumentException("Default categories cannot be deleted")
        }
        withContext(Dispatchers.IO) { apiClient.deleteCategory(token, category.id) }
        loadCategories(token)
        _state.update {
            val categories = if (it.formType == "income") it.incomeCategories else it.expenseCategories
            val nextCategory = if (it.formCategory == category.name) {
                categories.firstOrNull()?.name ?: if (it.formType == "income") "salary" else "food"
            } else {
                it.formCategory
            }
            it.copy(formCategory = nextCategory)
        }
    }

    fun openNewTransactionForm() {
        clearForm()
        _state.update { it.copy(isTransactionFormOpen = true) }
    }

    fun openPhysicalPurchaseForm() {
        clearForm()
        _state.update {
            it.copy(
                formType = "expense",
                formCategory = "shopping",
                formDescription = "Physical purchase",
                isTransactionFormOpen = true,
            )
        }
    }

    fun closeTransactionForm() {
        clearForm()
        _state.update { it.copy(isTransactionFormOpen = false, isCategoryPickerOpen = false) }
    }

    fun refresh() = runRequest {
        val current = state.value
        val token = current.token ?: return@runRequest
        refreshDashboard(token, current.month)
    }

    fun saveTransaction() = runRequest {
        val current = state.value
        val token = current.token ?: return@runRequest
        validateTransactionForm(current)
        val request = TransactionRequest(
            type = current.formType,
            category = current.formCategory,
            description = current.formDescription,
            amount = current.formAmount,
            occurredAt = current.formOccurredAt,
        )
        withContext(Dispatchers.IO) {
            if (current.editingId == null) {
                apiClient.createTransaction(token, request)
            } else {
                apiClient.updateTransaction(token, current.editingId, request)
            }
        }
        clearForm()
        _state.update { it.copy(isTransactionFormOpen = false) }
        refresh()
    }

    fun editTransaction(transaction: Transaction) {
        _state.update {
            it.copy(
                editingId = transaction.id,
                formType = transaction.type,
                formCategory = transaction.category,
                formAmount = transaction.amount,
                formDescription = transaction.description,
                formOccurredAt = transaction.occurredAt.dateOnly(),
                isTransactionFormOpen = true,
            )
        }
    }

    fun deleteTransaction(id: Int) = runRequest {
        val token = state.value.token ?: return@runRequest
        withContext(Dispatchers.IO) { apiClient.deleteTransaction(token, id) }
        refresh()
    }

    fun openExportDialog() {
        _state.update {
            it.copy(
                isExportDialogOpen = true,
                exportFrom = "${it.month}-01",
                exportTo = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                error = null,
            )
        }
    }

    fun closeExportDialog() {
        _state.update { it.copy(isExportDialogOpen = false) }
    }

    fun updateExportFrom(value: String) = _state.update { it.copy(exportFrom = value) }

    fun updateExportTo(value: String) = _state.update { it.copy(exportTo = value) }

    fun exportTransactions() = runRequest {
        val current = state.value
        val token = current.token ?: return@runRequest
        val fromDate = current.exportFrom.toLocalDateOrNull()
            ?: throw IllegalArgumentException("From date must use YYYY-MM-DD")
        val toDate = current.exportTo.toLocalDateOrNull()
            ?: throw IllegalArgumentException("To date must use YYYY-MM-DD")
        if (fromDate.isAfter(toDate)) {
            throw IllegalArgumentException("From date must be before or equal to to date")
        }
        val csv = withContext(Dispatchers.IO) {
            apiClient.exportTransactionsCsv(token, current.exportFrom, current.exportTo)
        }
        _state.update {
            it.copy(
                isExportDialogOpen = false,
                exportCsvContent = csv,
                exportFileName = "money-manager-${current.exportFrom}-to-${current.exportTo}.csv",
            )
        }
    }

    fun clearExportResult() {
        _state.update { it.copy(exportCsvContent = null, exportFileName = null) }
    }

    fun clearForm() {
        _state.update {
            it.copy(
                editingId = null,
                formType = "expense",
                formCategory = "food",
                formAmount = "",
                formDescription = "",
                formOccurredAt = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                isCategoryPickerOpen = false,
                newCategoryName = "",
            )
        }
    }

    private fun runRequest(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                block()
            } catch (error: ApiException) {
                if (error.status == 401) {
                    tokenStore.clearToken()
                    _state.value = MoneyManagerUiState(error = "Session expired. Please log in again.")
                } else {
                    _state.update { it.copy(error = error.message ?: "Something went wrong") }
                }
            } catch (error: Exception) {
                _state.update { it.copy(error = error.message ?: "Something went wrong") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun moveMonth(delta: Long) {
        val currentMonth = runCatching { YearMonth.parse(state.value.month) }
            .getOrElse { YearMonth.now() }
            .plusMonths(delta)
            .format(monthFormatter)
        _state.update { it.copy(month = currentMonth, selectedExpenseCategory = null) }
        refresh()
    }

    private fun loadInitialData() = runRequest {
        val token = state.value.token ?: return@runRequest
        loadCategories(token)
        refreshDashboard(token, state.value.month)
    }

    private suspend fun loadCategories(token: String) {
        val expenseCategories = withContext(Dispatchers.IO) { apiClient.getCategories(token, "expense") }
        val incomeCategories = withContext(Dispatchers.IO) { apiClient.getCategories(token, "income") }
        _state.update {
            it.copy(
                expenseCategories = expenseCategories,
                incomeCategories = incomeCategories,
                formCategory = if (it.formCategory.isBlank()) {
                    expenseCategories.firstOrNull()?.name ?: "food"
                } else {
                    it.formCategory
                },
            )
        }
    }

    private suspend fun refreshDashboard(token: String, month: String) {
        val summary = withContext(Dispatchers.IO) { apiClient.getSummary(token, month) }
        val transactions = withContext(Dispatchers.IO) {
            apiClient.getTransactions(token, month, type = null, category = null)
        }
        _state.update { it.copy(summary = summary, transactions = transactions, error = null) }
    }
}

class MoneyManagerViewModelFactory(
    private val apiClient: ApiClient,
    private val tokenStore: TokenStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MoneyManagerViewModel(apiClient, tokenStore) as T
    }
}

private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

private fun List<Transaction>.sumAmounts(): BigDecimal =
    fold(BigDecimal.ZERO) { total, transaction -> total + transaction.amount.toMoney() }

private fun String.toMoney(): BigDecimal =
    runCatching { BigDecimal(this) }.getOrDefault(BigDecimal.ZERO)

private fun String.toLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(dateOnly()) }.getOrNull()

private fun String.dateOnly(): String = take(10)

private fun validateTransactionForm(state: MoneyManagerUiState) {
    val amount = runCatching { BigDecimal(state.formAmount.trim()) }.getOrNull()
    require(amount != null && amount > BigDecimal.ZERO) { "Enter an amount greater than 0" }
    require(state.formCategory.isNotBlank()) { "Choose a category" }
    require(state.formOccurredAt.toLocalDateOrNull() != null) { "Enter a valid date as YYYY-MM-DD" }
}
