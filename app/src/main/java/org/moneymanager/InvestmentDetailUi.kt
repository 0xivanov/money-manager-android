package org.moneymanager

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import org.moneymanager.model.InvestmentPortfolioHistoryPoint
import org.moneymanager.model.InvestmentPosition
import org.moneymanager.model.InvestmentTrade

@Composable
internal fun PrivatePortfolioValue(
    value: String,
    hidden: Boolean,
    modifier: Modifier = Modifier,
    hiddenAccessibilityLabel: String = "Portfolio balance hidden",
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyleValue,
    fontWeight: FontWeight? = null,
) {
    Text(
        text = if (hidden) "••••••" else value,
        modifier = if (hidden) {
            modifier.clearAndSetSemantics { contentDescription = hiddenAccessibilityLabel }
        } else {
            modifier
        },
        color = color,
        style = style,
        fontWeight = fontWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private val LocalTextStyleValue: TextStyle
    @Composable get() = MaterialTheme.typography.bodyMedium

@Composable
internal fun InvestmentPriceFreshness(positions: List<InvestmentPosition>, color: Color = mutedText) {
    val latest = remember(positions) {
        positions.mapNotNull { position -> position.priceAsOf?.let(::parseInvestmentInstant) }.maxOrNull()
    } ?: return
    var now by remember(latest) { mutableStateOf(Instant.now()) }
    LaunchedEffect(latest) {
        while (true) {
            delay(30_000)
            now = Instant.now()
        }
    }
    val relative = relativePriceAge(latest, now)
    Text(
        text = "Prices · updated $relative",
        color = color,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.clearAndSetSemantics { contentDescription = "Prices updated $relative" },
    )
}

@Composable
internal fun InvestmentHistoryCard(state: MoneyManagerUiState, onExpand: () -> Unit) {
    val history = state.growth.portfolioHistory
    val points = history?.points.orEmpty()
    val currency = history?.currency ?: state.growth.portfolio?.currency ?: "EUR"
    val unsupportedPositions = history?.unsupportedPositions ?: 0
    Surface(color = appSurface, shape = cardShape, tonalElevation = 1.dp) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Portfolio history", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(historyRangeTitle(history?.range ?: state.growth.historyRange), color = mutedText, style = MaterialTheme.typography.bodySmall)
                }
                points.lastOrNull()?.let { last ->
                    PrivatePortfolioValue(
                        value = BigDecimal(last.value).money(currency),
                        hidden = state.hidePortfolioBalances,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Surface(color = appBackground, shape = RoundedCornerShape(11.dp)) {
                    IconButton(onClick = onExpand, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.OpenInFull, contentDescription = "Expand portfolio history", tint = financeGreen)
                    }
                }
            }
            InvestmentHistoryContent(
                points = points,
                hidden = state.hidePortfolioBalances,
                loading = state.growth.isInvestmentHistoryLoading,
                error = state.growth.investmentHistoryError,
                height = 190.dp,
            )
            if (points.isNotEmpty() && !state.hidePortfolioBalances) {
                HistoryLegend()
            }
            if (unsupportedPositions > 0) {
                Text(
                    if (unsupportedPositions == 1) "1 stock position is excluded" else "$unsupportedPositions stock positions are excluded",
                    color = mutedText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentHistoryScreen(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    val history = state.growth.portfolioHistory
    val points = history?.points.orEmpty()
    val currency = history?.currency ?: state.growth.portfolio?.currency ?: "EUR"
    val unsupportedPositions = history?.unsupportedPositions ?: 0
    BackHandler(onBack = viewModel::closeGrowthDestination)
    PullToRefreshBox(
        isRefreshing = state.growth.isInvestmentHistoryLoading,
        onRefresh = { viewModel.setInvestmentHistoryRange(state.growth.historyRange) },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { GrowthHeader("Portfolio history", "WEALTH", viewModel::closeGrowthDestination, null) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("1m" to "1M", "3m" to "3M", "1y" to "1Y").forEach { (range, title) ->
                        FilterChip(
                            selected = state.growth.historyRange == range,
                            onClick = { viewModel.setInvestmentHistoryRange(range) },
                            label = { Text(title) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item { InvestmentPriceFreshness(state.growth.portfolio?.positions.orEmpty()) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HistoryMetric(
                        "VALUE",
                        points.lastOrNull()?.let { BigDecimal(it.value).money(currency) } ?: "—",
                        state.hidePortfolioBalances,
                        Modifier.weight(1f),
                    )
                    HistoryMetric(
                        "INVESTED",
                        points.lastOrNull()?.let { BigDecimal(it.investedAmount).money(currency) } ?: "—",
                        state.hidePortfolioBalances,
                        Modifier.weight(1f),
                    )
                    val change = points.lastOrNull()?.let { BigDecimal(it.value) - BigDecimal(it.investedAmount) }
                    HistoryMetric(
                        "RETURN",
                        change?.signedMoney(currency) ?: "—",
                        state.hidePortfolioBalances,
                        Modifier.weight(1f),
                        change?.let { amountColor(it) } ?: mutedText,
                    )
                }
            }
            item {
                Surface(color = appSurface, shape = cardShape, tonalElevation = 1.dp) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Portfolio value", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            points.lastOrNull()?.let {
                                PrivatePortfolioValue(BigDecimal(it.value).money(currency), state.hidePortfolioBalances, fontWeight = FontWeight.Bold)
                            }
                        }
                        InvestmentHistoryContent(
                            points = points,
                            hidden = state.hidePortfolioBalances,
                            loading = state.growth.isInvestmentHistoryLoading,
                            error = state.growth.investmentHistoryError,
                            height = 340.dp,
                        )
                        if (points.isNotEmpty() && !state.hidePortfolioBalances) HistoryLegend()
                    }
                }
            }
            if (unsupportedPositions > 0) {
                item {
                    Text(
                        if (unsupportedPositions == 1) "1 stock position is excluded" else "$unsupportedPositions stock positions are excluded",
                        color = mutedText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentTradesScreen(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    var query by remember { mutableStateOf("") }
    var side by remember { mutableStateOf<String?>(null) }
    var deleteTrade by remember { mutableStateOf<InvestmentTrade?>(null) }
    val filtered = remember(state.growth.trades, query, side) {
        state.growth.trades.filter { trade ->
            (side == null || trade.side.equals(side, true)) && (
                query.isBlank() || trade.symbol.contains(query, true) || trade.assetName.contains(query, true) ||
                    trade.broker.brokerLabel().contains(query, true) || trade.notes.contains(query, true)
                )
        }
    }
    val buckets = remember(filtered) {
        filtered.groupBy { it.occurredAt.take(10) }.toSortedMap(compareByDescending { it })
    }
    BackHandler(onBack = viewModel::closeGrowthDestination)
    PullToRefreshBox(
        isRefreshing = state.growth.isInvestmentsLoading,
        onRefresh = viewModel::refreshInvestments,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { GrowthHeader("Investment activity", "WEALTH", viewModel::closeGrowthDestination, null) }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = inputShape,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("Search investments") },
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to "All", "buy" to "Buys", "sell" to "Sells").forEach { (value, title) ->
                        FilterChip(selected = side == value, onClick = { side = value }, label = { Text(title) })
                    }
                }
            }
            if (filtered.isEmpty()) {
                item {
                    GrowthEmptyMessage(
                        title = if (state.growth.trades.isEmpty()) "No investment activity yet" else "No matching trades",
                        body = if (state.growth.trades.isEmpty()) "Your buys and sells will appear here." else "Try another search or filter.",
                    )
                }
            } else {
                buckets.forEach { (date, trades) ->
                    item(key = "investment-day-$date") {
                        Text(investmentDateTitle(date), color = mutedText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    items(trades, key = { "investment-trade-${it.id}" }) { trade ->
                        InvestmentActivityRow(
                            trade,
                            hidePortfolioBalances = state.hidePortfolioBalances,
                            onDelete = { deleteTrade = trade },
                        )
                    }
                }
            }
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
private fun HistoryMetric(
    label: String,
    value: String,
    hidden: Boolean,
    modifier: Modifier,
    color: Color = nearBlack,
) {
    Surface(modifier = modifier, color = appSurface, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, color = mutedText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            PrivatePortfolioValue(value, hidden, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun InvestmentHistoryContent(
    points: List<InvestmentPortfolioHistoryPoint>,
    hidden: Boolean,
    loading: Boolean,
    error: String?,
    height: Dp,
) {
    when {
        hidden && points.isNotEmpty() -> HistoryMessage(height, Icons.Filled.VisibilityOff, "Portfolio balances hidden", null, financeGreen)
        loading && points.isEmpty() -> Box(Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                Text("Building your portfolio history", color = mutedText, style = MaterialTheme.typography.bodySmall)
            }
        }
        error != null && points.isEmpty() -> HistoryMessage(height, Icons.Filled.AutoGraph, "Portfolio history is unavailable", error, mutedText)
        points.isEmpty() -> HistoryMessage(height, Icons.Filled.AutoGraph, "No portfolio history yet", "Your chart will appear after the first BTC or ETH trade.", financeGreen)
        else -> InvestmentHistoryChart(points, height)
    }
}

@Composable
private fun InvestmentHistoryChart(points: List<InvestmentPortfolioHistoryPoint>, height: Dp) {
    val plotted = remember(points) {
        points.sortedBy { it.asOf }.mapNotNull { point ->
            val value = point.value.toFloatOrNull()
            val invested = point.investedAmount.toFloatOrNull()
            if (value == null || invested == null) null else Triple(point, value, invested)
        }
    }
    if (plotted.isEmpty()) return
    val lineColor = financeGreen
    val investedColor = mutedText
    val gridColor = softDivider
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clearAndSetSemantics {
                    contentDescription = "Portfolio value from ${plotted.first().second} to ${plotted.last().second}"
                },
        ) {
            val allValues = plotted.flatMap { listOf(it.second, it.third) }
            val minimum = allValues.minOrNull() ?: 0f
            val maximum = allValues.maxOrNull() ?: minimum + 1f
            val range = (maximum - minimum).takeIf { it > 0f } ?: 1f
            val topPadding = 10.dp.toPx()
            val bottomPadding = 10.dp.toPx()
            val chartHeight = size.height - topPadding - bottomPadding
            fun x(index: Int): Float = if (plotted.size == 1) size.width / 2f else size.width * index / (plotted.size - 1f)
            fun y(value: Float): Float = topPadding + chartHeight * (1f - (value - minimum) / range)

            repeat(4) { index ->
                val gridY = topPadding + chartHeight * index / 3f
                drawLine(gridColor, Offset(0f, gridY), Offset(size.width, gridY), strokeWidth = 1.dp.toPx())
            }

            if (plotted.size == 1) {
                val (_, value, invested) = plotted.single()
                val centerX = x(0)
                drawCircle(
                    color = investedColor,
                    radius = 5.dp.toPx(),
                    center = Offset(centerX, y(invested)),
                    style = Stroke(width = 2.dp.toPx()),
                )
                drawCircle(
                    color = lineColor.copy(alpha = 0.22f),
                    radius = 8.dp.toPx(),
                    center = Offset(centerX, y(value)),
                )
                drawCircle(
                    color = lineColor,
                    radius = 4.dp.toPx(),
                    center = Offset(centerX, y(value)),
                )
            } else {
                val valuePath = Path()
                val investedPath = Path()
                plotted.forEachIndexed { index, (_, value, invested) ->
                    if (index == 0) {
                        valuePath.moveTo(x(index), y(value))
                        investedPath.moveTo(x(index), y(invested))
                    } else {
                        valuePath.lineTo(x(index), y(value))
                        investedPath.lineTo(x(index), y(invested))
                    }
                }
                val areaPath = Path().apply {
                    addPath(valuePath)
                    lineTo(x(plotted.lastIndex), size.height - bottomPadding)
                    lineTo(x(0), size.height - bottomPadding)
                    close()
                }
                drawPath(areaPath, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.02f))))
                drawPath(valuePath, lineColor, style = Stroke(width = 2.5.dp.toPx()))
                drawPath(
                    investedPath,
                    investedColor,
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
                )
            }
        }
        if (plotted.size == 1) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(historyAxisDate(plotted.single().first.asOf), color = mutedText, style = MaterialTheme.typography.labelSmall)
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(historyAxisDate(plotted.first().first.asOf), color = mutedText, style = MaterialTheme.typography.labelSmall)
                Text(historyAxisDate(plotted.last().first.asOf), color = mutedText, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun HistoryLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        HistoryLegendItem(financeGreen, "Value")
        HistoryLegendItem(mutedText, "Invested")
    }
}

@Composable
private fun HistoryLegendItem(color: Color, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.width(20.dp).height(2.dp).background(color, RoundedCornerShape(2.dp)))
        Text(title, color = mutedText, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HistoryMessage(height: Dp, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String?, tint: Color) {
    Column(
        modifier = Modifier.fillMaxWidth().height(height).clearAndSetSemantics {
            contentDescription = listOfNotNull(title, body).joinToString(". ")
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.height(10.dp))
        Text(title, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        if (body != null) {
            Spacer(Modifier.height(4.dp))
            Text(body, color = mutedText, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun GrowthEmptyMessage(title: String, body: String) {
    Surface(color = appSurface, shape = cardShape) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.AutoGraph, contentDescription = null, tint = financeGreen)
            Text(title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(body, color = mutedText, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun InvestmentActivityRow(
    trade: InvestmentTrade,
    hidePortfolioBalances: Boolean,
    onDelete: () -> Unit,
) {
    Surface(color = appSurface, shape = cardShape) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(40.dp).background(
                    (if (trade.side == "buy") incomeColor else expenseColor).copy(alpha = 0.12f),
                    RoundedCornerShape(12.dp),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (trade.side == "buy") "↙" else "↗", color = if (trade.side == "buy") incomeColor else expenseColor, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("${trade.side.replaceFirstChar(Char::uppercase)} ${trade.symbol}", fontWeight = FontWeight.SemiBold)
                PrivatePortfolioValue(
                    value = "${formatInvestmentQuantity(trade.quantity)} ${trade.symbol} @ ${BigDecimal(trade.pricePerUnit).money(trade.currency)}",
                    hidden = hidePortfolioBalances,
                    hiddenAccessibilityLabel = "Trade quantity and execution price hidden",
                    color = mutedText,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    listOfNotNull(
                        trade.broker.brokerLabel(),
                        trade.priceProvider?.let { "Price by ${it.brokerLabel()}" },
                    ).joinToString(" · "),
                    color = mutedText,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            PrivatePortfolioValue(
                value = BigDecimal(trade.amount).money(trade.currency),
                hidden = hidePortfolioBalances,
                hiddenAccessibilityLabel = "Trade amount hidden",
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete trade", tint = expenseColor, modifier = Modifier.size(19.dp))
            }
        }
    }
}

private fun parseInvestmentInstant(value: String): Instant? =
    runCatching { Instant.parse(value) }.getOrElse {
        runCatching { LocalDate.parse(value.take(10)).atStartOfDay(java.time.ZoneOffset.UTC).toInstant() }.getOrNull()
    }

private fun relativePriceAge(updatedAt: Instant, now: Instant): String {
    val seconds = Duration.between(updatedAt, now).seconds.coerceAtLeast(0)
    return when {
        seconds < 60 -> "just now"
        seconds < 3_600 -> "${seconds / 60} min ago"
        seconds < 86_400 -> "${seconds / 3_600} hr ago"
        else -> "${seconds / 86_400}d ago"
    }
}

private fun historyRangeTitle(range: String): String = when (range.lowercase()) {
    "1m" -> "Last month"
    "3m" -> "Last 3 months"
    "1y" -> "Last year"
    else -> range.uppercase()
}

private fun historyAxisDate(value: String): String = runCatching {
    LocalDate.parse(value.take(10)).format(DateTimeFormatter.ofPattern("MMM d"))
}.getOrDefault(value.take(10))

private fun investmentDateTitle(value: String): String = runCatching {
    val date = LocalDate.parse(value)
    when (date) {
        LocalDate.now() -> "TODAY"
        LocalDate.now().minusDays(1) -> "YESTERDAY"
        else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")).uppercase()
    }
}.getOrDefault(value.uppercase())

internal fun formatInvestmentQuantity(value: String): String = runCatching {
    val amount = BigDecimal(value).stripTrailingZeros()
    if (amount.scale() <= 8) amount.toPlainString() else amount.setScale(8, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}.getOrDefault(value)
