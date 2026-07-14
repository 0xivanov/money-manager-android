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
internal fun ScheduledMoneyScreen(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    var showEditor by remember { mutableStateOf(false) }
    GrowthListScaffold(
        title = "Scheduled money",
        eyebrow = "PLANNING",
        onBack = viewModel::closeGrowthDestination,
        onAdd = { showEditor = true },
        isLoading = state.growth.isPlanningLoading,
        error = state.growth.error,
        onRetry = viewModel::refreshPlanning,
    ) {
        item {
            InfoCard(
                icon = Icons.Filled.CalendarMonth,
                title = "Plan with day-level accuracy",
                body = "Repeat income or expenses every day, week, or month. Auto-post only creates transactions when their date arrives.",
            )
        }
        if (!state.growth.isPlanningLoading && state.growth.schedules.isEmpty()) {
            item {
                GrowthEmptyCard(
                    "Nothing scheduled",
                    "Add rent, salary, subscriptions, or any recurring cash flow.",
                    "Create schedule",
                ) { showEditor = true }
            }
        } else {
            items(state.growth.schedules, key = { it.id }) { schedule ->
                ScheduleCard(
                    schedule = schedule,
                    enabled = !state.growth.isMutating,
                    onToggle = { viewModel.toggleSchedule(schedule) },
                    onDelete = { viewModel.deleteSchedule(schedule.id) },
                )
            }
        }
    }
    if (showEditor) {
        ScheduleEditor(
            categories = (state.expenseCategories + state.incomeCategories).map { it.name }.distinct(),
            isSaving = state.growth.isMutating,
            onDismiss = { showEditor = false },
            onSave = {
                viewModel.createSchedule(it)
                showEditor = false
            },
        )
    }
}

@Composable
private fun ScheduleCard(
    schedule: TransactionSchedule,
    enabled: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    GrowthCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GrowthIcon(
                if (schedule.type == "income") Icons.Filled.Savings else Icons.Filled.CalendarMonth,
                if (schedule.type == "income") incomeColor else expenseColor,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(schedule.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(
                        (if (schedule.type == "income") "+" else "-") +
                            BigDecimal(schedule.amount).money(schedule.currency),
                        color = if (schedule.type == "income") incomeColor else expenseColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    schedule.recurrenceLabel(),
                    color = mutedText,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    schedule.nextOccurrenceDate?.let { "Next $it" } ?: "No upcoming occurrence",
                    color = mutedText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        HorizontalDivider(color = softDivider)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (schedule.autoPost) "AUTO-POST" else "REMINDER",
                color = financeGreen,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onToggle, enabled = enabled) {
                Icon(
                    if (schedule.status == "active") Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (schedule.status == "active") "Pause" else "Resume",
                    tint = mutedText,
                )
            }
            IconButton(onClick = { confirmDelete = true }, enabled = enabled) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = expenseColor)
            }
        }
    }
    if (confirmDelete) {
        DeleteDialog(
            title = "Delete ${schedule.name}?",
            body = "Future occurrences will be removed. Existing transactions stay in your history.",
            onDismiss = { confirmDelete = false },
            onDelete = {
                confirmDelete = false
                onDelete()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleEditor(
    categories: List<String>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (TransactionScheduleRequest) -> Unit,
) {
    var type by remember { mutableStateOf("expense") }
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember(type) {
        mutableStateOf(if (type == "income") "salary" else categories.firstOrNull() ?: "other")
    }
    var frequency by remember { mutableStateOf("monthly") }
    var interval by remember { mutableStateOf("1") }
    var startDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var autoPost by remember { mutableStateOf(true) }
    val parsedAmount = parseLocalizedDecimal(amount)
    val parsedInterval = interval.toIntOrNull()
    val valid = name.isNotBlank() && parsedAmount != null && parsedAmount > BigDecimal.ZERO &&
        parsedInterval != null && parsedInterval in 1..365 && runCatching { LocalDate.parse(startDate) }.isSuccess

    GrowthSheet("New schedule", onDismiss) {
        ChoiceRow(listOf("expense", "income"), type) { type = it }
        GrowthField(name, { name = it }, "Name", "Rent")
        GrowthField(amount, { amount = it }, "Amount", "850")
        GrowthField(category, { category = it.lowercase().trim() }, "Category", "housing")
        Text("REPEATS", color = mutedText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        ChoiceRow(listOf("daily", "weekly", "monthly"), frequency) { frequency = it }
        GrowthField(interval, { interval = it.filter(Char::isDigit).take(3) }, "Every", "1")
        GrowthField(startDate, { startDate = it.take(10) }, "Start date", "YYYY-MM-DD")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Auto-post", fontWeight = FontWeight.SemiBold)
                Text("Create the transaction on its due date", color = mutedText, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = autoPost, onCheckedChange = { autoPost = it })
        }
        Button(
            onClick = {
                val date = LocalDate.parse(startDate)
                onSave(
                    TransactionScheduleRequest(
                        type = type,
                        name = name.trim(),
                        category = category.ifBlank { "other" },
                        amount = parsedAmount!!.stripTrailingZeros().toPlainString(),
                        frequency = frequency,
                        frequencyInterval = parsedInterval!!,
                        startDate = startDate,
                        dayOfWeek = if (frequency == "weekly") date.dayOfWeek.value else null,
                        dayOfMonth = if (frequency == "monthly") date.dayOfMonth else null,
                        timezone = TimeZone.getDefault().id,
                        autoPost = autoPost,
                    ),
                )
            },
            enabled = valid && !isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = buttonShape,
        ) { Text("Save schedule", fontWeight = FontWeight.Bold) }
    }
}
