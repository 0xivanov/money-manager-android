package org.moneymanager

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.TimeZone
import kotlin.math.roundToInt
import org.moneymanager.model.Budget
import org.moneymanager.model.BudgetRequest
import org.moneymanager.model.InvestmentPosition
import org.moneymanager.model.InvestmentPriceRequest
import org.moneymanager.model.InvestmentSchedule
import org.moneymanager.model.InvestmentScheduleRequest
import org.moneymanager.model.InvestmentTrade
import org.moneymanager.model.InvestmentTradeRequest
import org.moneymanager.model.NotificationPreferences
import org.moneymanager.model.TransactionSchedule
import org.moneymanager.model.TransactionScheduleRequest

@Composable
fun GrowthDestinationScreen(
    destination: GrowthDestination,
    state: MoneyManagerUiState,
    viewModel: MoneyManagerViewModel,
    notificationsEnabled: Boolean,
    onEnableNotifications: () -> Unit,
) {
    BackHandler(onBack = viewModel::closeGrowthDestination)
    when (destination) {
        GrowthDestination.Schedules -> ScheduledMoneyScreen(state, viewModel)
        GrowthDestination.Budgets -> BudgetsScreen(state, viewModel)
        GrowthDestination.Notifications -> NotificationSettingsScreen(
            state,
            viewModel,
            notificationsEnabled,
            onEnableNotifications,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduledMoneyScreen(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    var showEditor by remember { mutableStateOf(false) }
    GrowthListScaffold(
        title = "Scheduled money",
        eyebrow = "PLANNING",
        onBack = viewModel::closeGrowthDestination,
        onAdd = { showEditor = true },
        isLoading = state.growth.isPlanningLoading,
        error = state.growth.error,
        onRetry = viewModel::refreshPlanning,
    ) {
        item {
            InfoCard(
                icon = Icons.Filled.CalendarMonth,
                title = "Plan with day-level accuracy",
                body = "Repeat income or expenses every day, week, or month. Auto-post only creates transactions when their date arrives.",
            )
        }
        if (!state.growth.isPlanningLoading && state.growth.schedules.isEmpty()) {
            item {
                GrowthEmptyCard(
                    "Nothing scheduled",
                    "Add rent, salary, subscriptions, or any recurring cash flow.",
                    "Create schedule",
                ) { showEditor = true }
            }
        } else {
            items(state.growth.schedules, key = { it.id }) { schedule ->
                ScheduleCard(
                    schedule = schedule,
                    enabled = !state.growth.isMutating,
                    onToggle = { viewModel.toggleSchedule(schedule) },
                    onDelete = { viewModel.deleteSchedule(schedule.id) },
                )
            }
        }
    }
    if (showEditor) {
        ScheduleEditor(
            categories = (state.expenseCategories + state.incomeCategories).map { it.name }.distinct(),
            isSaving = state.growth.isMutating,
            onDismiss = { showEditor = false },
            onSave = {
                viewModel.createSchedule(it)
                showEditor = false
            },
        )
    }
}

@Composable
private fun ScheduleCard(
    schedule: TransactionSchedule,
    enabled: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    GrowthCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GrowthIcon(
                if (schedule.type == "income") Icons.Filled.Savings else Icons.Filled.CalendarMonth,
                if (schedule.type == "income") incomeColor else expenseColor,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(schedule.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(
                        (if (schedule.type == "income") "+" else "-") +
                            BigDecimal(schedule.amount).money(schedule.currency),
                        color = if (schedule.type == "income") incomeColor else expenseColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    schedule.recurrenceLabel(),
                    color = mutedText,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    schedule.nextOccurrenceDate?.let { "Next $it" } ?: "No upcoming occurrence",
                    color = mutedText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        HorizontalDivider(color = softDivider)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (schedule.autoPost) "AUTO-POST" else "REMINDER",
                color = financeGreen,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onToggle, enabled = enabled) {
                Icon(
                    if (schedule.status == "active") Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (schedule.status == "active") "Pause" else "Resume",
                    tint = mutedText,
                )
            }
            IconButton(onClick = { confirmDelete = true }, enabled = enabled) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = expenseColor)
            }
        }
    }
    if (confirmDelete) {
        DeleteDialog(
            title = "Delete ${schedule.name}?",
            body = "Future occurrences will be removed. Existing transactions stay in your history.",
            onDismiss = { confirmDelete = false },
            onDelete = {
                confirmDelete = false
                onDelete()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleEditor(
    categories: List<String>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (TransactionScheduleRequest) -> Unit,
) {
    var type by remember { mutableStateOf("expense") }
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember(type) {
        mutableStateOf(if (type == "income") "salary" else categories.firstOrNull() ?: "other")
    }
    var frequency by remember { mutableStateOf("monthly") }
    var interval by remember { mutableStateOf("1") }
    var startDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var autoPost by remember { mutableStateOf(true) }
    val parsedAmount = parseLocalizedDecimal(amount)
    val parsedInterval = interval.toIntOrNull()
    val valid = name.isNotBlank() && parsedAmount != null && parsedAmount > BigDecimal.ZERO &&
        parsedInterval != null && parsedInterval in 1..365 && runCatching { LocalDate.parse(startDate) }.isSuccess

    GrowthSheet("New schedule", onDismiss) {
        ChoiceRow(listOf("expense", "income"), type) { type = it }
        GrowthField(name, { name = it }, "Name", "Rent")
        GrowthField(amount, { amount = it }, "Amount", "850")
        GrowthField(category, { category = it.lowercase().trim() }, "Category", "housing")
        Text("REPEATS", color = mutedText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        ChoiceRow(listOf("daily", "weekly", "monthly"), frequency) { frequency = it }
        GrowthField(interval, { interval = it.filter(Char::isDigit).take(3) }, "Every", "1")
        GrowthField(startDate, { startDate = it.take(10) }, "Start date", "YYYY-MM-DD")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Auto-post", fontWeight = FontWeight.SemiBold)
                Text("Create the transaction on its due date", color = mutedText, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = autoPost, onCheckedChange = { autoPost = it })
        }
        Button(
            onClick = {
                val date = LocalDate.parse(startDate)
                onSave(
                    TransactionScheduleRequest(
                        type = type,
                        name = name.trim(),
                        category = category.ifBlank { "other" },
                        amount = parsedAmount!!.stripTrailingZeros().toPlainString(),
                        frequency = frequency,
                        frequencyInterval = parsedInterval!!,
                        startDate = startDate,
                        dayOfWeek = if (frequency == "weekly") date.dayOfWeek.value else null,
                        dayOfMonth = if (frequency == "monthly") date.dayOfMonth else null,
                        timezone = TimeZone.getDefault().id,
                        autoPost = autoPost,
                    ),
                )
            },
            enabled = valid && !isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = buttonShape,
        ) { Text("Save schedule", fontWeight = FontWeight.Bold) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetsScreen(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    var showEditor by remember { mutableStateOf(false) }
    GrowthListScaffold(
        title = "Budgets",
        eyebrow = "SPENDING PLAN",
        onBack = viewModel::closeGrowthDestination,
        onAdd = { showEditor = true },
        isLoading = state.growth.isPlanningLoading,
        error = state.growth.error,
        onRetry = viewModel::refreshPlanning,
    ) {
        item {
            InfoCard(
                Icons.Filled.Notifications,
                "Know before you overspend",
                "Each budget tracks posted spending and can alert you near the limit.",
            )
        }
        if (!state.growth.isPlanningLoading && state.growth.budgets.isEmpty()) {
            item { GrowthEmptyCard("No budgets yet", "Set an overall or category limit.", "Create budget") { showEditor = true } }
        } else {
            items(state.growth.budgets, key = { it.id }) { budget ->
                BudgetCard(budget, !state.growth.isMutating) { viewModel.deleteBudget(budget.id) }
            }
        }
    }
    if (showEditor) {
        BudgetEditor(
            isSaving = state.growth.isMutating,
            onDismiss = { showEditor = false },
            onSave = {
                viewModel.createBudget(it)
                showEditor = false
            },
        )
    }
}

@Composable
private fun BudgetCard(budget: Budget, enabled: Boolean, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    val percent = budget.progressPercent.toFloatOrNull()?.coerceIn(0f, 100f) ?: 0f
    val warning = percent >= budget.warningThreshold
    GrowthCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(budget.name, fontWeight = FontWeight.Bold)
                Text(
                    budget.category?.let(::categoryTitle) ?: "All spending",
                    color = mutedText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text("${percent.roundToInt()}%", color = if (warning) expenseColor else financeGreen, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(9.dp),
            color = if (warning) expenseColor else financeGreen,
            trackColor = softGreenSurface,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${BigDecimal(budget.spentAmount).money(budget.currency)} of ${BigDecimal(budget.amount).money(budget.currency)}",
                color = mutedText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            Text(budget.period.uppercase(), color = mutedText, style = MaterialTheme.typography.labelSmall)
            IconButton(onClick = { confirmDelete = true }, enabled = enabled) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete budget", tint = expenseColor)
            }
        }
    }
    if (confirmDelete) {
        DeleteDialog("Delete ${budget.name}?", "The budget history and limit will be removed.", { confirmDelete = false }) {
            confirmDelete = false
            onDelete()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetEditor(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (BudgetRequest) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("monthly") }
    var threshold by remember { mutableStateOf("80") }
    val parsedAmount = parseLocalizedDecimal(amount)
    val parsedThreshold = threshold.toIntOrNull()
    val valid = name.isNotBlank() && parsedAmount != null && parsedAmount > BigDecimal.ZERO &&
        parsedThreshold != null && parsedThreshold in 1..100
    GrowthSheet("New budget", onDismiss) {
        GrowthField(name, { name = it }, "Name", "Monthly spending")
        GrowthField(amount, { amount = it }, "Limit", "1200")
        GrowthField(category, { category = it.lowercase().trim() }, "Category (optional)", "food")
        ChoiceRow(listOf("weekly", "monthly"), period) { period = it }
        GrowthField(threshold, { threshold = it.filter(Char::isDigit).take(3) }, "Warn me at %", "80")
        Button(
            onClick = {
                onSave(
                    BudgetRequest(
                        name = name.trim(),
                        category = category.ifBlank { null },
                        amount = parsedAmount!!.stripTrailingZeros().toPlainString(),
                        period = period,
                        warningThreshold = parsedThreshold!!,
                    ),
                )
            },
            enabled = valid && !isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = buttonShape,
        ) { Text("Save budget", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun NotificationSettingsScreen(
    state: MoneyManagerUiState,
    viewModel: MoneyManagerViewModel,
    notificationsEnabled: Boolean,
    onEnableNotifications: () -> Unit,
) {
    val preferences = state.growth.notificationPreferences
    GrowthListScaffold(
        title = "Notifications",
        eyebrow = "ALERTS",
        onBack = viewModel::closeGrowthDestination,
        isLoading = state.growth.isPlanningLoading,
        error = state.growth.error,
        onRetry = viewModel::refreshPlanning,
    ) {
        if (!notificationsEnabled) {
            item {
                GrowthEmptyCard(
                    "Notifications are off",
                    "Allow notifications on this device before enabling money alerts.",
                    "Allow notifications",
                    onEnableNotifications,
                )
            }
        }
        if (preferences != null) {
            item {
                GrowthCard {
                    NotificationToggle("Bank spending", "A connected account reports new spending", preferences.bankSpending) {
                        viewModel.updateNotificationPreferences(preferences.copy(bankSpending = it))
                    }
                    HorizontalDivider(color = softDivider)
                    NotificationToggle("Budget alerts", "You are approaching or have reached a limit", preferences.budgetAlerts) {
                        viewModel.updateNotificationPreferences(preferences.copy(budgetAlerts = it))
                    }
                    HorizontalDivider(color = softDivider)
                    NotificationToggle("Scheduled money", "A planned income or expense is due", preferences.scheduledMoney) {
                        viewModel.updateNotificationPreferences(preferences.copy(scheduledMoney = it))
                    }
                    HorizontalDivider(color = softDivider)
                    NotificationToggle("Investment reminders", "A recurring investment plan is due", preferences.investmentReminders) {
                        viewModel.updateNotificationPreferences(preferences.copy(investmentReminders = it))
                    }
                }
            }
            item {
                InfoCard(
                    Icons.Filled.Notifications,
                    "Delivery is being connected",
                    "Your preferences are saved. Remote push delivery requires the final APNs and FCM setup.",
                )
            }
        }
    }
}

@Composable
private fun NotificationToggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = mutedText, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrowthInvestmentScreen(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    var showTradeEditor by remember { mutableStateOf(false) }
    var showPlanEditor by remember { mutableStateOf(false) }
    var pricePosition by remember { mutableStateOf<InvestmentPosition?>(null) }
    var deleteTrade by remember { mutableStateOf<InvestmentTrade?>(null) }
    val portfolio = state.growth.portfolio
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("WEALTH", color = mutedText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("Invest", color = nearBlack, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = viewModel::refreshInvestments) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = mutedText)
                }
                Surface(color = financeGreen, shape = CircleShape) {
                    IconButton(onClick = { showTradeEditor = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add trade", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
        state.growth.error?.let { item { GrowthError(it, viewModel::refreshInvestments) } }
        if (state.growth.isInvestmentsLoading && portfolio == null) {
            item { GrowthLoading() }
        } else if (portfolio != null) {
            item { PortfolioCard(portfolio) }
            if (portfolio.positions.isEmpty()) {
                item { GrowthEmptyCard("No investments yet", "Record a buy or sell to build your portfolio.", "Add trade") { showTradeEditor = true } }
            } else {
                item { GrowthSectionTitle("Holdings") }
                items(portfolio.positions, key = { "${it.assetType}-${it.symbol}-${it.broker}" }) { position ->
                    PositionCard(position) { pricePosition = position }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GrowthSectionTitle("Investment plans", Modifier.weight(1f))
                TextButton(onClick = { showPlanEditor = true }) { Text("Add plan") }
            }
        }
        if (state.growth.investmentSchedules.isEmpty()) {
            item { Text("No recurring reminders", color = mutedText) }
        } else {
            items(state.growth.investmentSchedules, key = { "plan-${it.id}" }) { schedule ->
                InvestmentPlanCard(schedule, viewModel)
            }
        }
        item { GrowthSectionTitle("Recent activity") }
        if (state.growth.trades.isEmpty()) {
            item { Text("No trades recorded", color = mutedText) }
        } else {
            items(state.growth.trades.take(8), key = { "trade-${it.id}" }) { trade ->
                TradeCard(trade) { deleteTrade = trade }
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    viewModel.exportInvestments(LocalDate.now().minusYears(20).toString(), LocalDate.now().toString())
                },
                enabled = !state.growth.isMutating,
                modifier = Modifier.fillMaxWidth(),
                shape = buttonShape,
            ) {
                Icon(Icons.Filled.FileDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export investment audit CSV")
            }
        }
        item {
            InfoCard(
                Icons.Filled.AutoGraph,
                "Manual tracking by design",
                "Prices and trades remain under your control. Broker and exchange connections are not enabled yet.",
            )
        }
    }
    if (showTradeEditor) {
        TradeEditor(state.growth.isMutating, { showTradeEditor = false }) {
            viewModel.createInvestmentTrade(it)
            showTradeEditor = false
        }
    }
    if (showPlanEditor) {
        InvestmentPlanEditor(state.growth.isMutating, { showPlanEditor = false }) {
            viewModel.createInvestmentSchedule(it)
            showPlanEditor = false
        }
    }
    pricePosition?.let { position ->
        PriceEditor(position, state.growth.isMutating, { pricePosition = null }) {
            viewModel.setInvestmentPrice(it)
            pricePosition = null
        }
    }
    deleteTrade?.let { trade ->
        DeleteDialog(
            "Delete ${trade.side} of ${trade.symbol}?",
            "Portfolio cost basis and performance will be recalculated.",
            { deleteTrade = null },
        ) {
            deleteTrade = null
            viewModel.deleteInvestmentTrade(trade.id)
        }
    }
}

@Composable
private fun PortfolioCard(portfolio: org.moneymanager.model.InvestmentPortfolio) {
    Surface(color = invertedSurface, shape = cardShape) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("PORTFOLIO VALUE", color = inverseText.copy(alpha = 0.65f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(
                portfolio.currentValue?.let { BigDecimal(it).money(portfolio.currency) } ?: "Price update needed",
                color = inverseText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Row {
                PortfolioMetric("INVESTED", BigDecimal(portfolio.investedAmount).money(portfolio.currency), Modifier.weight(1f))
                PortfolioMetric("REALIZED", BigDecimal(portfolio.realizedProfit).signedMoney(portfolio.currency), Modifier.weight(1f))
            }
            if (portfolio.missingPrices > 0) {
                Text(
                    "${portfolio.missingPrices} holding${if (portfolio.missingPrices == 1) " needs" else "s need"} a current price. Totals are hidden until complete.",
                    color = cryptoColor,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                portfolio.unrealizedProfit?.let {
                    Text("Unrealized ${BigDecimal(it).signedMoney(portfolio.currency)}", color = incomeColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PortfolioMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = inverseText.copy(alpha = 0.55f), style = MaterialTheme.typography.labelSmall)
        Text(value, color = inverseText, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PositionCard(position: InvestmentPosition, onSetPrice: () -> Unit) {
    GrowthCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GrowthIcon(
                if (position.assetType == "crypto") Icons.Filled.AutoGraph else Icons.Filled.Savings,
                if (position.assetType == "crypto") cryptoColor else stocksColor,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(position.assetName.ifBlank { position.symbol }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${position.symbol} · ${position.quantity} · ${position.broker.brokerLabel()}", color = mutedText, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    position.currentValue?.let { BigDecimal(it).money(position.currency) } ?: "No price",
                    fontWeight = FontWeight.Bold,
                )
                position.unrealizedPercent?.let { Text("${it}%", color = amountColor(BigDecimal(position.unrealizedProfit ?: "0")), style = MaterialTheme.typography.bodySmall) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Avg ${BigDecimal(position.averageCost).money(position.currency)}", color = mutedText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onSetPrice) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("Set price")
            }
        }
    }
}

@Composable
private fun InvestmentPlanCard(schedule: InvestmentSchedule, viewModel: MoneyManagerViewModel) {
    var confirmDelete by remember { mutableStateOf(false) }
    GrowthCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GrowthIcon(Icons.Filled.CalendarMonth, financeGreen)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${schedule.assetName} plan", fontWeight = FontWeight.Bold)
                Text(
                    "${BigDecimal(schedule.amount).money(schedule.currency)} · ${schedule.frequencyLabel()}",
                    color = mutedText,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(schedule.nextOccurrence?.let { "Next $it" } ?: schedule.status, color = mutedText, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { viewModel.toggleInvestmentSchedule(schedule) }) {
                Icon(if (schedule.status == "active") Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null, tint = mutedText)
            }
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete plan", tint = expenseColor)
            }
        }
    }
    if (confirmDelete) {
        DeleteDialog("Delete ${schedule.symbol} plan?", "This removes future reminders only.", { confirmDelete = false }) {
            confirmDelete = false
            viewModel.deleteInvestmentSchedule(schedule.id)
        }
    }
}

@Composable
private fun TradeCard(trade: InvestmentTrade, onDelete: () -> Unit) {
    GrowthCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                trade.side.uppercase(),
                color = if (trade.side == "buy") financeGreen else expenseColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(42.dp),
            )
            Column(Modifier.weight(1f)) {
                Text("${trade.symbol} · ${trade.quantity}", fontWeight = FontWeight.SemiBold)
                Text("${trade.occurredAt} · ${trade.broker.brokerLabel()}", color = mutedText, style = MaterialTheme.typography.bodySmall)
            }
            Text(BigDecimal(trade.pricePerUnit).money(trade.currency), fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete trade", tint = expenseColor) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TradeEditor(isSaving: Boolean, onDismiss: () -> Unit, onSave: (InvestmentTradeRequest) -> Unit) {
    var assetType by remember { mutableStateOf("crypto") }
    var symbol by remember { mutableStateOf("BTC") }
    var name by remember { mutableStateOf("Bitcoin") }
    var broker by remember { mutableStateOf("revolut_x") }
    var side by remember { mutableStateOf("buy") }
    var quantity by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var fees by remember { mutableStateOf("0") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    val q = parseLocalizedDecimal(quantity)
    val p = parseLocalizedDecimal(price)
    val f = parseLocalizedDecimal(fees)
    val valid = symbol.isNotBlank() && name.isNotBlank() && q != null && q > BigDecimal.ZERO && p != null && p > BigDecimal.ZERO && f != null && f >= BigDecimal.ZERO && runCatching { LocalDate.parse(date) }.isSuccess
    GrowthSheet("Record trade", onDismiss) {
        ChoiceRow(listOf("crypto", "stock"), assetType) {
            assetType = it
            if (it == "crypto") {
                symbol = "BTC"; name = "Bitcoin"; broker = "revolut_x"
            } else {
                symbol = "AAPL"; name = "Apple"; broker = "trading212"
            }
        }
        ChoiceRow(listOf("buy", "sell"), side) { side = it }
        GrowthField(symbol, { symbol = it.uppercase().take(12) }, "Symbol", "BTC")
        GrowthField(name, { name = it }, "Asset name", "Bitcoin")
        GrowthField(broker, { broker = it.lowercase().replace(' ', '_') }, "Broker", "revolut_x")
        GrowthField(quantity, { quantity = it }, "Quantity", "0.01")
        GrowthField(price, { price = it }, "Price per unit", "58000")
        GrowthField(fees, { fees = it }, "Fees", "0")
        GrowthField(date, { date = it.take(10) }, "Trade date", "YYYY-MM-DD")
        Button(
            onClick = {
                onSave(
                    InvestmentTradeRequest(
                        assetType = assetType,
                        symbol = symbol,
                        assetName = name,
                        broker = broker,
                        side = side,
                        quantity = q!!.stripTrailingZeros().toPlainString(),
                        pricePerUnit = p!!.stripTrailingZeros().toPlainString(),
                        fees = f!!.stripTrailingZeros().toPlainString(),
                        occurredAt = date,
                    ),
                )
            },
            enabled = valid && !isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = buttonShape,
        ) { Text("Save trade", fontWeight = FontWeight.Bold) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvestmentPlanEditor(isSaving: Boolean, onDismiss: () -> Unit, onSave: (InvestmentScheduleRequest) -> Unit) {
    var assetType by remember { mutableStateOf("crypto") }
    var symbol by remember { mutableStateOf("BTC") }
    var name by remember { mutableStateOf("Bitcoin") }
    var broker by remember { mutableStateOf("revolut_x") }
    var amount by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("monthly") }
    var interval by remember { mutableStateOf("1") }
    var startDate by remember { mutableStateOf(LocalDate.now().toString()) }
    val parsedAmount = parseLocalizedDecimal(amount)
    val parsedInterval = interval.toIntOrNull()
    val valid = symbol.isNotBlank() && name.isNotBlank() && parsedAmount != null && parsedAmount > BigDecimal.ZERO &&
        parsedInterval != null && parsedInterval in 1..365 && runCatching { LocalDate.parse(startDate) }.isSuccess
    GrowthSheet("Investment reminder", onDismiss) {
        ChoiceRow(listOf("crypto", "stock"), assetType) {
            assetType = it
            if (it == "crypto") { symbol = "BTC"; name = "Bitcoin"; broker = "revolut_x" }
            else { symbol = "AAPL"; name = "Apple"; broker = "trading212" }
        }
        GrowthField(symbol, { symbol = it.uppercase().take(12) }, "Symbol", "BTC")
        GrowthField(name, { name = it }, "Asset name", "Bitcoin")
        GrowthField(broker, { broker = it.lowercase().replace(' ', '_') }, "Broker", "revolut_x")
        GrowthField(amount, { amount = it }, "Planned amount", "100")
        ChoiceRow(listOf("daily", "weekly", "monthly"), frequency) { frequency = it }
        GrowthField(interval, { interval = it.filter(Char::isDigit).take(3) }, "Every", "1")
        GrowthField(startDate, { startDate = it.take(10) }, "First reminder", "YYYY-MM-DD")
        Button(
            onClick = {
                val date = LocalDate.parse(startDate)
                onSave(
                    InvestmentScheduleRequest(
                        assetType = assetType,
                        symbol = symbol,
                        assetName = name,
                        broker = broker,
                        amount = parsedAmount!!.stripTrailingZeros().toPlainString(),
                        frequency = frequency,
                        frequencyInterval = parsedInterval!!,
                        startDate = startDate,
                        dayOfWeek = if (frequency == "weekly") date.dayOfWeek.value else null,
                        dayOfMonth = if (frequency == "monthly") date.dayOfMonth else null,
                        timezone = TimeZone.getDefault().id,
                    ),
                )
            },
            enabled = valid && !isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = buttonShape,
        ) { Text("Save reminder", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun PriceEditor(
    position: InvestmentPosition,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (InvestmentPriceRequest) -> Unit,
) {
    var value by remember { mutableStateOf(position.currentPrice.orEmpty()) }
    val price = parseLocalizedDecimal(value)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update ${position.symbol} price") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Manual snapshot in ${position.currency}. Portfolio totals will be recalculated.", color = mutedText)
                GrowthField(value, { value = it }, "Current price", "0")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        InvestmentPriceRequest(
                            assetType = position.assetType,
                            symbol = position.symbol,
                            currency = position.currency,
                            price = price!!.stripTrailingZeros().toPlainString(),
                        ),
                    )
                },
                enabled = price != null && price > BigDecimal.ZERO && !isSaving,
            ) { Text("Update") }
        },
    )
}

@Composable
private fun GrowthListScaffold(
    title: String,
    eyebrow: String,
    onBack: () -> Unit,
    onAdd: (() -> Unit)? = null,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { GrowthHeader(title, eyebrow, onBack, onAdd) }
        if (error != null) item { GrowthError(error, onRetry) }
        if (isLoading) item { GrowthLoading() }
        content()
    }
}

@Composable
private fun GrowthHeader(title: String, eyebrow: String, onBack: () -> Unit, onAdd: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(color = appSurface, shape = CircleShape) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(eyebrow, color = financeGreen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        if (onAdd != null) {
            Surface(color = financeGreen, shape = CircleShape) {
                IconButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.onPrimary) }
            }
        }
    }
}

@Composable
private fun GrowthCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = appSurface, shape = cardShape, tonalElevation = 1.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun GrowthIcon(icon: ImageVector, color: Color) {
    Box(Modifier.size(44.dp).background(color.copy(alpha = 0.14f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun InfoCard(icon: ImageVector, title: String, body: String) {
    Surface(color = softGreenCard, shape = cardShape) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = financeGreen)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(body, color = mutedText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun GrowthEmptyCard(title: String, body: String, action: String, onClick: () -> Unit) {
    GrowthCard {
        Text(title, fontWeight = FontWeight.Bold)
        Text(body, color = mutedText)
        Button(onClick = onClick, shape = buttonShape) { Text(action) }
    }
}

@Composable
private fun GrowthError(message: String, onRetry: () -> Unit) {
    Surface(color = expenseColor.copy(alpha = 0.12f), shape = cardShape) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, color = expenseColor, modifier = Modifier.weight(1f))
            TextButton(onClick = onRetry) { Text("Retry", color = expenseColor) }
        }
    }
}

@Composable
private fun GrowthLoading() {
    Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
    }
}

@Composable
private fun GrowthSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier = modifier, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GrowthSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = appBackground) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            item { Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = content) }
        }
    }
}

@Composable
private fun GrowthField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = inputShape,
    )
}

@Composable
private fun ChoiceRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(option.replaceFirstChar(Char::uppercase)) },
            )
        }
    }
}

@Composable
private fun DeleteDialog(title: String, body: String, onDismiss: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = expenseColor)) {
                Text("Delete")
            }
        },
    )
}

private fun TransactionSchedule.recurrenceLabel(): String {
    val every = if (frequencyInterval == 1) "Every" else "Every $frequencyInterval"
    return when (frequency) {
        "daily" -> if (frequencyInterval == 1) "Every day" else "$every days"
        "weekly" -> if (frequencyInterval == 1) "Every week" else "$every weeks"
        else -> if (frequencyInterval == 1) "Every month" else "$every months"
    }
}

private fun InvestmentSchedule.frequencyLabel(): String =
    when (frequency) {
        "daily" -> if (frequencyInterval == 1) "daily" else "every $frequencyInterval days"
        "weekly" -> if (frequencyInterval == 1) "weekly" else "every $frequencyInterval weeks"
        else -> if (frequencyInterval == 1) "monthly" else "every $frequencyInterval months"
    }

private fun String.brokerLabel(): String = when (this) {
    "revolut_x" -> "Revolut X"
    "trading212" -> "Trading 212"
    "manual" -> "Manual"
    else -> replace('_', ' ').replaceFirstChar(Char::uppercase)
}
