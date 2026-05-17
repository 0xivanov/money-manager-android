package org.moneymanager

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.roundToInt
import org.moneymanager.model.Category
import org.moneymanager.model.Transaction

@Composable
fun MoneyManagerRoot(
    state: MoneyManagerUiState,
    viewModel: MoneyManagerViewModel,
    pendingTrackPurchase: Boolean,
    onTrackPurchaseHandled: () -> Unit,
    onExportCsv: (fileName: String, csv: String) -> Unit,
    onSimulatePurchaseSignal: () -> Unit,
) {
    MoneyManagerTheme {
        LaunchedEffect(state.token, pendingTrackPurchase) {
            if (state.token != null && pendingTrackPurchase) {
                viewModel.openPhysicalPurchaseForm()
                onTrackPurchaseHandled()
            }
        }
        LaunchedEffect(state.exportCsvContent, state.exportFileName) {
            val csv = state.exportCsvContent
            val fileName = state.exportFileName
            if (csv != null && fileName != null) {
                onExportCsv(fileName, csv)
                viewModel.clearExportResult()
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            color = appBackground,
        ) {
            if (state.token == null) {
                AuthScreen(state = state, viewModel = viewModel)
            } else {
                MoneyApp(
                    state = state,
                    viewModel = viewModel,
                    onSimulatePurchaseSignal = onSimulatePurchaseSignal,
                )
            }
        }
    }
}

@Composable
private fun AuthScreen(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Money Manager",
                    style = MaterialTheme.typography.headlineLarge,
                    color = nearBlack,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Track spending, spot patterns, stay in control.",
                    color = mutedText,
                    textAlign = TextAlign.Center,
                )
            }
        }
        item {
            AppCard {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("May 2026", color = mutedText)
                    Text("€3,921.50", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("+€3,921.50", color = incomeColor, fontWeight = FontWeight.SemiBold)
                    HorizontalDivider(color = softDivider)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryIcon("food")
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Food", fontWeight = FontWeight.SemiBold)
                            Text("Top category", style = MaterialTheme.typography.bodySmall, color = mutedText)
                        }
                        Text("-€127.70", color = expenseColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                AppTextField(
                    value = state.email,
                    onValueChange = viewModel::updateEmail,
                    label = "Email",
                    placeholder = "you@example.com",
                    keyboardType = KeyboardType.Email,
                )
                AppTextField(
                    value = state.password,
                    onValueChange = viewModel::updatePassword,
                    label = "Password",
                    placeholder = "........",
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardType = KeyboardType.Password,
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                tint = mutedText,
                            )
                        }
                    },
                )
                ErrorText(state.error)
                Button(
                    onClick = viewModel::submitAuth,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = buttonShape,
                    contentPadding = PaddingValues(vertical = 15.dp),
                ) {
                    Text(if (state.isRegisterMode) "Create account" else "Log in", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = viewModel::toggleAuthMode,
                    modifier = Modifier.fillMaxWidth(),
                    shape = buttonShape,
                    border = BorderStroke(1.dp, softDivider),
                    contentPadding = PaddingValues(vertical = 15.dp),
                ) {
                    Text(
                        text = if (state.isRegisterMode) {
                            "Already have an account? Log in"
                        } else {
                            "Create account"
                        },
                        color = financeGreen,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun MoneyApp(
    state: MoneyManagerUiState,
    viewModel: MoneyManagerViewModel,
    onSimulatePurchaseSignal: () -> Unit,
) {
    Scaffold(
        containerColor = appBackground,
        floatingActionButton = {
            if (state.selectedTab != AppTab.Profile && !state.isTransactionFormOpen) {
                FloatingActionButton(
                    onClick = viewModel::openNewTransactionForm,
                    shape = CircleShape,
                    containerColor = financeGreen,
                    contentColor = Color.White,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add transaction")
                }
            }
        },
        bottomBar = {
            if (!state.isTransactionFormOpen) {
                BottomNav(state = state, viewModel = viewModel)
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                state.isTransactionFormOpen -> TransactionEditor(state, viewModel)
                state.selectedTab == AppTab.Dashboard -> DashboardScreen(state, viewModel)
                state.selectedTab == AppTab.Transactions -> TransactionsScreen(state, viewModel)
                state.selectedTab == AppTab.Profile -> ProfileScreen(
                    state = state,
                    viewModel = viewModel,
                    onSimulatePurchaseSignal = onSimulatePurchaseSignal,
                )
            }
        }
    }

    if (state.isCategoryPickerOpen) {
        CategoryPickerSheet(state, viewModel)
    }
    if (state.isExportDialogOpen) {
        ExportDialog(state, viewModel)
    }
}

@Composable
private fun BottomNav(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    NavigationBar(containerColor = appSurface, tonalElevation = 0.dp) {
        AppTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = state.selectedTab == tab,
                onClick = { viewModel.selectTab(tab) },
                icon = { Icon(tabIcon(tab), contentDescription = null) },
                label = { Text(tabLabel(tab)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = financeGreen,
                    selectedTextColor = financeGreen,
                    indicatorColor = softGreenCard,
                    unselectedIconColor = mutedText,
                    unselectedTextColor = mutedText,
                ),
            )
        }
    }
}

@Composable
private fun DashboardScreen(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { AppHeader("Money Manager", state, viewModel) }
        item { BalanceCard(state, Modifier.padding(horizontal = 16.dp)) }
        item { SummaryTiles(state, Modifier.padding(horizontal = 16.dp)) }
        item { SpendingCard(state, viewModel, Modifier.padding(horizontal = 16.dp)) }
        item { SectionTitle("Recent transactions", Modifier.padding(horizontal = 16.dp)) }
        if (state.error != null) item { ErrorText(state.error, Modifier.padding(horizontal = 16.dp)) }
        if (state.dayBuckets.isEmpty()) {
            item { EmptyCard("No transactions for this month yet.", Modifier.padding(horizontal = 16.dp)) }
        } else {
            state.dayBuckets.take(4).forEach { bucket ->
                item(key = "dash-${bucket.date}") {
                    DayCard(bucket.copy(transactions = bucket.transactions.take(3)), viewModel, Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun TransactionsScreen(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { AppHeader("Transactions", state, viewModel) }
        item { TypeFilters(state, viewModel, Modifier.padding(horizontal = 16.dp)) }
        if (state.error != null) item { ErrorText(state.error, Modifier.padding(horizontal = 16.dp)) }
        if (state.dayBuckets.isEmpty()) {
            item { EmptyCard("No matching transactions.", Modifier.padding(horizontal = 16.dp)) }
        } else {
            state.dayBuckets.forEach { bucket ->
                item(key = "tx-${bucket.date}") {
                    DayCard(bucket, viewModel, Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    state: MoneyManagerUiState,
    viewModel: MoneyManagerViewModel,
    onSimulatePurchaseSignal: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { SimpleHeader("Profile") }
        item {
            AppCard(Modifier.padding(horizontal = 16.dp)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Signed in as", color = mutedText)
                    Text(state.signedInEmail.ifBlank { "user@example.com" }, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            ProfileGroup(title = "Connection", modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("API Base URL", color = mutedText)
                Text(
                    BuildConfig.API_BASE_URL,
                    modifier = Modifier
                        .background(softGreenSurface, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Medium,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = incomeColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("Connected", color = incomeColor)
                }
            }
        }
        item {
            ProfileGroup(title = "Data", modifier = Modifier.padding(horizontal = 16.dp)) {
                ProfileAction(
                    icon = Icons.Filled.FileDownload,
                    title = "Export transactions",
                    subtitle = "Download as CSV",
                    onClick = viewModel::openExportDialog,
                )
            }
        }
        item {
            ProfileGroup(title = "Developer", modifier = Modifier.padding(horizontal = 16.dp)) {
                ProfileAction(
                    icon = Icons.Filled.FlashOn,
                    title = "Simulate purchase signal",
                    subtitle = null,
                    onClick = onSimulatePurchaseSignal,
                )
            }
        }
        item {
            OutlinedButton(
                onClick = viewModel::logout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = buttonShape,
                border = BorderStroke(1.dp, softDivider),
                contentPadding = PaddingValues(vertical = 15.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = expenseColor),
            ) {
                Text("Log out", fontWeight = FontWeight.SemiBold)
            }
        }
        if (state.error != null) item { ErrorText(state.error, Modifier.padding(horizontal = 16.dp)) }
    }
}

@Composable
private fun TransactionEditor(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    Scaffold(
        containerColor = appBackground,
        bottomBar = {
            Surface(color = appSurface, shadowElevation = 0.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = viewModel::closeTransactionForm,
                        modifier = Modifier.weight(1f),
                        shape = buttonShape,
                        border = BorderStroke(1.dp, softDivider),
                        contentPadding = PaddingValues(vertical = 15.dp),
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = viewModel::saveTransaction,
                        enabled = !state.isLoading,
                        modifier = Modifier.weight(1f),
                        shape = buttonShape,
                        contentPadding = PaddingValues(vertical = 15.dp),
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SimpleHeader(
                title = transactionEditorTitle(state),
                trailingIcon = Icons.Filled.Close,
                trailingContentDescription = "Close",
                onTrailingClick = viewModel::closeTransactionForm,
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SegmentButton(
                        text = "Expense",
                        selected = state.formType == "expense",
                        onClick = { viewModel.updateFormType("expense") },
                        modifier = Modifier.weight(1f),
                    )
                    SegmentButton(
                        text = "Income",
                        selected = state.formType == "income",
                        onClick = { viewModel.updateFormType("income") },
                        modifier = Modifier.weight(1f),
                    )
                }
                AppTextField(
                    value = state.formAmount,
                    onValueChange = viewModel::updateFormAmount,
                    label = "Amount",
                    placeholder = "0.00",
                    prefix = "€",
                    keyboardType = KeyboardType.Decimal,
                    large = true,
                )
                CategoryButton(state.formCategory, viewModel::openCategoryPicker)
                AppTextField(
                    value = state.formDescription,
                    onValueChange = viewModel::updateFormDescription,
                    label = "Description (optional)",
                    placeholder = "e.g., Groceries at supermarket",
                )
                AppTextField(
                    value = state.formOccurredAt.toDisplayDate(),
                    onValueChange = { viewModel.updateFormOccurredAt(it.displayDateToIso()) },
                    label = "Date",
                    placeholder = "15.05.2026",
                    keyboardType = KeyboardType.Number,
                )
                ErrorText(state.error)
            }
        }
    }
}

@Composable
private fun AppHeader(title: String, state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    Surface(color = appSurface, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButtonText(
                    icon = Icons.Filled.ChevronLeft,
                    contentDescription = "Previous month",
                    enabled = !state.isLoading,
                    onClick = viewModel::previousMonth,
                )
                Text(formatMonth(state.month), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButtonText(
                    icon = Icons.Filled.ChevronRight,
                    contentDescription = "Next month",
                    enabled = state.canGoNextMonth && !state.isLoading,
                    onClick = viewModel::nextMonth,
                )
            }
        }
    }
}

@Composable
private fun SimpleHeader(
    title: String,
    trailingIcon: ImageVector? = null,
    trailingContentDescription: String? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    Surface(color = appSurface, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (trailingIcon != null && onTrailingClick != null) {
                IconButton(onClick = onTrailingClick) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = trailingContentDescription,
                        tint = mutedText,
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(state: MoneyManagerUiState, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Balance", color = mutedText)
            val balance = state.summary?.balance ?: "0.00"
            Text(balance.toMoneyAmount().money(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(balance.toMoneyAmount().signedMoney(), color = amountColor(balance.toMoneyAmount()), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SummaryTiles(state: MoneyManagerUiState, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryTile("Income", state.summary?.income ?: "0.00", incomeColor, Modifier.weight(1f))
        SummaryTile("Expenses", state.summary?.expense ?: "0.00", expenseColor, Modifier.weight(1f))
        SummaryTile("Count", "${state.summary?.transactionCount ?: 0}", nearBlack, Modifier.weight(1f), false)
    }
}

@Composable
private fun SummaryTile(label: String, value: String, color: Color, modifier: Modifier, money: Boolean = true) {
    AppCard(modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, color = mutedText, style = MaterialTheme.typography.bodySmall)
            Text(if (money) value.toMoneyAmount().money() else value, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SpendingCard(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Spending by category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (state.expenseCategoryTotals.isEmpty()) {
                Text("No expenses for this month yet.", color = mutedText)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    DonutChart(
                        totals = state.expenseCategoryTotals,
                        selectedCategory = state.selectedExpenseCategory,
                        onCategorySelected = viewModel::selectExpenseCategory,
                        modifier = Modifier.size(132.dp),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val total = state.expenseCategoryTotals.sumOfMoney()
                        state.expenseCategoryTotals.take(3).forEach { item ->
                            LegendRow(item, total, state.selectedExpenseCategory == item.category)
                        }
                    }
                }
                state.expenseCategoryTotals.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(categoryColor(item.category), CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Text(categoryTitle(item.category), color = mutedText, modifier = Modifier.weight(1f))
                        Text(item.amount.money(), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeFilters(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SegmentButton("All", state.filterType == null, { viewModel.updateFilterType(null) })
        SegmentButton("Expenses", state.filterType == "expense", { viewModel.updateFilterType("expense") })
        SegmentButton("Income", state.filterType == "income", { viewModel.updateFilterType("income") })
    }
}

@Composable
private fun DayCard(bucket: DayBucket, viewModel: MoneyManagerViewModel, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(softGreenSurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(dayTitle(bucket.date), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(bucket.balanceChange.signedMoney(), color = amountColor(bucket.balanceChange), fontWeight = FontWeight.SemiBold)
            }
            bucket.transactions.forEachIndexed { index, transaction ->
                TransactionRow(transaction, viewModel)
                if (index != bucket.transactions.lastIndex) HorizontalDivider(color = softDivider)
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction, viewModel: MoneyManagerViewModel) {
    var isDeleteRevealed by remember(transaction.id) { mutableStateOf(false) }
    var isDeleting by remember(transaction.id) { mutableStateOf(false) }
    var dragDistance by remember(transaction.id) { mutableStateOf(0f) }
    val density = LocalDensity.current
    val revealWidthPx = with(density) { 96.dp.toPx() }
    val swipeThresholdPx = with(density) { 48.dp.toPx() }
    val rowOffset by animateFloatAsState(
        targetValue = when {
            isDeleting -> -1_200f
            isDeleteRevealed -> -revealWidthPx
            else -> 0f
        },
        animationSpec = tween(durationMillis = 220),
        label = "transaction-delete-offset",
    )

    LaunchedEffect(isDeleting) {
        if (isDeleting) {
            delay(220)
            viewModel.deleteTransaction(transaction.id)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(transaction.id, isDeleteRevealed, isDeleting) {
                detectHorizontalDragGestures(
                    onDragStart = { dragDistance = 0f },
                    onHorizontalDrag = { _, dragAmount -> dragDistance += dragAmount },
                    onDragEnd = {
                        when {
                            dragDistance < -swipeThresholdPx && isDeleteRevealed -> isDeleting = true
                            dragDistance < -swipeThresholdPx -> isDeleteRevealed = true
                            dragDistance > swipeThresholdPx -> isDeleteRevealed = false
                        }
                        dragDistance = 0f
                    },
                    onDragCancel = { dragDistance = 0f },
                )
            },
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(expenseColor),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Row(
                modifier = Modifier
                    .width(96.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier
                .offset { IntOffset(rowOffset.roundToInt(), 0) }
                .fillMaxWidth()
                .background(appSurface)
                .clickable(enabled = !isDeleting) { viewModel.editTransaction(transaction) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryIcon(transaction.category)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(categoryTitle(transaction.category), fontWeight = FontWeight.SemiBold)
                Text(
                    transaction.description.ifBlank { transaction.occurredAt.toDisplayDate() },
                    color = mutedText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                transaction.signedAmount(),
                color = if (transaction.type == "income") incomeColor else expenseColor,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    ModalBottomSheet(
        onDismissRequest = viewModel::closeCategoryPicker,
        containerColor = appSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Choose category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = viewModel::closeCategoryPicker) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = mutedText)
                }
            }
            state.formCategoryOptions.forEach { category ->
                CategoryChoice(category, selected = state.formCategory == category.name, viewModel = viewModel)
            }
            HorizontalDivider(color = softDivider)
            AppTextField(
                value = state.newCategoryName,
                onValueChange = viewModel::updateNewCategoryName,
                label = "Custom category",
                placeholder = "Add custom category",
            )
            Button(
                onClick = viewModel::addCategory,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = buttonShape,
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add custom category")
            }
            ErrorText(state.error)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CategoryChoice(category: Category, selected: Boolean, viewModel: MoneyManagerViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = { viewModel.chooseFormCategory(category.name) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected) softGreenCard else softGreenSurface,
                contentColor = nearBlack,
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(category.name)
                Spacer(Modifier.width(12.dp))
                Text(categoryTitle(category.name), fontWeight = FontWeight.SemiBold)
            }
        }
                if (!category.isDefault && category.id != 0) {
            IconButton(onClick = { viewModel.deleteCategory(category) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = expenseColor)
            }
        }
    }
}

@Composable
private fun ExportDialog(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::closeExportDialog,
        containerColor = appSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Export CSV", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = viewModel::closeExportDialog) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = mutedText)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select a date range to export your transactions. The range is inclusive of both dates.", color = mutedText)
                AppTextField(
                    value = state.exportFrom.toDisplayDate(),
                    onValueChange = { viewModel.updateExportFrom(it.displayDateToIso()) },
                    label = "From date",
                    placeholder = "01.05.2026",
                    keyboardType = KeyboardType.Number,
                )
                AppTextField(
                    value = state.exportTo.toDisplayDate(),
                    onValueChange = { viewModel.updateExportTo(it.displayDateToIso()) },
                    label = "To date",
                    placeholder = "15.05.2026",
                    keyboardType = KeyboardType.Number,
                )
                ErrorText(state.error)
            }
        },
        confirmButton = {
            Button(onClick = viewModel::exportTransactions, enabled = !state.isLoading, shape = buttonShape) {
                Text("Export CSV")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = viewModel::closeExportDialog, shape = buttonShape) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    large: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold, color = nearBlack)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(placeholder, color = mutedText.copy(alpha = 0.65f)) },
            prefix = if (prefix == null) null else ({ Text(prefix, color = mutedText) }),
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = inputShape,
            textStyle = if (large) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.bodyLarge,
            trailingIcon = trailingIcon,
        )
    }
}

@Composable
private fun CategoryButton(category: String, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Category", fontWeight = FontWeight.SemiBold)
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = inputShape,
            border = BorderStroke(1.dp, softDivider),
            contentPadding = PaddingValues(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(category)
                Spacer(Modifier.width(12.dp))
                Text(categoryTitle(category), color = nearBlack, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
            }
        }
    }
}

@Composable
private fun SegmentButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color.Transparent,
            selectedBorderColor = Color.Transparent,
            borderWidth = 0.dp,
            selectedBorderWidth = 0.dp,
        ),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = financeGreen,
            selectedLabelColor = Color.White,
            containerColor = softGreenSurface,
            labelColor = nearBlack,
        ),
    )
}

@Composable
private fun IconButtonText(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(44.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) nearBlack else mutedText.copy(alpha = 0.35f),
        )
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun ProfileGroup(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    AppCard(modifier) {
        Column {
            Text(title, modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            HorizontalDivider(color = softDivider)
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
private fun ProfileAction(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.Transparent),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = appSurface),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(softGreenSurface, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = financeGreen, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(title, color = nearBlack, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) Text(subtitle, color = mutedText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CategoryIcon(category: String) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(categoryColor(category).copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = null,
            tint = categoryColor(category),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AppCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = appSurface),
        border = BorderStroke(1.dp, softDivider),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        content()
    }
}

@Composable
private fun EmptyCard(message: String, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        Column(
            Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CategoryIcon("other")
            Text("Nothing here yet", fontWeight = FontWeight.Bold)
            Text(message, color = mutedText, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ErrorText(error: String?, modifier: Modifier = Modifier) {
    if (error != null) {
        Text(error, color = expenseColor, style = MaterialTheme.typography.bodySmall, modifier = modifier)
    }
}

@Composable
private fun DonutChart(
    totals: List<CategoryTotal>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = totals.sumOfMoney()
    Canvas(
        modifier = modifier.pointerInput(totals) {
            detectTapGestures { offset ->
                findPieCategoryForTap(offset, size.width.toFloat(), size.height.toFloat(), totals)
                    ?.let(onCategorySelected)
            }
        },
    ) {
        val strokeWidth = size.minDimension * 0.17f
        var start = -90f
        drawArc(
            color = softGreenSurface,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        totals.forEach { item ->
            val sweep = item.amount.divide(total, 4, RoundingMode.HALF_UP).toFloat() * 360f
            val dimmed = selectedCategory != null && selectedCategory != item.category
            drawArc(
                color = categoryColor(item.category).copy(alpha = if (dimmed) 0.35f else 1f),
                startAngle = start,
                sweepAngle = sweep.coerceAtLeast(2f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            start += sweep
        }
    }
}

@Composable
private fun LegendRow(item: CategoryTotal, total: BigDecimal, selected: Boolean) {
    val percent = if (total > BigDecimal.ZERO) {
        item.amount.multiply(BigDecimal(100)).divide(total, 0, RoundingMode.HALF_UP).toPlainString()
    } else {
        "0"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(categoryColor(item.category), CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(categoryTitle(item.category), modifier = Modifier.weight(1f), fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        Text("$percent%", color = mutedText)
    }
}
