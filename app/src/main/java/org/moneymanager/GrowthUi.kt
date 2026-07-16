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

@Composable
fun GrowthDestinationScreen(
    destination: GrowthDestination,
    state: MoneyManagerUiState,
    viewModel: MoneyManagerViewModel,
    notificationsEnabled: Boolean,
    onEnableNotifications: () -> Unit,
) {
    BackHandler(onBack = viewModel::closeGrowthDestination)
    when (destination) {
        GrowthDestination.Schedules -> ScheduledMoneyScreen(state, viewModel)
        GrowthDestination.Budgets -> BudgetsScreen(state, viewModel)
        GrowthDestination.Notifications -> NotificationSettingsScreen(
            state,
            viewModel,
            notificationsEnabled,
            onEnableNotifications,
        )
        GrowthDestination.InvestmentTrades -> InvestmentTradesScreen(state, viewModel)
        GrowthDestination.InvestmentHistory -> InvestmentHistoryScreen(state, viewModel)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GrowthListScaffold(
    title: String,
    eyebrow: String,
    onBack: () -> Unit,
    onAdd: (() -> Unit)? = null,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { GrowthHeader(title, eyebrow, onBack, onAdd) }
        if (error != null) item { GrowthError(error, onRetry) }
        if (isLoading) item { GrowthLoading() }
        content()
    }
}

@Composable
internal fun GrowthHeader(title: String, eyebrow: String, onBack: () -> Unit, onAdd: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(color = appSurface, shape = CircleShape) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(eyebrow, color = financeGreen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        if (onAdd != null) {
            Surface(color = financeGreen, shape = CircleShape) {
                IconButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.onPrimary) }
            }
        }
    }
}

@Composable
internal fun GrowthCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = appSurface, shape = cardShape, tonalElevation = 1.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
internal fun GrowthIcon(icon: ImageVector, color: Color) {
    Box(Modifier.size(44.dp).background(color.copy(alpha = 0.14f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun InfoCard(icon: ImageVector, title: String, body: String) {
    Surface(color = softGreenCard, shape = cardShape) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = financeGreen)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(body, color = mutedText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun GrowthEmptyCard(title: String, body: String, action: String, onClick: () -> Unit) {
    GrowthCard {
        Text(title, fontWeight = FontWeight.Bold)
        Text(body, color = mutedText)
        Button(onClick = onClick, shape = buttonShape) { Text(action) }
    }
}

@Composable
internal fun GrowthError(message: String, onRetry: () -> Unit) {
    Surface(color = expenseColor.copy(alpha = 0.12f), shape = cardShape) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, color = expenseColor, modifier = Modifier.weight(1f))
            TextButton(onClick = onRetry) { Text("Retry", color = expenseColor) }
        }
    }
}

@Composable
internal fun GrowthLoading() {
    Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
    }
}

@Composable
internal fun GrowthSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier = modifier, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GrowthSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = appBackground) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            item { Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = content) }
        }
    }
}

@Composable
internal fun GrowthField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = inputShape,
    )
}

@Composable
internal fun ChoiceRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = {
                    Text(
                        if (option in setOf("manual", "revolut_x", "trading212")) {
                            option.brokerLabel()
                        } else {
                            option.replaceFirstChar(Char::uppercase)
                        },
                    )
                },
            )
        }
    }
}

@Composable
internal fun DeleteDialog(title: String, body: String, onDismiss: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = expenseColor)) {
                Text("Delete")
            }
        },
    )
}

internal fun TransactionSchedule.recurrenceLabel(): String {
    val every = if (frequencyInterval == 1) "Every" else "Every $frequencyInterval"
    return when (frequency) {
        "daily" -> if (frequencyInterval == 1) "Every day" else "$every days"
        "weekly" -> if (frequencyInterval == 1) "Every week" else "$every weeks"
        else -> if (frequencyInterval == 1) "Every month" else "$every months"
    }
}

internal fun InvestmentSchedule.frequencyLabel(): String =
    when (frequency) {
        "daily" -> if (frequencyInterval == 1) "daily" else "every $frequencyInterval days"
        "weekly" -> if (frequencyInterval == 1) "weekly" else "every $frequencyInterval weeks"
        else -> if (frequencyInterval == 1) "monthly" else "every $frequencyInterval months"
    }

internal fun String.brokerLabel(): String = when (this) {
    "revolut_x" -> "Revolut X"
    "trading212" -> "Trading 212"
    "manual" -> "Manual"
    else -> replace('_', ' ').replaceFirstChar(Char::uppercase)
}
