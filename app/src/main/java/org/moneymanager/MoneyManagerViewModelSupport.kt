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

class MoneyManagerViewModelFactory(
    private val apiClient: MoneyManagerApi,
    private val tokenStore: SessionStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MoneyManagerViewModel(apiClient, tokenStore) as T
}

internal val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
internal const val MAX_DESCRIPTION_LENGTH = 200
internal const val MAX_AMOUNT_LENGTH = 18
internal val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

internal fun List<Transaction>.sumAmounts(): BigDecimal =
    fold(BigDecimal.ZERO) { total, transaction -> total + transaction.amount.toMoney() }

internal fun String.toMoney(): BigDecimal = BigDecimal(this)

internal fun String.toLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(dateOnly()) }.getOrNull()

internal fun String.dateOnly(): String = take(10)

internal fun validateAuth(state: MoneyManagerUiState): String? = when {
    state.email.isBlank() -> "Enter your email address"
    !emailPattern.matches(state.email.trim()) -> "Enter a valid email address"
    state.password.isBlank() -> "Enter your password"
    state.isRegisterMode && state.password.length < 8 -> "Use at least 8 characters for your password"
    state.isRegisterMode && state.password != state.confirmPassword -> "Passwords do not match"
    else -> null
}

internal fun validateTransactionForm(state: MoneyManagerUiState): String? {
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

internal fun resetEditorState(state: MoneyManagerUiState): MoneyManagerUiState = state.copy(
    isTransactionFormOpen = false,
    editingId = null,
    formType = "expense",
    formCategory = state.expenseCategories.firstOrNull()?.name ?: "groceries",
    formAmount = "",
    formDescription = "",
    formOccurredAt = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    formBaseline = "",
    formError = null,
    isCategoryPickerOpen = false,
    newCategoryName = "",
    categoryError = null,
)

internal fun formFingerprint(
    type: String,
    category: String,
    amount: String,
    currency: String,
    description: String,
    occurredAt: String,
): String = listOf(type, category, amount, currency, description, occurredAt).joinToString("\u0000")

internal fun userMessage(error: Exception): String = when (error) {
    is ApiException -> error.message ?: "The server could not complete the request"
    is IOException -> "Could not connect. Check your connection and try again."
    else -> error.message ?: "Something went wrong. Try again."
}
