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
internal fun TransactionsScreen(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
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
            item { AppHeader("Transactions", state, viewModel, onAdd = viewModel::openNewTransactionForm) }
            if (state.isMonthLoading && !state.hasMonthContent) {
                item { LoadingCard("Loading transactions…", Modifier.padding(horizontal = 16.dp)) }
            } else if (state.monthLoadPhase == MonthLoadPhase.Failure && !state.hasMonthContent) {
                item {
                    FailureCard(
                        message = state.monthError ?: "Could not load transactions",
                        onRetry = viewModel::refresh,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            } else if (state.hasMonthContent) {
                item { TransactionSearch(state, viewModel, Modifier.padding(horizontal = 16.dp)) }
                item { TypeFilters(state, viewModel, Modifier.padding(horizontal = 16.dp)) }
                item { CategoryFilter(state, viewModel, Modifier.padding(horizontal = 16.dp)) }
                if (state.hasActiveTransactionFilters) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Filters are active", color = mutedText, modifier = Modifier.weight(1f))
                            TextButton(onClick = viewModel::clearTransactionFilters) { Text("Reset") }
                        }
                    }
                }
                if (state.monthLoadPhase == MonthLoadPhase.Failure) {
                    item {
                        InlineRetry(
                            message = state.monthError ?: "Refresh failed. Showing saved data.",
                            onRetry = viewModel::refresh,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
                if (state.dayBuckets.isEmpty()) {
                    item {
                        EmptyCard(
                            title = if (state.hasActiveTransactionFilters) "No matches" else "No transactions yet",
                            message = if (state.hasActiveTransactionFilters) {
                                "Try a different search or clear the filters."
                            } else {
                                "Add your first transaction for ${formatMonth(state.month)}."
                            },
                            actionLabel = if (state.hasActiveTransactionFilters) "Reset filters" else "Add transaction",
                            onAction = if (state.hasActiveTransactionFilters) {
                                viewModel::clearTransactionFilters
                            } else {
                                viewModel::openNewTransactionForm
                            },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                } else {
                    state.dayBuckets.forEach { bucket ->
                        item(key = "tx-${bucket.date}") {
                            DayCard(
                                bucket = bucket,
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
internal fun TransactionSearch(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = state.searchQuery,
        onValueChange = viewModel::updateSearchQuery,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search description, category, or amount") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = if (state.searchQuery.isBlank()) null else ({
            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                Icon(Icons.Filled.Close, contentDescription = "Clear search")
            }
        }),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        singleLine = true,
        shape = inputShape,
    )
}

@Composable
internal fun TypeFilters(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SegmentButton("All", state.filterType == null, { viewModel.updateFilterType(null) }, Modifier.weight(1f))
        SegmentButton("Expenses", state.filterType == "expense", { viewModel.updateFilterType("expense") }, Modifier.weight(1f))
        SegmentButton("Income", state.filterType == "income", { viewModel.updateFilterType("income") }, Modifier.weight(1f))
    }
}

@Composable
internal fun CategoryFilter(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = inputShape,
        ) {
            Text(
                state.filterCategory?.let { "Category: ${categoryTitle(it)}" } ?: "All categories",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
            )
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.92f),
        ) {
            DropdownMenuItem(
                text = { Text("All categories") },
                onClick = {
                    viewModel.updateFilterCategory(null)
                    expanded = false
                },
                leadingIcon = if (state.filterCategory == null) ({ Icon(Icons.Filled.CheckCircle, contentDescription = null) }) else null,
            )
            state.availableFilterCategories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(categoryTitle(category)) },
                    onClick = {
                        viewModel.updateFilterCategory(category)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(categoryIcon(category), contentDescription = null, tint = categoryColor(category))
                    },
                    trailingIcon = if (state.filterCategory == category) ({
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Selected")
                    }) else null,
                )
            }
        }
    }
}

@Composable
internal fun DayCard(
    bucket: DayBucket,
    viewModel: MoneyManagerViewModel,
    isTransactionMutating: Boolean,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(softGreenSurface)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(dayTitle(bucket.date), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (bucket.transactions.size > 1) {
                    Text(
                        bucket.balanceChange.signedMoney(bucket.transactions.first().currency),
                        color = amountColor(bucket.balanceChange),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            bucket.transactions.forEachIndexed { index, transaction ->
                TransactionRow(transaction, viewModel, isTransactionMutating)
                if (index != bucket.transactions.lastIndex) HorizontalDivider(color = softDivider)
            }
        }
    }
}

@Composable
internal fun TransactionRow(
    transaction: Transaction,
    viewModel: MoneyManagerViewModel,
    isTransactionMutating: Boolean,
) {
    var showDeleteConfirmation by remember(transaction.id) { mutableStateOf(false) }
    val accessibleDescription = buildString {
        append(if (transaction.type == "income") "Income" else "Expense")
        append(", ${categoryTitle(transaction.category)}, ${transaction.signedAmount()}")
        if (transaction.description.isNotBlank()) append(", ${transaction.description}")
        append(", tap to edit")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Edit transaction") { viewModel.editTransaction(transaction) }
            .semantics { contentDescription = accessibleDescription }
            .padding(start = 14.dp, top = 11.dp, bottom = 11.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(transaction.category)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                transaction.description.ifBlank { categoryTitle(transaction.category) },
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (transaction.description.isBlank()) transaction.occurredAt.toDisplayDate() else categoryTitle(transaction.category),
                color = mutedText,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            transaction.signedAmount(),
            color = if (transaction.type == "income") incomeColor else expenseColor,
            fontWeight = FontWeight.Bold,
        )
        IconButton(
            onClick = { showDeleteConfirmation = true },
            enabled = !isTransactionMutating,
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete ${transaction.description.ifBlank { categoryTitle(transaction.category) }}",
                tint = mutedText,
            )
        }
    }

    if (showDeleteConfirmation) {
        DestructiveConfirmationDialog(
            title = "Delete transaction?",
            message = "${transaction.description.ifBlank { categoryTitle(transaction.category) }} · ${transaction.signedAmount()}",
            confirmLabel = "Delete",
            onDismiss = { showDeleteConfirmation = false },
            onConfirm = {
                showDeleteConfirmation = false
                viewModel.deleteTransaction(transaction.id)
            },
        )
    }
}
