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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BudgetsScreen(state: MoneyManagerUiState, viewModel: MoneyManagerViewModel) {
    var showEditor by remember { mutableStateOf(false) }
    GrowthListScaffold(
        title = "Budgets",
        eyebrow = "SPENDING PLAN",
        onBack = viewModel::closeGrowthDestination,
        onAdd = { showEditor = true },
        isLoading = state.growth.isPlanningLoading,
        error = state.growth.error,
        onRetry = viewModel::refreshPlanning,
    ) {
        item {
            InfoCard(
                Icons.Filled.Notifications,
                "Know before you overspend",
                "Each budget tracks posted spending and can alert you near the limit.",
            )
        }
        if (!state.growth.isPlanningLoading && state.growth.budgets.isEmpty()) {
            item { GrowthEmptyCard("No budgets yet", "Set an overall or category limit.", "Create budget") { showEditor = true } }
        } else {
            items(state.growth.budgets, key = { it.id }) { budget ->
                BudgetCard(budget, !state.growth.isMutating) { viewModel.deleteBudget(budget.id) }
            }
        }
    }
    if (showEditor) {
        BudgetEditor(
            isSaving = state.growth.isMutating,
            onDismiss = { showEditor = false },
            onSave = {
                viewModel.createBudget(it)
                showEditor = false
            },
        )
    }
}
@Composable
private fun BudgetCard(budget: Budget, enabled: Boolean, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    val percent = budget.progressPercent.toFloatOrNull()?.coerceIn(0f, 100f) ?: 0f
    val warning = percent >= budget.warningThreshold
    GrowthCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(budget.name, fontWeight = FontWeight.Bold)
                Text(
                    budget.category?.let(::categoryTitle) ?: "All spending",
                    color = mutedText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text("${percent.roundToInt()}%", color = if (warning) expenseColor else financeGreen, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(9.dp),
            color = if (warning) expenseColor else financeGreen,
            trackColor = softGreenSurface,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${BigDecimal(budget.spentAmount).money(budget.currency)} of ${BigDecimal(budget.amount).money(budget.currency)}",
                color = mutedText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            Text(budget.period.uppercase(), color = mutedText, style = MaterialTheme.typography.labelSmall)
            IconButton(onClick = { confirmDelete = true }, enabled = enabled) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete budget", tint = expenseColor)
            }
        }
    }
    if (confirmDelete) {
        DeleteDialog("Delete ${budget.name}?", "The budget history and limit will be removed.", { confirmDelete = false }) {
            confirmDelete = false
            onDelete()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetEditor(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (BudgetRequest) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("monthly") }
    var threshold by remember { mutableStateOf("80") }
    val parsedAmount = parseLocalizedDecimal(amount)
    val parsedThreshold = threshold.toIntOrNull()
    val valid = name.isNotBlank() && parsedAmount != null && parsedAmount > BigDecimal.ZERO &&
        parsedThreshold != null && parsedThreshold in 1..100
    GrowthSheet("New budget", onDismiss) {
        GrowthField(name, { name = it }, "Name", "Monthly spending")
        GrowthField(amount, { amount = it }, "Limit", "1200")
        GrowthField(category, { category = it.lowercase().trim() }, "Category (optional)", "groceries")
        ChoiceRow(listOf("weekly", "monthly"), period) { period = it }
        GrowthField(threshold, { threshold = it.filter(Char::isDigit).take(3) }, "Warn me at %", "80")
        Button(
            onClick = {
                onSave(
                    BudgetRequest(
                        name = name.trim(),
                        category = category.ifBlank { null },
                        amount = parsedAmount!!.stripTrailingZeros().toPlainString(),
                        period = period,
                        warningThreshold = parsedThreshold!!,
                    ),
                )
            },
            enabled = valid && !isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = buttonShape,
        ) { Text("Save budget", fontWeight = FontWeight.Bold) }
    }
}
