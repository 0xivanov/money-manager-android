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
import androidx.compose.foundation.layout.navigationBarsPadding
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
internal fun TransactionEditor(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
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
            Surface(
                modifier = Modifier.navigationBarsPadding(),
                color = appSurface,
                shadowElevation = 3.dp,
            ) {
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
