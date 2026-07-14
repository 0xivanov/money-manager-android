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

enum class GrowthDestination {
    Schedules,
    Budgets,
    Notifications,
}

data class GrowthUiState(
    val schedules: List<TransactionSchedule> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val notificationPreferences: NotificationPreferences? = null,
    val portfolio: InvestmentPortfolio? = null,
    val trades: List<InvestmentTrade> = emptyList(),
    val investmentSchedules: List<InvestmentSchedule> = emptyList(),
    val isPlanningLoading: Boolean = false,
    val isInvestmentsLoading: Boolean = false,
    val isMutating: Boolean = false,
    val error: String? = null,
    val investmentExportCsv: String? = null,
    val investmentExportFileName: String? = null,
)

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
    val growthDestination: GrowthDestination? = null,
    val growth: GrowthUiState = GrowthUiState(),
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
