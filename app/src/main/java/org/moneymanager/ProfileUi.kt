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
internal fun ProfileScreen(
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
