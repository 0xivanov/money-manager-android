package org.moneymanager

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.moneymanager.data.GrowthApi
import org.moneymanager.data.SessionStore
import org.moneymanager.model.BudgetRequest
import org.moneymanager.model.InvestmentPriceRequest
import org.moneymanager.model.InvestmentSchedule
import org.moneymanager.model.InvestmentScheduleRequest
import org.moneymanager.model.InvestmentTradeRequest
import org.moneymanager.model.NotificationPreferences
import org.moneymanager.model.TransactionSchedule
import org.moneymanager.model.TransactionScheduleRequest

internal class GrowthViewModelActions(
    private val api: GrowthApi?,
    private val tokenStore: SessionStore,
    private val ioDispatcher: CoroutineDispatcher,
    private val appScope: CoroutineScope,
    private val sessionScope: () -> CoroutineScope,
    private val state: () -> MoneyManagerUiState,
    private val updateState: ((MoneyManagerUiState) -> MoneyManagerUiState) -> Unit,
    private val handleSessionExpiry: (Exception, String) -> Boolean,
    private val sessionIsCurrent: (String) -> Boolean,
) {
    fun openDestination(destination: GrowthDestination) {
        updateState { it.copy(growthDestination = destination, growth = it.growth.copy(error = null)) }
        refreshPlanning()
    }

    fun closeDestination() {
        updateState { it.copy(growthDestination = null, growth = it.growth.copy(error = null)) }
    }

    fun refreshPlanning() = loadPlanning()

    fun refreshInvestments() = loadInvestments()

    fun createSchedule(request: TransactionScheduleRequest) = mutate(refresh = ::loadPlanning) { service, token ->
        service.createSchedule(token, request)
    }

    fun toggleSchedule(schedule: TransactionSchedule) = mutate(refresh = ::loadPlanning) { service, token ->
        if (schedule.status == "active") service.pauseSchedule(token, schedule.id) else service.resumeSchedule(token, schedule.id)
    }

    fun deleteSchedule(id: Int) = mutate(refresh = ::loadPlanning) { service, token ->
        service.deleteSchedule(token, id)
    }

    fun createBudget(request: BudgetRequest) = mutate(refresh = ::loadPlanning) { service, token ->
        service.createBudget(token, request)
    }

    fun deleteBudget(id: Int) = mutate(refresh = ::loadPlanning) { service, token ->
        service.deleteBudget(token, id)
    }

    fun updateNotificationPreferences(preferences: NotificationPreferences) =
        mutate(refresh = ::loadPlanning) { service, token ->
            service.updateNotificationPreferences(token, preferences)
        }

    fun registerPushDevice(deviceToken: String) {
        val token = state().token ?: return
        val service = api ?: return
        sessionScope().launch {
            try {
                withContext(ioDispatcher) { service.registerPushDevice(token, deviceToken) }
                    .also(tokenStore::savePushDeviceID)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                handleSessionExpiry(error, token)
            }
        }
    }

    fun openPushEvent(eventType: String) {
        updateState { current ->
            when (eventType) {
                "bank_spending", "scheduled_transaction_posted", "scheduled_transaction_due" -> current.copy(
                    selectedTab = AppTab.Transactions,
                    growthDestination = null,
                )
                "budget_alert" -> current.copy(
                    selectedTab = AppTab.Profile,
                    growthDestination = GrowthDestination.Budgets,
                )
                "investment_reminder" -> current.copy(
                    selectedTab = AppTab.Investments,
                    growthDestination = null,
                )
                else -> current
            }
        }
    }

    fun unregisterPushDevice() {
        val token = state().token ?: return
        val deviceID = tokenStore.getPushDeviceID() ?: return
        val service = api ?: return
        appScope.launch(ioDispatcher) {
            try {
                service.deletePushDevice(token, deviceID)
                tokenStore.clearPushDeviceID()
            } catch (_: Exception) {
                // A later device registration reassigns the same installation safely.
            }
        }
    }

    fun createInvestmentTrade(request: InvestmentTradeRequest) =
        mutate(refresh = ::loadInvestments) { service, token ->
            service.createInvestmentTrade(token, request)
        }

    fun deleteInvestmentTrade(id: Int) = mutate(refresh = ::loadInvestments) { service, token ->
        service.deleteInvestmentTrade(token, id)
    }

    fun setInvestmentPrice(request: InvestmentPriceRequest) =
        mutate(refresh = ::loadInvestments) { service, token ->
            service.setInvestmentPrice(token, request)
        }

    fun createInvestmentSchedule(request: InvestmentScheduleRequest) =
        mutate(refresh = ::loadInvestments) { service, token ->
            service.createInvestmentSchedule(token, request)
        }

    fun toggleInvestmentSchedule(schedule: InvestmentSchedule) =
        mutate(refresh = ::loadInvestments) { service, token ->
            if (schedule.status == "active") {
                service.pauseInvestmentSchedule(token, schedule.id)
            } else {
                service.resumeInvestmentSchedule(token, schedule.id)
            }
        }

    fun deleteInvestmentSchedule(id: Int) = mutate(refresh = ::loadInvestments) { service, token ->
        service.deleteInvestmentSchedule(token, id)
    }

    fun exportInvestments(from: String, to: String) {
        val token = state().token ?: return
        val service = api ?: return showUnavailable()
        val fromDate = from.toLocalDateOrNull()
        val toDate = to.toLocalDateOrNull()
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            updateState { it.copy(growth = it.growth.copy(error = "Choose a valid export date range")) }
            return
        }
        sessionScope().launch {
            updateState { it.copy(growth = it.growth.copy(isMutating = true, error = null)) }
            try {
                val csv = withContext(ioDispatcher) { service.exportInvestmentsCsv(token, from, to) }
                if (!sessionIsCurrent(token)) return@launch
                updateState {
                    it.copy(
                        growth = it.growth.copy(
                            investmentExportCsv = csv,
                            investmentExportFileName = "money-manager-investments-$from-to-$to.csv",
                        ),
                    )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!handleSessionExpiry(error, token) && sessionIsCurrent(token)) showError(error)
            } finally {
                if (sessionIsCurrent(token)) {
                    updateState { it.copy(growth = it.growth.copy(isMutating = false)) }
                }
            }
        }
    }

    fun clearInvestmentExport() = updateState {
        it.copy(growth = it.growth.copy(investmentExportCsv = null, investmentExportFileName = null))
    }

    fun clearError() = updateState { it.copy(growth = it.growth.copy(error = null)) }

    private fun loadPlanning() {
        val token = state().token ?: return
        val service = api ?: return showUnavailable()
        sessionScope().launch {
            updateState { it.copy(growth = it.growth.copy(isPlanningLoading = true, error = null)) }
            try {
                val result = withContext(ioDispatcher) {
                    coroutineScope {
                        val schedules = async { service.getSchedules(token) }
                        val budgets = async { service.getBudgets(token) }
                        val preferences = async { service.getNotificationPreferences(token) }
                        Triple(schedules.await(), budgets.await(), preferences.await())
                    }
                }
                if (!sessionIsCurrent(token)) return@launch
                updateState {
                    it.copy(
                        growth = it.growth.copy(
                            schedules = result.first,
                            budgets = result.second,
                            notificationPreferences = result.third,
                            isPlanningLoading = false,
                            error = null,
                        ),
                    )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!handleSessionExpiry(error, token) && sessionIsCurrent(token)) showError(error)
            } finally {
                if (sessionIsCurrent(token)) {
                    updateState { it.copy(growth = it.growth.copy(isPlanningLoading = false)) }
                }
            }
        }
    }

    private fun loadInvestments() {
        val token = state().token ?: return
        val service = api ?: return showUnavailable()
        sessionScope().launch {
            updateState { it.copy(growth = it.growth.copy(isInvestmentsLoading = true, error = null)) }
            try {
                val result = withContext(ioDispatcher) {
                    coroutineScope {
                        val portfolio = async { service.getInvestmentPortfolio(token) }
                        val trades = async { service.getInvestmentTrades(token) }
                        val schedules = async { service.getInvestmentSchedules(token) }
                        Triple(portfolio.await(), trades.await(), schedules.await())
                    }
                }
                if (!sessionIsCurrent(token)) return@launch
                updateState {
                    it.copy(
                        growth = it.growth.copy(
                            portfolio = result.first,
                            trades = result.second,
                            investmentSchedules = result.third,
                            isInvestmentsLoading = false,
                            error = null,
                        ),
                    )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!handleSessionExpiry(error, token) && sessionIsCurrent(token)) showError(error)
            } finally {
                if (sessionIsCurrent(token)) {
                    updateState { it.copy(growth = it.growth.copy(isInvestmentsLoading = false)) }
                }
            }
        }
    }

    private fun mutate(
        refresh: () -> Unit,
        operation: (GrowthApi, String) -> Any?,
    ) {
        val token = state().token ?: return
        val service = api ?: return showUnavailable()
        if (state().growth.isMutating) return
        sessionScope().launch {
            updateState { it.copy(growth = it.growth.copy(isMutating = true, error = null)) }
            try {
                withContext(ioDispatcher) { operation(service, token) }
                if (!sessionIsCurrent(token)) return@launch
                refresh()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!handleSessionExpiry(error, token) && sessionIsCurrent(token)) showError(error)
            } finally {
                if (sessionIsCurrent(token)) {
                    updateState { it.copy(growth = it.growth.copy(isMutating = false)) }
                }
            }
        }
    }

    private fun showUnavailable() {
        updateState {
            it.copy(growth = it.growth.copy(error = "This build does not include planning services"))
        }
    }

    private fun showError(error: Exception) {
        updateState { it.copy(growth = it.growth.copy(error = userMessage(error))) }
    }
}
