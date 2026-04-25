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
import org.moneymanager.data.ApiClient
import org.moneymanager.data.TokenStore
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

data class MoneyManagerUiState(
    val token: String? = null,
    val email: String = "",
    val password: String = "",
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
    val formOccurredAt: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
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
        get() = selectedExpenseCategory?.let { category ->
            transactions.filter { it.type == "expense" && it.category == category }
        } ?: transactions
}

class MoneyManagerViewModel(
    private val apiClient: ApiClient,
    private val tokenStore: TokenStore,
) : ViewModel() {
    private val _state = MutableStateFlow(MoneyManagerUiState(token = tokenStore.getToken()))
    val state: StateFlow<MoneyManagerUiState> = _state.asStateFlow()

    init {
        if (_state.value.token != null) {
            refresh()
        }
    }

    fun updateEmail(value: String) = _state.update { it.copy(email = value) }

    fun updatePassword(value: String) = _state.update { it.copy(password = value) }

    fun toggleAuthMode() = _state.update { it.copy(isRegisterMode = !it.isRegisterMode, error = null) }

    fun submitAuth() = runRequest {
        val current = state.value
        val result = withContext(Dispatchers.IO) {
            if (current.isRegisterMode) {
                apiClient.register(current.email, current.password)
            } else {
                apiClient.login(current.email, current.password)
            }
        }
        tokenStore.saveToken(result.token)
        _state.update {
            it.copy(
                token = result.token,
                password = "",
                error = null,
            )
        }
        refresh()
    }

    fun logout() {
        tokenStore.clearToken()
        _state.value = MoneyManagerUiState()
    }

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
        it.copy(
            formType = value,
            formCategory = if (value == "income") "salary" else "food",
        )
    }

    fun updateFormCategory(value: String) = _state.update { it.copy(formCategory = value) }

    fun updateFormAmount(value: String) = _state.update { it.copy(formAmount = value) }

    fun updateFormOccurredAt(value: String) = _state.update { it.copy(formOccurredAt = value) }

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
                isTransactionFormOpen = true,
            )
        }
    }

    fun closeTransactionForm() {
        clearForm()
        _state.update { it.copy(isTransactionFormOpen = false) }
    }

    fun refresh() = runRequest {
        val current = state.value
        val token = current.token ?: return@runRequest
        val summary = withContext(Dispatchers.IO) { apiClient.getSummary(token, current.month) }
        val transactions = withContext(Dispatchers.IO) {
            apiClient.getTransactions(token, current.month, type = null, category = null)
        }
        _state.update { it.copy(summary = summary, transactions = transactions, error = null) }
    }

    fun saveTransaction() = runRequest {
        val current = state.value
        val token = current.token ?: return@runRequest
        val request = TransactionRequest(
            type = current.formType,
            category = current.formCategory,
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

    fun clearForm() {
        _state.update {
            it.copy(
                editingId = null,
                formType = "expense",
                formCategory = "food",
                formAmount = "",
                formOccurredAt = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            )
        }
    }

    private fun runRequest(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                block()
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
