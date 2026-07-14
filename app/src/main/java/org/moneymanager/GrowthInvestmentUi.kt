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
