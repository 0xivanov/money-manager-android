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
