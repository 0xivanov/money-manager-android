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

internal class TransactionViewModelActions(
    private val api: MoneyManagerApi,
    private val ioDispatcher: CoroutineDispatcher,
    private val sessionScope: () -> CoroutineScope,
    private val state: () -> MoneyManagerUiState,
    private val updateState: ((MoneyManagerUiState) -> MoneyManagerUiState) -> Unit,
    private val refreshCategories: suspend (String) -> Unit,
    private val reloadMonth: (String, Boolean) -> Unit,
    private val invalidateMonth: (String) -> Unit,
    private val refreshMonth: () -> Unit,
    private val handleSessionExpiry: (Exception, String) -> Boolean,
    private val sessionIsCurrent: (String) -> Boolean,
) {
    fun selectExpenseCategory(category: String) {
        updateState {
            it.copy(selectedExpenseCategory = if (it.selectedExpenseCategory == category) null else category)
        }
    }

    fun clearSelectedExpenseCategory() {
        updateState { it.copy(selectedExpenseCategory = null) }
    }

    fun updateFilterType(value: String?) = updateState {
        it.copy(filterType = value, filterCategory = null)
    }

    fun updateFilterCategory(value: String?) = updateState { it.copy(filterCategory = value) }

    fun updateSearchQuery(value: String) = updateState { it.copy(searchQuery = value) }

    fun clearTransactionFilters() = updateState {
        it.copy(filterType = null, filterCategory = null, searchQuery = "")
    }

    fun showAllTransactions() = updateState {
        it.copy(
            selectedTab = AppTab.Transactions,
            filterType = if (it.selectedExpenseCategory == null) null else "expense",
            filterCategory = it.selectedExpenseCategory,
            searchQuery = "",
        )
    }

    fun updateFormType(value: String) {
        if (state().isFormSaving) return
        updateState {
            val categories = if (value == "income") it.incomeCategories else it.expenseCategories
            it.copy(
                formType = value,
                formCategory = categories.firstOrNull()?.name ?: if (value == "income") "salary" else "food",
                formError = null,
            )
        }
    }

    fun updateFormCategory(value: String) {
        if (state().isFormSaving) return
        updateState { it.copy(formCategory = value, formError = null) }
    }

    fun chooseFormCategory(value: String) {
        if (state().isFormSaving || state().isCategoryMutating) return
        updateState {
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
        if (state().isFormSaving) return
        if (value.length <= MAX_AMOUNT_LENGTH && value.all { it.isDigit() || it == '.' || it == ',' || it.isWhitespace() }) {
            updateState { it.copy(formAmount = value, formError = null) }
        }
    }

    fun updateFormDescription(value: String) {
        if (state().isFormSaving) return
        if (value.length <= MAX_DESCRIPTION_LENGTH) {
            updateState { it.copy(formDescription = value, formError = null) }
        }
    }

    fun updateFormOccurredAt(value: String) {
        if (state().isFormSaving) return
        updateState { it.copy(formOccurredAt = value, formError = null) }
    }

    fun updateNewCategoryName(value: String) {
        if (state().isCategoryMutating) return
        updateState { it.copy(newCategoryName = value, categoryError = null) }
    }

    fun openCategoryPicker() = updateState {
        it.copy(isCategoryPickerOpen = true, categoryError = null)
    }

    fun closeCategoryPicker() {
        if (state().isCategoryMutating) return
        updateState {
            it.copy(isCategoryPickerOpen = false, newCategoryName = "", categoryError = null)
        }
    }

    fun addCategory() {
        val current = state()
        val token = current.token ?: return
        if (current.isCategoryMutating || current.isFormSaving) return
        val name = current.newCategoryName.trim()
        if (name.isBlank()) {
            updateState { it.copy(categoryError = "Enter a category name") }
            return
        }
        sessionScope().launch {
            updateState { it.copy(isCategoryMutating = true, categoryError = null) }
            try {
                val category = withContext(ioDispatcher) {
                    api.createCategory(token, current.formType, name)
                }
                refreshCategories(token)
                if (!sessionIsCurrent(token) || state().formType != current.formType || !state().isCategoryPickerOpen) return@launch
                updateState {
                    it.copy(
                        formCategory = category.name,
                        isCategoryPickerOpen = false,
                        newCategoryName = "",
                    )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!handleSessionExpiry(error, token) && sessionIsCurrent(token)) {
                    updateState { it.copy(categoryError = userMessage(error)) }
                }
            } finally {
                if (sessionIsCurrent(token)) {
                    updateState { it.copy(isCategoryMutating = false) }
                }
            }
        }
    }

    fun deleteCategory(category: Category) {
        val current = state()
        val token = current.token ?: return
        if (current.isCategoryMutating || current.isFormSaving) return
        if (category.isDefault || category.id == 0) return
        sessionScope().launch {
            updateState { it.copy(isCategoryMutating = true, categoryError = null) }
            try {
                withContext(ioDispatcher) { api.deleteCategory(token, category.id) }
                refreshCategories(token)
                if (!sessionIsCurrent(token)) return@launch
                updateState {
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
                    updateState { it.copy(categoryError = userMessage(error)) }
                }
            } finally {
                if (sessionIsCurrent(token)) {
                    updateState { it.copy(isCategoryMutating = false) }
                }
            }
        }
    }

    fun openNewTransactionForm() {
        val current = state()
        val category = current.expenseCategories.firstOrNull()?.name ?: "food"
        val currency = current.summary?.takeIf { current.hasMonthContent }?.currency ?: "EUR"
        val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val baseline = formFingerprint("expense", category, "", currency, "", date)
        updateState {
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
        updateState {
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
        updateState(::resetEditorState)
    }

    fun saveTransaction() {
        val current = state()
        val token = current.token ?: return
        if (current.isFormSaving) return
        val sourceMonth = current.month
        validateTransactionForm(current)?.let { message ->
            updateState { it.copy(formError = message) }
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

        sessionScope().launch {
            updateState { it.copy(isFormSaving = true, formError = null) }
            try {
                withContext(ioDispatcher) {
                    if (current.editingId == null) {
                        api.createTransaction(token, request)
                    } else {
                        api.updateTransaction(token, current.editingId, request)
                    }
                }
                if (!sessionIsCurrent(token)) return@launch
                val transactionMonth = YearMonth.from(LocalDate.parse(current.formOccurredAt)).format(monthFormatter)
                invalidateMonth(sourceMonth)
                invalidateMonth(transactionMonth)
                updateState { resetEditorState(it).copy(month = transactionMonth) }
                reloadMonth(transactionMonth, true)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!handleSessionExpiry(error, token) && sessionIsCurrent(token)) {
                    updateState { it.copy(formError = userMessage(error)) }
                }
            } finally {
                if (sessionIsCurrent(token)) {
                    updateState { it.copy(isFormSaving = false) }
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
        updateState {
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
        val current = state()
        val token = current.token ?: return
        val sourceMonth = current.month
        sessionScope().launch {
            updateState { it.copy(isTransactionMutating = true, monthError = null) }
            try {
                withContext(ioDispatcher) { api.deleteTransaction(token, id) }
                if (!sessionIsCurrent(token)) return@launch
                invalidateMonth(sourceMonth)
                if (state().month == sourceMonth) {
                    reloadMonth(sourceMonth, true)
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!handleSessionExpiry(error, token) && sessionIsCurrent(token)) {
                    updateState { it.copy(monthError = userMessage(error)) }
                }
            } finally {
                if (sessionIsCurrent(token)) {
                    updateState { it.copy(isTransactionMutating = false) }
                }
            }
        }
    }

    fun openExportDialog() {
        val selectedMonth = runCatching { YearMonth.parse(state().month) }.getOrDefault(YearMonth.now())
        val today = LocalDate.now()
        val end = minOf(selectedMonth.atEndOfMonth(), today)
        updateState {
            it.copy(
                isExportDialogOpen = true,
                exportFrom = selectedMonth.atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
                exportTo = end.format(DateTimeFormatter.ISO_LOCAL_DATE),
                exportError = null,
            )
        }
    }

    fun closeExportDialog() {
        if (state().isExporting) return
        updateState {
            it.copy(isExportDialogOpen = false, exportError = null)
        }
    }

    fun updateExportFrom(value: String) {
        if (state().isExporting) return
        updateState { it.copy(exportFrom = value, exportError = null) }
    }

    fun updateExportTo(value: String) {
        if (state().isExporting) return
        updateState { it.copy(exportTo = value, exportError = null) }
    }

    fun exportTransactions() {
        val current = state()
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
            updateState { it.copy(exportError = validationError) }
            return
        }

        sessionScope().launch {
            updateState { it.copy(isExporting = true, exportError = null) }
            try {
                val csv = withContext(ioDispatcher) {
                    api.exportTransactionsCsv(token, current.exportFrom, current.exportTo)
                }
                if (!sessionIsCurrent(token)) return@launch
                updateState {
                    it.copy(
                        isExportDialogOpen = false,
                        exportCsvContent = csv,
                        exportFileName = "money-manager-${current.exportFrom}-to-${current.exportTo}.csv",
                    )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!handleSessionExpiry(error, token) && sessionIsCurrent(token)) {
                    updateState { it.copy(exportError = userMessage(error)) }
                }
            } finally {
                if (sessionIsCurrent(token)) {
                    updateState { it.copy(isExporting = false) }
                }
            }
        }
    }

    fun clearExportResult() = updateState {
        it.copy(exportCsvContent = null, exportFileName = null)
    }

    fun importRevolutCsv(contents: ByteArray) {
        val current = state()
        val token = current.token ?: return
        if (current.isImporting) return
        sessionScope().launch {
            updateState { it.copy(isImporting = true, profileError = null) }
            try {
                val result = withContext(ioDispatcher) { api.importRevolutCsv(token, contents) }
                if (!sessionIsCurrent(token)) return@launch
                updateState {
                    it.copy(importMessage = "Imported ${result.imported}. Skipped ${result.skipped} duplicates and ${result.ignored} unsupported rows.")
                }
                refreshMonth()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!handleSessionExpiry(error, token) && sessionIsCurrent(token)) {
                    updateState { it.copy(profileError = userMessage(error)) }
                }
            } finally {
                if (sessionIsCurrent(token)) updateState { it.copy(isImporting = false) }
            }
        }
    }

    fun clearImportMessage() = updateState { it.copy(importMessage = null) }
}
