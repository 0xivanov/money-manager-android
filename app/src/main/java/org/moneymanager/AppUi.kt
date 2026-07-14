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

@Composable
fun MoneyManagerRoot(
    state: MoneyManagerUiState,
    viewModel: MoneyManagerViewModel,
    pendingTrackPurchase: Boolean,
    onTrackPurchaseHandled: () -> Unit,
    onExportCsv: (fileName: String, csv: String) -> Unit,
    notificationsEnabled: Boolean,
    onEnableNotifications: () -> Unit,
    onSimulatePurchaseSignal: () -> Unit,
) {
    val context = LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }
                .getOrNull()
                ?.let(viewModel::importRevolutCsv)
        }
    }
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
        LaunchedEffect(state.growth.investmentExportCsv, state.growth.investmentExportFileName) {
            val csv = state.growth.investmentExportCsv
            val fileName = state.growth.investmentExportFileName
            if (csv != null && fileName != null) {
                onExportCsv(fileName, csv)
                viewModel.clearInvestmentExport()
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
                    notificationsEnabled = notificationsEnabled,
                    onEnableNotifications = onEnableNotifications,
                    onSimulatePurchaseSignal = onSimulatePurchaseSignal,
                    onImportRevolut = { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain")) },
                )
            }
        }
    }
}

@Composable
private fun AuthScreen(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    val passwordFocus = remember { FocusRequester() }
    val confirmationFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackground),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(62.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = brandGreen,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Money Manager",
                    style = MaterialTheme.typography.headlineLarge,
                    color = nearBlack,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "A clear view of your money, without the busywork.",
                    color = mutedText,
                    textAlign = TextAlign.Center,
                )
            }
        }
        item { BenefitPanel() }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                AppTextField(
                    value = state.email,
                    onValueChange = viewModel::updateEmail,
                    label = "Email",
                    placeholder = "you@example.com",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        autoCorrectEnabled = false,
                    ),
                    keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
                )
                AppTextField(
                    value = state.password,
                    onValueChange = viewModel::updatePassword,
                    label = "Password",
                    placeholder = if (state.isRegisterMode) "At least 8 characters" else "Your password",
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (state.isRegisterMode) ImeAction.Next else ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { confirmationFocus.requestFocus() },
                        onDone = {
                            focusManager.clearFocus()
                            if (!state.isAuthLoading) viewModel.submitAuth()
                        },
                    ),
                    focusRequester = passwordFocus,
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
                if (state.isRegisterMode) {
                    AppTextField(
                        value = state.confirmPassword,
                        onValueChange = viewModel::updateConfirmPassword,
                        label = "Confirm password",
                        placeholder = "Repeat your password",
                        visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (!state.isAuthLoading) viewModel.submitAuth()
                            },
                        ),
                        focusRequester = confirmationFocus,
                        trailingIcon = {
                            IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (isConfirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (isConfirmPasswordVisible) "Hide confirmation" else "Show confirmation",
                                    tint = mutedText,
                                )
                            }
                        },
                    )
                }
                ErrorText(state.authError)
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.submitAuth()
                    },
                    enabled = !state.isAuthLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = buttonShape,
                    contentPadding = PaddingValues(vertical = 15.dp),
                ) {
                    if (state.isAuthLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(if (state.isRegisterMode) "Create account" else "Log in", fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = viewModel::toggleAuthMode,
                    enabled = !state.isAuthLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (state.isRegisterMode) {
                            "Already have an account? Log in"
                        } else {
                            "New here? Create an account"
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
private fun BenefitPanel() {
    AppCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            BenefitRow(Icons.Filled.Insights, "Understand every month", "Income, spending, and categories at a glance")
            BenefitRow(Icons.Filled.Add, "Capture expenses quickly", "Add a transaction in a few focused steps")
            BenefitRow(Icons.Filled.Shield, "Private session storage", "Your sign-in token is encrypted on this device")
        }
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(softGreenCard, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = financeGreen, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, color = mutedText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MoneyApp(
    state: MoneyManagerUiState,
    viewModel: MoneyManagerViewModel,
    notificationsEnabled: Boolean,
    onEnableNotifications: () -> Unit,
    onSimulatePurchaseSignal: () -> Unit,
    onImportRevolut: () -> Unit,
) {
    Scaffold(
        containerColor = appBackground,
        bottomBar = {
            if (!state.isTransactionFormOpen && state.growthDestination == null) {
                BottomNav(state = state, viewModel = viewModel)
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isTransactionFormOpen -> TransactionEditor(state, viewModel)
                state.growthDestination != null -> GrowthDestinationScreen(
                    destination = state.growthDestination,
                    state = state,
                    viewModel = viewModel,
                    notificationsEnabled = notificationsEnabled,
                    onEnableNotifications = onEnableNotifications,
                )
                state.selectedTab == AppTab.Dashboard -> DashboardScreen(state, viewModel)
                state.selectedTab == AppTab.Transactions -> TransactionsScreen(state, viewModel)
                state.selectedTab == AppTab.Investments -> GrowthInvestmentScreen(state, viewModel)
                else -> ProfileScreen(
                    state = state,
                    viewModel = viewModel,
                    notificationsEnabled = notificationsEnabled,
                    onEnableNotifications = onEnableNotifications,
                    onSimulatePurchaseSignal = onSimulatePurchaseSignal,
                    onImportRevolut = onImportRevolut,
                )
            }
        }
    }

    if (state.isCategoryPickerOpen) CategoryPickerSheet(state, viewModel)
    if (state.isExportDialogOpen) ExportDialog(state, viewModel)
    state.importMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearImportMessage,
            title = { Text("Revolut import") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::clearImportMessage) { Text("OK") } },
        )
    }
}

@Composable
private fun BottomNav(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    Surface(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        color = appSurface,
        shape = RoundedCornerShape(23.dp),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavDestination(AppTab.Dashboard, state, viewModel)
            NavDestination(AppTab.Transactions, state, viewModel)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(financeGreen, RoundedCornerShape(16.dp))
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Add transaction",
                            onClick = viewModel::openNewTransactionForm,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            NavDestination(AppTab.Investments, state, viewModel)
            NavDestination(AppTab.Profile, state, viewModel)
        }
    }
}

@Composable
private fun RowScope.NavDestination(
    tab: AppTab,
    state: MoneyManagerUiState,
    viewModel: MoneyManagerViewModel,
) {
    val selected = state.selectedTab == tab
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(role = Role.Tab) { viewModel.selectTab(tab) }
            .semantics { this.selected = selected }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            tabIcon(tab),
            contentDescription = null,
            tint = if (selected) financeGreen else mutedText,
            modifier = Modifier.size(20.dp),
        )
        Text(
            tabLabel(tab).uppercase(),
            color = if (selected) financeGreen else mutedText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun InvestmentScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("WEALTH", color = mutedText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("Invest", color = nearBlack, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                }
                Surface(color = stocksColor.copy(alpha = 0.13f), shape = CircleShape) {
                    Text(
                        "PREVIEW",
                        color = stocksColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                    )
                }
            }
        }
        item { InvestmentPortfolioCard() }
        item { InvestmentAllocation() }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Holdings", color = nearBlack, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                InvestmentAssetRow("A", "Apple", "AAPL · 4.2 shares", "€812.40", "+2.41%", stocksColor)
                InvestmentAssetRow("B", "Bitcoin", "BTC · 0.084 BTC", "€5,420.10", "+12.8%", cryptoColor)
            }
        }
        item { InvestmentConnectionCard() }
    }
}

@Composable
private fun InvestmentPortfolioCard() {
    AppCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("PORTFOLIO VALUE", color = mutedText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("€15,600.80", color = nearBlack, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
                Text("+8.24%", color = incomeColor, fontWeight = FontWeight.Bold)
            }
            val lineColor = stocksColor
            val chartSurface = stocksColor.copy(alpha = 0.12f)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .background(chartSurface, RoundedCornerShape(16.dp))
                    .semantics { contentDescription = "Preview portfolio chart rising from 11,900 euros to 15,600 euros" },
            ) {
                val values = listOf(0.12f, 0.28f, 0.22f, 0.55f, 0.72f, 0.92f)
                val horizontalPadding = 18.dp.toPx()
                val verticalPadding = 20.dp.toPx()
                val step = (size.width - horizontalPadding * 2) / (values.size - 1)
                values.zipWithNext().forEachIndexed { index, (startValue, endValue) ->
                    val start = Offset(horizontalPadding + step * index, size.height - verticalPadding - startValue * (size.height - verticalPadding * 2))
                    val end = Offset(horizontalPadding + step * (index + 1), size.height - verticalPadding - endValue * (size.height - verticalPadding * 2))
                    drawLine(lineColor, start, end, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                }
                values.forEachIndexed { index, value ->
                    drawCircle(
                        color = lineColor,
                        radius = 4.dp.toPx(),
                        center = Offset(horizontalPadding + step * index, size.height - verticalPadding - value * (size.height - verticalPadding * 2)),
                    )
                }
            }
        }
    }
}

@Composable
private fun InvestmentAllocation() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Allocation", color = nearBlack, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().height(10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.weight(0.68f).height(10.dp).background(stocksColor, RoundedCornerShape(5.dp)))
            Box(Modifier.weight(0.32f).height(10.dp).background(cryptoColor, RoundedCornerShape(5.dp)))
        }
        Row(Modifier.fillMaxWidth()) {
            InvestmentAllocationLabel(stocksColor, "Stocks", "68%", Modifier.weight(1f))
            InvestmentAllocationLabel(cryptoColor, "Crypto", "32%", Modifier.weight(1f))
        }
    }
}

@Composable
private fun InvestmentAllocationLabel(color: Color, title: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(9.dp).background(color, CircleShape))
        Text(title, color = mutedText, style = MaterialTheme.typography.bodySmall)
        Text(value, color = nearBlack, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InvestmentAssetRow(
    symbol: String,
    name: String,
    detail: String,
    value: String,
    change: String,
    color: Color,
) {
    AppCard {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(42.dp).background(color, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(symbol, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, color = nearBlack, fontWeight = FontWeight.Bold)
                Text(detail, color = mutedText, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(value, color = nearBlack, fontWeight = FontWeight.SemiBold)
                Text(change, color = incomeColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InvestmentConnectionCard() {
    Surface(color = invertedSurface, shape = cardShape) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text("Connect your investments", color = inverseText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Broker and exchange integrations are planned. Your existing money tracking remains fully functional.",
                color = inverseText.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InvestmentSoonPill("STOCK BROKERS")
                InvestmentSoonPill("CRYPTO")
                Spacer(Modifier.weight(1f))
                Text("SOON", color = cryptoColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InvestmentSoonPill(title: String) {
    Text(
        title,
        color = inverseText,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(inverseText.copy(alpha = 0.08f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionsScreen(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
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
private fun ProfileScreen(
    state: MoneyManagerUiState,
    viewModel: MoneyManagerViewModel,
    notificationsEnabled: Boolean,
    onEnableNotifications: () -> Unit,
    onSimulatePurchaseSignal: () -> Unit,
    onImportRevolut: () -> Unit,
) {
    var showDeleteAccountConfirmation by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SimpleHeader("Settings") }
        item {
            AppCard(Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(softGreenCard, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = financeGreen)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Signed in", color = mutedText, style = MaterialTheme.typography.bodySmall)
                        Text(
                            state.signedInEmail,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        item {
            ProfileGroup(title = "Connection", modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val connected = state.connectionStatus == ConnectionStatus.Connected
                    Icon(
                        imageVector = when (state.connectionStatus) {
                            ConnectionStatus.Connected -> Icons.Filled.CloudDone
                            ConnectionStatus.Offline -> Icons.Filled.CloudOff
                            ConnectionStatus.Checking -> Icons.Filled.Refresh
                        },
                        contentDescription = null,
                        tint = if (connected) incomeColor else if (state.connectionStatus == ConnectionStatus.Offline) expenseColor else mutedText,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            when (state.connectionStatus) {
                                ConnectionStatus.Connected -> "Service available"
                                ConnectionStatus.Offline -> "Service unavailable"
                                ConnectionStatus.Checking -> "Checking service…"
                            },
                            fontWeight = FontWeight.SemiBold,
                        )
                        state.connectionMessage?.let {
                            Text(it, color = mutedText, style = MaterialTheme.typography.bodySmall)
                        }
                        if (BuildConfig.DEBUG) {
                            Text(BuildConfig.API_BASE_URL, color = mutedText, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    IconButton(onClick = viewModel::refreshHealth) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Check connection")
                    }
                }
            }
        }
        item {
            ProfileGroup(title = "Plan", modifier = Modifier.padding(horizontal = 16.dp)) {
                ProfileAction(
                    icon = Icons.Filled.CalendarMonth,
                    title = "Scheduled money",
                    subtitle = "Recurring income and expenses",
                    onClick = { viewModel.openGrowthDestination(GrowthDestination.Schedules) },
                )
                HorizontalDivider(color = softDivider)
                ProfileAction(
                    icon = Icons.Filled.Insights,
                    title = "Budgets",
                    subtitle = "Weekly and monthly spending limits",
                    onClick = { viewModel.openGrowthDestination(GrowthDestination.Budgets) },
                )
                HorizontalDivider(color = softDivider)
                ProfileAction(
                    icon = Icons.Filled.Notifications,
                    title = "Notifications",
                    subtitle = "Budget, spending, schedule, and investment alerts",
                    onClick = { viewModel.openGrowthDestination(GrowthDestination.Notifications) },
                )
            }
        }
        item {
            ProfileGroup(title = "Future connections", modifier = Modifier.padding(horizontal = 16.dp)) {
                IntegrationRoadmapRow(Icons.Filled.AccountBalance, "Bank accounts", "Balances and transactions")
                HorizontalDivider(color = softDivider)
                IntegrationRoadmapRow(Icons.AutoMirrored.Filled.ShowChart, "Stock brokers", "Holdings and performance")
                HorizontalDivider(color = softDivider)
                IntegrationRoadmapRow(Icons.Filled.CurrencyBitcoin, "Crypto exchanges", "Wallets and positions")
            }
        }
        if (BuildConfig.DEBUG) {
            item {
                ProfileGroup(title = "Notifications", modifier = Modifier.padding(horizontal = 16.dp)) {
                    ProfileAction(
                        icon = if (notificationsEnabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                        title = if (notificationsEnabled) "Purchase reminders enabled" else "Enable purchase reminders",
                        subtitle = if (notificationsEnabled) {
                            "Money Manager can remind you to record detected purchases"
                        } else {
                            "Permission is requested only when you choose to enable reminders"
                        },
                        onClick = if (notificationsEnabled) ({}) else onEnableNotifications,
                        enabled = !notificationsEnabled,
                    )
                }
            }
        }
        item {
            ProfileGroup(title = "Your data", modifier = Modifier.padding(horizontal = 16.dp)) {
                ProfileAction(
                    icon = Icons.Filled.FileUpload,
                    title = "Import Revolut CSV",
                    subtitle = "Add completed EUR transactions",
                    onClick = onImportRevolut,
                    enabled = !state.isImporting,
                )
                HorizontalDivider(color = softDivider)
                ProfileAction(
                    icon = Icons.Filled.FileDownload,
                    title = "Export transactions",
                    subtitle = "Share a CSV for the selected month",
                    onClick = viewModel::openExportDialog,
                )
            }
        }
        if (BuildConfig.DEBUG) {
            item {
                ProfileGroup(title = "Developer tools", modifier = Modifier.padding(horizontal = 16.dp)) {
                    ProfileAction(
                        icon = Icons.Filled.FlashOn,
                        title = "Simulate purchase signal",
                        subtitle = if (notificationsEnabled) "Send a local test notification" else "Notification permission will be requested",
                        onClick = onSimulatePurchaseSignal,
                    )
                }
            }
        }
        item {
            ProfileGroup(title = "About", modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Version", color = mutedText)
                    Text(BuildConfig.VERSION_NAME, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = viewModel::logout,
                    modifier = Modifier.fillMaxWidth(),
                    shape = buttonShape,
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    Text("Log out", fontWeight = FontWeight.SemiBold)
                }
                TextButton(
                    onClick = { showDeleteAccountConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isAccountDeleting,
                    colors = ButtonDefaults.textButtonColors(contentColor = expenseColor),
                ) {
                    if (state.isAccountDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Delete account")
                }
                ErrorText(state.profileError)
            }
        }
    }

    if (showDeleteAccountConfirmation) {
        DestructiveConfirmationDialog(
            title = "Delete account?",
            message = "This permanently deletes your account, categories, and every transaction. This cannot be undone.",
            confirmLabel = "Delete permanently",
            onDismiss = { showDeleteAccountConfirmation = false },
            onConfirm = {
                showDeleteAccountConfirmation = false
                viewModel.deleteAccount()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionEditor(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    var showDiscardConfirmation by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val requestClose = {
        if (!state.isFormSaving) {
            if (state.hasUnsavedFormChanges) showDiscardConfirmation = true else viewModel.closeTransactionForm()
        }
    }
    BackHandler(onBack = requestClose)

    Scaffold(
        containerColor = appBackground,
        bottomBar = {
            Surface(color = appSurface, shadowElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = requestClose,
                        enabled = !state.isFormSaving,
                        modifier = Modifier.weight(1f),
                        shape = buttonShape,
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = viewModel::saveTransaction,
                        enabled = state.isFormValid && !state.isFormSaving,
                        modifier = Modifier.weight(1f),
                        shape = buttonShape,
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        if (state.isFormSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (state.editingId == null) "Add" else "Save", fontWeight = FontWeight.Bold)
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
                trailingContentDescription = "Close editor",
                onTrailingClick = requestClose,
                trailingEnabled = !state.isFormSaving,
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SegmentButton(
                        text = "Expense",
                        selected = state.formType == "expense",
                        onClick = { viewModel.updateFormType("expense") },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isFormSaving,
                    )
                    SegmentButton(
                        text = "Income",
                        selected = state.formType == "income",
                        onClick = { viewModel.updateFormType("income") },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isFormSaving,
                    )
                }
                AppTextField(
                    value = state.formAmount,
                    onValueChange = viewModel::updateFormAmount,
                    label = "Amount",
                    placeholder = "0.00",
                    prefix = currencySymbol(state.formCurrency),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    large = true,
                    enabled = !state.isFormSaving,
                )
                CategoryButton(state.formCategory, viewModel::openCategoryPicker, enabled = !state.isFormSaving)
                AppTextField(
                    value = state.formDescription,
                    onValueChange = viewModel::updateFormDescription,
                    label = "Description",
                    placeholder = "Groceries, rent, salary…",
                    supportingText = "Optional · ${state.formDescription.length}/200",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isFormSaving,
                )
                DateField(
                    label = "Date",
                    isoDate = state.formOccurredAt,
                    onClick = { showDatePicker = true },
                    enabled = !state.isFormSaving,
                )
                ErrorText(state.formError)
                if (!state.isFormValid && state.formError == null) {
                    Text(
                        "Enter a positive amount, choose a category, and select a date to continue.",
                        color = mutedText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text("Discard changes?") },
            text = { Text("Your edits have not been saved.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmation = false
                        viewModel.closeTransactionForm()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = expenseColor),
                ) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) { Text("Keep editing") }
            },
        )
    }

    if (showDatePicker) {
        MoneyManagerDatePicker(
            initialIsoDate = state.formOccurredAt,
            onDismiss = { showDatePicker = false },
            onDateSelected = {
                viewModel.updateFormOccurredAt(it)
                showDatePicker = false
            },
        )
    }
}

@Composable
private fun AppHeader(
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
private fun SimpleHeader(
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
private fun BalanceCard(state: MoneyManagerUiState, modifier: Modifier = Modifier) {
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
private fun SummaryTiles(state: MoneyManagerUiState, modifier: Modifier = Modifier) {
    val summary = state.summary ?: return
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryTile("Income", BigDecimal(summary.income).money(summary.currency), incomeColor, Modifier.weight(1f))
        SummaryTile("Spent", BigDecimal(summary.expense).money(summary.currency), expenseColor, Modifier.weight(1f))
        SummaryTile("Entries", summary.transactionCount.toString(), nearBlack, Modifier.weight(0.72f))
    }
}

@Composable
private fun SummaryTile(label: String, value: String, color: Color, modifier: Modifier) {
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
private fun SpendingCard(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel, modifier: Modifier = Modifier) {
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
private fun ActiveCategoryFilter(category: String, onClear: () -> Unit, modifier: Modifier = Modifier) {
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

@Composable
private fun TransactionSearch(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel, modifier: Modifier = Modifier) {
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
private fun TypeFilters(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SegmentButton("All", state.filterType == null, { viewModel.updateFilterType(null) }, Modifier.weight(1f))
        SegmentButton("Expenses", state.filterType == "expense", { viewModel.updateFilterType("expense") }, Modifier.weight(1f))
        SegmentButton("Income", state.filterType == "income", { viewModel.updateFilterType("income") }, Modifier.weight(1f))
    }
}

@Composable
private fun CategoryFilter(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel, modifier: Modifier = Modifier) {
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
private fun DayCard(
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
private fun TransactionRow(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    ModalBottomSheet(
        onDismissRequest = { if (!state.isCategoryMutating) viewModel.closeCategoryPicker() },
        containerColor = appSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Choose category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                IconButton(onClick = viewModel::closeCategoryPicker, enabled = !state.isCategoryMutating) {
                    Icon(Icons.Filled.Close, contentDescription = "Close category picker", tint = mutedText)
                }
            }
            state.formCategoryOptions.forEach { category ->
                CategoryChoice(category, selected = state.formCategory == category.name, state, viewModel)
            }
            HorizontalDivider(color = softDivider)
            AppTextField(
                value = state.newCategoryName,
                onValueChange = viewModel::updateNewCategoryName,
                label = "Custom category",
                placeholder = "e.g., Pets",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (!state.isCategoryMutating) viewModel.addCategory() }),
                enabled = !state.isCategoryMutating,
            )
            Button(
                onClick = viewModel::addCategory,
                enabled = !state.isCategoryMutating && state.newCategoryName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = buttonShape,
            ) {
                if (state.isCategoryMutating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text("Add category")
            }
            ErrorText(state.categoryError)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CategoryChoice(
    category: Category,
    selected: Boolean,
    state: MoneyManagerUiState,
    viewModel: MoneyManagerViewModel,
) {
    var confirmDelete by remember(category.id) { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = { viewModel.chooseFormCategory(category.name) },
            enabled = !state.isCategoryMutating,
            modifier = Modifier
                .weight(1f)
                .semantics { this.selected = selected },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (selected) financeGreen else softDivider),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (selected) softGreenCard else appSurface,
                contentColor = nearBlack,
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(category.name)
                Spacer(Modifier.width(12.dp))
                Text(categoryTitle(category.name), fontWeight = FontWeight.SemiBold)
            }
        }
        if (!category.isDefault && category.id != 0) {
            IconButton(onClick = { confirmDelete = true }, enabled = !state.isCategoryMutating) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete ${categoryTitle(category.name)} category",
                    tint = expenseColor,
                )
            }
        }
    }
    if (confirmDelete) {
        DestructiveConfirmationDialog(
            title = "Delete ${categoryTitle(category.name)}?",
            message = "Existing transactions keep their category, but it will no longer appear as a choice.",
            confirmLabel = "Delete category",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                viewModel.deleteCategory(category)
            },
        )
    }
}

@Composable
private fun ExportDialog(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    var selectingStartDate by remember { mutableStateOf<Boolean?>(null) }
    AlertDialog(
        onDismissRequest = { if (!state.isExporting) viewModel.closeExportDialog() },
        containerColor = appSurface,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Export transactions", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose an inclusive date range. A CSV will open in the Android share sheet.", color = mutedText)
                DateField("From", state.exportFrom, enabled = !state.isExporting) { selectingStartDate = true }
                DateField("To", state.exportTo, enabled = !state.isExporting) { selectingStartDate = false }
                ErrorText(state.exportError)
            }
        },
        confirmButton = {
            Button(onClick = viewModel::exportTransactions, enabled = !state.isExporting, shape = buttonShape) {
                if (state.isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::closeExportDialog, enabled = !state.isExporting) { Text("Cancel") }
        },
    )

    selectingStartDate?.let { isStart ->
        MoneyManagerDatePicker(
            initialIsoDate = if (isStart) state.exportFrom else state.exportTo,
            onDismiss = { selectingStartDate = null },
            onDateSelected = {
                if (isStart) viewModel.updateExportFrom(it) else viewModel.updateExportTo(it)
                selectingStartDate = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoneyManagerDatePicker(
    initialIsoDate: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit,
) {
    val initialMillis = runCatching {
        LocalDate.parse(initialIsoDate).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }.getOrNull()
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString(),
                        )
                    }
                },
                enabled = pickerState.selectedDateMillis != null,
            ) { Text("Choose") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
private fun DateField(label: String, isoDate: String, enabled: Boolean = true, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold)
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = inputShape,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 15.dp),
        ) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(isoDate.toDisplayDate(), modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
        }
    }
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
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    large: Boolean = false,
    focusRequester: FocusRequester? = null,
    supportingText: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .then(if (focusRequester == null) Modifier else Modifier.focusRequester(focusRequester)),
        singleLine = true,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = mutedText.copy(alpha = 0.72f)) },
        prefix = if (prefix == null) null else ({ Text(prefix, color = mutedText) }),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = inputShape,
        textStyle = if (large) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.bodyLarge,
        supportingText = supportingText?.let { text -> ({ Text(text) }) },
        trailingIcon = trailingIcon,
    )
}

@Composable
private fun CategoryButton(category: String, onClick: () -> Unit, enabled: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Category", fontWeight = FontWeight.SemiBold)
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = inputShape,
            border = BorderStroke(1.dp, softDivider),
            contentPadding = PaddingValues(14.dp),
        ) {
            CategoryIcon(category)
            Spacer(Modifier.width(12.dp))
            Text(categoryTitle(category), color = nearBlack, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = mutedText)
        }
    }
}

@Composable
private fun SegmentButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(text, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = Color.Transparent,
            selectedBorderColor = Color.Transparent,
            borderWidth = 0.dp,
            selectedBorderWidth = 0.dp,
        ),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = financeGreen,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
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
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) nearBlack else mutedText.copy(alpha = 0.35f),
        )
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun ProfileGroup(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    AppCard(modifier) {
        Column {
            Text(
                title,
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            HorizontalDivider(color = softDivider)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }
}

@Composable
private fun IntegrationRoadmapRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$title, $subtitle, coming later" }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(38.dp).background(softGreenSurface, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = financeGreen, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = nearBlack, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = mutedText, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "SOON",
            color = mutedText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.background(appBackground, CircleShape).padding(horizontal = 8.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun ProfileAction(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.Transparent),
        contentPadding = PaddingValues(10.dp),
    ) {
        Box(Modifier.size(38.dp).background(softGreenSurface, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = financeGreen, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(title, color = nearBlack, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, color = mutedText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CategoryIcon(category: String) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(categoryColor(category).copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = null,
            tint = categoryColor(category),
            modifier = Modifier.size(19.dp),
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
    ) { content() }
}

@Composable
private fun LoadingCard(message: String, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        Column(
            Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp), strokeWidth = 3.dp)
            Text(message, color = mutedText)
        }
    }
}

@Composable
private fun FailureCard(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.CloudOff, contentDescription = null, tint = expenseColor, modifier = Modifier.size(32.dp))
            Text("Could not load this month", fontWeight = FontWeight.Bold)
            Text(message, color = mutedText, textAlign = TextAlign.Center)
            Button(onClick = onRetry) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Try again")
            }
        }
    }
}

@Composable
private fun InlineRetry(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = softGreenSurface) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, color = mutedText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun EmptyCard(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier) {
        Column(
            Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CategoryIcon("other")
            Text(title, fontWeight = FontWeight.Bold)
            Text(message, color = mutedText, textAlign = TextAlign.Center)
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun ErrorText(error: String?, modifier: Modifier = Modifier) {
    if (error != null) {
        Text(
            error,
            color = expenseColor,
            style = MaterialTheme.typography.bodySmall,
            modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun DestructiveConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = expenseColor),
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DonutChart(
    totals: List<CategoryTotal>,
    currency: String,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = totals.sumOfMoney()
    val trackColor = softGreenSurface
    val description = totals.joinToString(prefix = "Spending chart. ", separator = ", ") {
        "${categoryTitle(it.category)} ${it.amount.money(currency)}"
    }
    Box(
        modifier = modifier.semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(totals) {
                    detectTapGestures { offset ->
                        findPieCategoryForTap(offset, size.width.toFloat(), size.height.toFloat(), totals)
                            ?.let(onCategorySelected)
                    }
                },
        ) {
            val strokeWidth = size.minDimension * 0.17f
            var start = -90f
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth),
            )
            totals.forEach { item ->
                val sweep = item.amount.divide(total, 6, RoundingMode.HALF_UP).toFloat() * 360f
                val dimmed = selectedCategory != null && selectedCategory != item.category
                drawArc(
                    color = categoryColor(item.category).copy(alpha = if (dimmed) 0.28f else 1f),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Spent", color = mutedText, style = MaterialTheme.typography.labelSmall)
            Text(
                total.money(currency),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LegendRow(item: CategoryTotal, total: BigDecimal, selected: Boolean, onClick: () -> Unit) {
    val percent = if (total > BigDecimal.ZERO) {
        item.amount.multiply(BigDecimal(100)).divide(total, 0, RoundingMode.HALF_UP).toPlainString()
    } else {
        "0"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Filter by ${categoryTitle(item.category)}", onClick = onClick)
            .semantics {
                role = Role.Button
                this.selected = selected
            }
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(categoryColor(item.category), CircleShape))
        Spacer(Modifier.width(7.dp))
        Text(
            categoryTitle(item.category),
            modifier = Modifier.weight(1f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text("$percent%", color = mutedText)
    }
}
