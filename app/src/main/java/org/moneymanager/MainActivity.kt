package org.moneymanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.atan2
import org.moneymanager.data.ApiClient
import org.moneymanager.data.TokenStore
import org.moneymanager.model.Transaction
import org.moneymanager.model.expenseCategories
import org.moneymanager.notifications.ACTION_TRACK_PURCHASE
import org.moneymanager.notifications.PurchaseNotificationManager
import org.moneymanager.signals.FakePurchaseSignalSource

class MainActivity : ComponentActivity() {
    private lateinit var purchaseNotificationManager: PurchaseNotificationManager
    private val fakePurchaseSignalSource = FakePurchaseSignalSource()
    private var pendingTrackPurchase by mutableStateOf(false)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenStore = TokenStore(this)
        val apiClient = ApiClient(BuildConfig.API_BASE_URL)
        purchaseNotificationManager = PurchaseNotificationManager(this).also {
            it.createNotificationChannel()
        }
        fakePurchaseSignalSource.start {
            purchaseNotificationManager.showPurchaseDetectedNotification()
        }
        requestNotificationPermissionIfNeeded()
        pendingTrackPurchase = isTrackPurchaseIntent(intent)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                ) {
                    val viewModel: MoneyManagerViewModel = viewModel(
                        factory = MoneyManagerViewModelFactory(apiClient, tokenStore),
                    )
                    val state by viewModel.state.collectAsState()

                    LaunchedEffect(state.token, pendingTrackPurchase) {
                        if (state.token != null && pendingTrackPurchase) {
                            viewModel.openPhysicalPurchaseForm()
                            pendingTrackPurchase = false
                        }
                    }

                    if (state.token == null) {
                        AuthScreen(state, viewModel)
                    } else {
                        DashboardScreen(
                            state = state,
                            viewModel = viewModel,
                            onSimulatePurchaseSignal = fakePurchaseSignalSource::simulatePurchaseSignal,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isTrackPurchaseIntent(intent)) {
            pendingTrackPurchase = true
        }
    }

    override fun onDestroy() {
        fakePurchaseSignalSource.stop()
        super.onDestroy()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun isTrackPurchaseIntent(intent: Intent?): Boolean =
        intent?.action == ACTION_TRACK_PURCHASE
}

@Composable
private fun AuthScreen(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (state.isRegisterMode) "Create Account" else "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::updateEmail,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::updatePassword,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
        )
        ErrorText(state.error)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = viewModel::submitAuth,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isRegisterMode) "Register" else "Login")
        }
        TextButton(onClick = viewModel::toggleAuthMode, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.isRegisterMode) "Already have an account? Login" else "Need an account? Register")
        }
    }
}

@Composable
private fun DashboardScreen(
    state: MoneyManagerUiState,
    viewModel: MoneyManagerViewModel,
    onSimulatePurchaseSignal: () -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openNewTransactionForm) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { DashboardHeader(state, viewModel) }
            item { SimulateWalletSignalButton(onSimulatePurchaseSignal) }
            item { SpendingPieCard(state, viewModel) }
            item { TransactionsHeading(state) }
            if (state.error != null) {
                item { ErrorText(state.error) }
            }
            if (state.dayBuckets.isEmpty()) {
                item { EmptyTransactionsCard(state.selectedExpenseCategory) }
            } else {
                state.dayBuckets.forEach { bucket ->
                    item(key = bucket.date.toString()) {
                        DayBucketCard(bucket, viewModel)
                    }
                }
            }
        }
    }

    if (state.isTransactionFormOpen) {
        TransactionFormDialog(state, viewModel)
    }
}

@Composable
private fun SimulateWalletSignalButton(onSimulatePurchaseSignal: () -> Unit) {
    OutlinedButton(onClick = onSimulatePurchaseSignal, modifier = Modifier.fillMaxWidth()) {
        Text("Simulate wallet signal")
    }
}

@Composable
private fun DashboardHeader(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Money Manager", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            TextButton(onClick = viewModel::logout) { Text("Logout") }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = viewModel::previousMonth, enabled = !state.isLoading) {
                Text("Previous")
            }
            Text(formatMonth(state.month), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = viewModel::nextMonth, enabled = !state.isLoading && state.canGoNextMonth) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun SpendingPieCard(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Spending by Category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (state.expenseCategoryTotals.isEmpty()) {
                Text("No expenses for this month yet.")
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SpendingPieChart(
                        totals = state.expenseCategoryTotals,
                        selectedCategory = state.selectedExpenseCategory,
                        onCategorySelected = viewModel::selectExpenseCategory,
                        modifier = Modifier.size(180.dp),
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.expenseCategoryTotals.forEach { total ->
                            LegendRow(
                                category = total.category,
                                amount = total.amount,
                                isSelected = state.selectedExpenseCategory == total.category,
                            )
                        }
                    }
                }
            }
            if (state.selectedExpenseCategory != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Filtering: ${categoryEmoji(state.selectedExpenseCategory)} ${categoryTitle(state.selectedExpenseCategory)}")
                    TextButton(onClick = viewModel::clearSelectedExpenseCategory) {
                        Text("Clear filter")
                    }
                }
            }
            Text(
                "Monthly balance: ${state.summary?.balance ?: "0.00"} EUR",
                style = MaterialTheme.typography.bodyMedium,
                color = amountColor(state.summary?.balance?.toMoney() ?: BigDecimal.ZERO),
            )
        }
    }
}

@Composable
private fun TransactionsHeading(state: MoneyManagerUiState) {
    val selectedCategory = state.selectedExpenseCategory
    Text(
        text = if (selectedCategory == null) {
            "Transactions"
        } else {
            "Transactions: ${categoryEmoji(selectedCategory)} ${categoryTitle(selectedCategory)}"
        },
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun TransactionFormDialog(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::closeTransactionForm,
        title = {
            Text(if (state.editingId == null) "Add Transaction" else "Edit Transaction")
        },
        text = {
            TransactionFormFields(state, viewModel)
        },
        confirmButton = {
            Button(onClick = viewModel::saveTransaction, enabled = !state.isLoading) {
                Text(if (state.editingId == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::closeTransactionForm) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun TransactionFormFields(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.formType == "expense",
                onClick = { viewModel.updateFormType("expense") },
                label = { Text("Expense") },
            )
            FilterChip(
                selected = state.formType == "income",
                onClick = { viewModel.updateFormType("income") },
                label = { Text("Income") },
            )
        }
        CategoryChips(
            categories = categoriesForForm(state.formType),
            selected = state.formCategory,
            onSelected = { category -> if (category != null) viewModel.updateFormCategory(category) },
        )
        OutlinedTextField(
            value = state.formAmount,
            onValueChange = viewModel::updateFormAmount,
            label = { Text("Amount (EUR)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        if (state.editingId != null) {
            OutlinedTextField(
                value = state.formOccurredAt,
                onValueChange = viewModel::updateFormOccurredAt,
                label = { Text("Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
    }
}

@Composable
private fun CategoryChips(categories: List<String>, selected: String?, onSelected: (String?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { category ->
                    FilterChip(
                        selected = selected == category,
                        onClick = { onSelected(if (selected == category) null else category) },
                        label = { Text("${categoryEmoji(category)} ${categoryTitle(category)}") },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SpendingPieChart(
    totals: List<CategoryTotal>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = totals.fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }
    Canvas(
        modifier = modifier.pointerInput(totals) {
            detectTapGestures { offset ->
                findPieCategoryForTap(offset, size.width.toFloat(), size.height.toFloat(), totals)
                    ?.let(onCategorySelected)
            }
        },
    ) {
        var startAngle = -90f
        totals.forEach { item ->
            val sweep = item.amount.divide(total, 4, java.math.RoundingMode.HALF_UP).toFloat() * 360f
            val isDimmed = selectedCategory != null && selectedCategory != item.category
            drawArc(
                color = if (isDimmed) categoryColor(item.category).copy(alpha = 0.35f) else categoryColor(item.category),
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun LegendRow(category: String, amount: BigDecimal, isSelected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(categoryColor(category)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${categoryEmoji(category)} ${categoryTitle(category)} ${amount.formatMoney()} EUR",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun EmptyTransactionsCard(selectedExpenseCategory: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (selectedExpenseCategory == null) {
                "No transactions for this month yet."
            } else {
                "No ${categoryTitle(selectedExpenseCategory)} expenses for this month."
            },
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun DayBucketCard(bucket: DayBucket, viewModel: MoneyManagerViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = bucket.date.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = bucket.balanceChange.signedMoney(),
                    color = amountColor(bucket.balanceChange),
                    fontWeight = FontWeight.Bold,
                )
            }
            bucket.transactions.forEach { transaction ->
                TransactionRow(transaction, viewModel)
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction, viewModel: MoneyManagerViewModel) {
    var isMenuOpen by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryIcon(transaction.category)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(categoryTitle(transaction.category), fontWeight = FontWeight.SemiBold)
                Text(transaction.occurredAt.dateOnly(), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = transaction.signedAmount(),
                color = if (transaction.type == "income") incomeColor else expenseColor,
                fontWeight = FontWeight.Bold,
            )
            Box {
                TextButton(onClick = { isMenuOpen = true }) {
                    Text("...")
                }
                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            isMenuOpen = false
                            viewModel.editTransaction(transaction)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            isMenuOpen = false
                            viewModel.deleteTransaction(transaction.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryIcon(category: String) {
    Text(
        text = categoryEmoji(category),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.width(36.dp),
    )
}

@Composable
private fun ErrorText(error: String?) {
    if (error != null) {
        Spacer(Modifier.height(8.dp))
        Text(text = error, color = MaterialTheme.colorScheme.error)
    }
}

private fun categoriesForForm(type: String): List<String> =
    when (type) {
        "income" -> org.moneymanager.model.incomeCategories
        else -> expenseCategories
    }

private val expenseColor = Color(0xFFC62828)
private val incomeColor = Color(0xFF2E7D32)

private fun categoryColor(category: String): Color =
    when (category) {
        "food" -> Color(0xFFEF5350)
        "transport" -> Color(0xFF42A5F5)
        "housing" -> Color(0xFFAB47BC)
        "utilities" -> Color(0xFFFFA726)
        "health" -> Color(0xFF26A69A)
        "entertainment" -> Color(0xFFFF7043)
        "shopping" -> Color(0xFF7E57C2)
        "travel" -> Color(0xFF29B6F6)
        "education" -> Color(0xFF66BB6A)
        else -> Color(0xFF78909C)
    }

private fun amountColor(amount: BigDecimal): Color =
    when {
        amount > BigDecimal.ZERO -> incomeColor
        amount < BigDecimal.ZERO -> expenseColor
        else -> Color.Unspecified
    }

private fun Transaction.signedAmount(): String {
    val prefix = if (type == "income") "+" else "-"
    return "$prefix${amount.toMoney().formatMoney()} $currency"
}

private fun BigDecimal.signedMoney(): String {
    val sign = if (this >= BigDecimal.ZERO) "+" else "-"
    return "$sign${abs().formatMoney()} EUR"
}

private fun BigDecimal.formatMoney(): String = setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()

private fun String.toMoney(): BigDecimal = runCatching { BigDecimal(this) }.getOrDefault(BigDecimal.ZERO)

private fun String.dateOnly(): String = take(10)

private fun findPieCategoryForTap(
    offset: Offset,
    width: Float,
    height: Float,
    totals: List<CategoryTotal>,
): String? {
    if (totals.isEmpty()) return null

    val centerX = width / 2f
    val centerY = height / 2f
    val dx = offset.x - centerX
    val dy = offset.y - centerY
    val radius = minOf(width, height) / 2f
    if ((dx * dx) + (dy * dy) > radius * radius) return null

    val angle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 90.0 + 360.0) % 360.0
    val total = totals.fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }
    var currentAngle = 0.0
    totals.forEach { item ->
        val sweep = item.amount.divide(total, 4, java.math.RoundingMode.HALF_UP).toDouble() * 360.0
        if (angle >= currentAngle && angle < currentAngle + sweep) {
            return item.category
        }
        currentAngle += sweep
    }
    return totals.lastOrNull()?.category
}

private fun categoryTitle(category: String): String =
    category.replaceFirstChar { it.uppercase() }

private fun categoryEmoji(category: String): String =
    when (category) {
        "food" -> "🍔"
        "transport" -> "🚗"
        "housing" -> "🏠"
        "utilities" -> "💡"
        "health" -> "🏥"
        "entertainment" -> "🎬"
        "shopping" -> "🛍️"
        "travel" -> "✈️"
        "education" -> "📚"
        "salary" -> "💼"
        "freelance" -> "💻"
        "gift" -> "🎁"
        "investment" -> "📈"
        "refund" -> "↩️"
        else -> "•"
    }

private fun formatMonth(month: String): String =
    runCatching {
        LocalDate.parse("$month-01").format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }.getOrDefault(month)
