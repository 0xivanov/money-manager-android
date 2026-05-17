package org.moneymanager

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.atan2
import org.moneymanager.model.Transaction

fun tabLabel(tab: AppTab): String =
    when (tab) {
        AppTab.Dashboard -> "Dashboard"
        AppTab.Transactions -> "Transactions"
        AppTab.Profile -> "Profile"
    }

fun tabIcon(tab: AppTab): ImageVector =
    when (tab) {
        AppTab.Dashboard -> Icons.Filled.Home
        AppTab.Transactions -> Icons.Filled.Receipt
        AppTab.Profile -> Icons.Filled.Person
    }

fun transactionEditorTitle(state: MoneyManagerUiState): String =
    when {
        state.editingId != null -> "Edit transaction"
        state.formType == "income" -> "Add income"
        else -> "Add expense"
    }

fun Transaction.signedAmount(): String {
    val prefix = if (type == "income") "+" else "-"
    return amount.toMoneyAmount().money(prefix)
}

fun BigDecimal.signedMoney(): String {
    val sign = if (this >= BigDecimal.ZERO) "+" else "-"
    return abs().money(sign)
}

fun BigDecimal.money(prefix: String = ""): String = "$prefix€${formatMoney()}"

fun BigDecimal.formatMoney(): String = setScale(2, RoundingMode.HALF_UP).toPlainString()

fun List<CategoryTotal>.sumOfMoney(): BigDecimal =
    fold(BigDecimal.ZERO) { total, item -> total + item.amount }

fun String.toMoneyAmount(): BigDecimal = runCatching { BigDecimal(this) }.getOrDefault(BigDecimal.ZERO)

fun amountColor(amount: BigDecimal): Color =
    when {
        amount > BigDecimal.ZERO -> incomeColor
        amount < BigDecimal.ZERO -> expenseColor
        else -> nearBlack
    }

fun categoryColor(category: String): Color =
    when (category.lowercase()) {
        "food" -> Color(0xFFFF6B7A)
        "transport" -> Color(0xFF27BDB2)
        "housing" -> Color(0xFF35A9C8)
        "utilities" -> Color(0xFFFF9469)
        "health" -> Color(0xFF78CDBB)
        "entertainment" -> Color(0xFFA980CE)
        "shopping" -> Color(0xFFFF8DAA)
        "travel" -> Color(0xFF81C8EF)
        "education" -> Color(0xFFE9A24D)
        "salary" -> Color(0xFF2C9A68)
        "freelance" -> Color(0xFF35A9C8)
        "gift" -> Color(0xFFE9A24D)
        "investment" -> Color(0xFF2C9A68)
        "refund" -> Color(0xFF78CDBB)
        else -> Color(0xFF8C978F)
    }

fun categoryTitle(category: String): String =
    category.replaceFirstChar { it.uppercase() }

fun categoryIcon(category: String): ImageVector =
    when (category.lowercase()) {
        "food" -> Icons.Filled.Restaurant
        "transport" -> Icons.Filled.DirectionsCar
        "housing" -> Icons.Filled.Home
        "utilities" -> Icons.Filled.FlashOn
        "health" -> Icons.Filled.Favorite
        "entertainment" -> Icons.Filled.Movie
        "shopping" -> Icons.Filled.ShoppingBag
        "travel" -> Icons.Filled.Flight
        "education" -> Icons.Filled.School
        "salary" -> Icons.Filled.Work
        "freelance" -> Icons.Filled.Computer
        "gift" -> Icons.Filled.CardGiftcard
        "investment" -> Icons.AutoMirrored.Filled.TrendingUp
        "refund" -> Icons.Filled.Receipt
        else -> Icons.Filled.MoreHoriz
    }

fun dayTitle(date: LocalDate): String =
    when (date) {
        LocalDate.now() -> "Today"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    }

fun formatMonth(month: String): String =
    runCatching {
        YearMonth.parse(month).format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }.getOrDefault(month)

fun String.toDisplayDate(): String =
    runCatching {
        LocalDate.parse(take(10)).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }.getOrDefault(this)

fun String.displayDateToIso(): String {
    val trimmed = trim()
    return runCatching {
        LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("dd.MM.yyyy")).format(DateTimeFormatter.ISO_LOCAL_DATE)
    }.getOrElse { trimmed }
}

fun findPieCategoryForTap(
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
    val total = totals.sumOfMoney()
    var current = 0.0
    totals.forEach { item ->
        val sweep = item.amount.divide(total, 4, RoundingMode.HALF_UP).toDouble() * 360.0
        if (angle >= current && angle < current + sweep) return item.category
        current += sweep
    }
    return totals.lastOrNull()?.category
}
