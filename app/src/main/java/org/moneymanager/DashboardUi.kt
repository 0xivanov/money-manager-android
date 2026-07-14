package org.moneymanager

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.delay
import org.moneymanager.model.Category
import org.moneymanager.model.Transaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardScreen(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    PullToRefreshBox(
        isRefreshing = state.isMonthRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { AppHeader("Overview", state, viewModel, onAdd = viewModel::openNewTransactionForm) }

            if (state.isMonthLoading && !state.hasMonthContent) {
                item { LoadingCard("Loading ${formatMonth(state.month)}…", Modifier.padding(horizontal = 16.dp)) }
            } else if (state.monthLoadPhase == MonthLoadPhase.Failure && !state.hasMonthContent) {
                item {
                    FailureCard(
                        message = state.monthError ?: "Could not load this month",
                        onRetry = viewModel::refresh,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            } else if (state.hasMonthContent) {
                if (state.monthLoadPhase == MonthLoadPhase.Failure) {
                    item {
                        InlineRetry(
                            message = state.monthError ?: "Refresh failed. Showing saved data.",
                            onRetry = viewModel::refresh,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
                item { BalanceCard(state, Modifier.padding(horizontal = 16.dp)) }
                item { SummaryTiles(state, Modifier.padding(horizontal = 16.dp)) }
                item { SpendingCard(state, viewModel, Modifier.padding(horizontal = 16.dp)) }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionTitle("Recent transactions", Modifier.weight(1f))
                        TextButton(onClick = viewModel::showAllTransactions) {
                            Text("View all")
                        }
                    }
                }
                state.selectedExpenseCategory?.let { category ->
                    item {
                        ActiveCategoryFilter(
                            category = category,
                            onClear = viewModel::clearSelectedExpenseCategory,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
                if (state.dashboardDayBuckets.isEmpty()) {
                    item {
                        EmptyCard(
                            title = if (state.selectedExpenseCategory == null) "No transactions yet" else "No matching transactions",
                            message = if (state.selectedExpenseCategory == null) {
                                "Add your first transaction for ${formatMonth(state.month)}."
                            } else {
                                "Clear the category filter to see all activity."
                            },
                            actionLabel = if (state.selectedExpenseCategory == null) "Add transaction" else "Clear filter",
                            onAction = if (state.selectedExpenseCategory == null) {
                                viewModel::openNewTransactionForm
                            } else {
                                viewModel::clearSelectedExpenseCategory
                            },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                } else {
                    state.dashboardDayBuckets.take(3).forEach { bucket ->
                        item(key = "dash-${bucket.date}") {
                            DayCard(
                                bucket = bucket.copy(transactions = bucket.transactions.take(2)),
                                viewModel = viewModel,
                                isTransactionMutating = state.isTransactionMutating,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AppHeader(
    title: String,
    state: MoneyManagerUiState,
    viewModel: MoneyManagerViewModel,
    onAdd: (() -> Unit)? = null,
) {
    Surface(color = appSurface, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                        .semantics { heading() },
                )
                if (onAdd != null) {
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Filled.Add, contentDescription = "Add transaction", tint = financeGreen)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButtonText(
                    icon = Icons.Filled.ChevronLeft,
                    contentDescription = "Previous month",
                    enabled = !state.isMonthLoading,
                    onClick = viewModel::previousMonth,
                )
                Text(
                    formatMonth(state.month),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                IconButtonText(
                    icon = Icons.Filled.ChevronRight,
                    contentDescription = "Next month",
                    enabled = state.canGoNextMonth && !state.isMonthLoading,
                    onClick = viewModel::nextMonth,
                )
            }
            if (state.isMonthRefreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun SimpleHeader(
    title: String,
    trailingIcon: ImageVector? = null,
    trailingContentDescription: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    trailingEnabled: Boolean = true,
) {
    Surface(color = appSurface, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            if (trailingIcon != null && onTrailingClick != null) {
                IconButton(onClick = onTrailingClick, enabled = trailingEnabled) {
                    Icon(trailingIcon, contentDescription = trailingContentDescription, tint = mutedText)
                }
            }
        }
    }
}

@Composable
internal fun BalanceCard(state: MoneyManagerUiState, modifier: Modifier = Modifier) {
    val summary = state.summary ?: return
    val balance = BigDecimal(summary.balance)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        color = financeGreen,
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Monthly balance", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f))
                Text(
                    balance.money(summary.currency),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                if (balance >= BigDecimal.ZERO) "On track" else "Overdrawn",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun SummaryTiles(state: MoneyManagerUiState, modifier: Modifier = Modifier) {
    val summary = state.summary ?: return
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryTile("Income", BigDecimal(summary.income).money(summary.currency), incomeColor, Modifier.weight(1f))
        SummaryTile("Spent", BigDecimal(summary.expense).money(summary.currency), expenseColor, Modifier.weight(1f))
        SummaryTile("Entries", summary.transactionCount.toString(), nearBlack, Modifier.weight(0.72f))
    }
}

@Composable
internal fun SummaryTile(label: String, value: String, color: Color, modifier: Modifier) {
    AppCard(modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = mutedText, style = MaterialTheme.typography.labelMedium)
            Text(
                value,
                color = color,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
internal fun SpendingCard(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Spending by category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            if (state.expenseCategoryTotals.isEmpty()) {
                Text("No expenses for this month.", color = mutedText)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    DonutChart(
                        totals = state.expenseCategoryTotals,
                        currency = state.currentCurrency,
                        selectedCategory = state.selectedExpenseCategory,
                        onCategorySelected = viewModel::selectExpenseCategory,
                        modifier = Modifier.size(108.dp),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val total = state.expenseCategoryTotals.sumOfMoney()
                        state.expenseCategoryTotals.take(4).forEach { item ->
                            LegendRow(
                                item = item,
                                total = total,
                                selected = state.selectedExpenseCategory == item.category,
                                onClick = { viewModel.selectExpenseCategory(item.category) },
                            )
                        }
                        if (state.expenseCategoryTotals.size > 4) {
                            Text(
                                "+${state.expenseCategoryTotals.size - 4} more categories",
                                color = mutedText,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ActiveCategoryFilter(category: String, onClear: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = softGreenCard,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Showing ${categoryTitle(category)}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = onClear, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Clear category filter")
            }
        }
    }
}
