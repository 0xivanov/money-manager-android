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
import androidx.compose.runtime.Composable
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale
import kotlin.math.atan2
import org.moneymanager.model.Transaction

fun tabLabel(tab: AppTab): String =
    when (tab) {
        AppTab.Dashboard -> "Home"
        AppTab.Transactions -> "Activity"
        AppTab.Investments -> "Invest"
        AppTab.Profile -> "Profile"
    }

fun tabIcon(tab: AppTab): ImageVector =
    when (tab) {
        AppTab.Dashboard -> Icons.Filled.Home
        AppTab.Transactions -> Icons.Filled.Receipt
        AppTab.Investments -> Icons.AutoMirrored.Filled.TrendingUp
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
    return prefix + BigDecimal(amount).abs().money(currency)
}

fun BigDecimal.signedMoney(currencyCode: String = "EUR"): String {
    val sign = if (this >= BigDecimal.ZERO) "+" else "-"
    return sign + abs().money(currencyCode)
}

fun BigDecimal.money(currencyCode: String = "EUR", locale: Locale = Locale.getDefault()): String {
    val formatter = NumberFormat.getCurrencyInstance(locale).apply {
        currency = runCatching { Currency.getInstance(currencyCode) }.getOrDefault(Currency.getInstance("EUR"))
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return formatter.format(setScale(2, RoundingMode.HALF_UP))
}

fun BigDecimal.formatMoney(locale: Locale = Locale.getDefault()): String =
    NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(setScale(2, RoundingMode.HALF_UP))

fun currencySymbol(currencyCode: String, locale: Locale = Locale.getDefault()): String =
    runCatching { Currency.getInstance(currencyCode).getSymbol(locale) }.getOrDefault(currencyCode)

fun List<CategoryTotal>.sumOfMoney(): BigDecimal =
    fold(BigDecimal.ZERO) { total, item -> total + item.amount }

fun parseLocalizedDecimal(value: String, locale: Locale = Locale.getDefault()): BigDecimal? {
    val raw = value.trim().filterNot { it.isWhitespace() || it == '\u00A0' }
    if (raw.isBlank()) return null
    val symbols = java.text.DecimalFormatSymbols.getInstance(locale)
    val localeDecimalSeparator = symbols.decimalSeparator
    val groupingSeparator = symbols.groupingSeparator
    val fallbackSeparator = if (localeDecimalSeparator == ',') '.' else ','

    val decimalSeparator = when {
        localeDecimalSeparator in raw -> localeDecimalSeparator
        groupingSeparator in raw && normalizeLocalizedWhole(raw, groupingSeparator) != null -> {
            return normalizeLocalizedWhole(raw, groupingSeparator)?.toBigDecimalOrNull()
        }
        groupingSeparator in raw && groupingSeparator in charArrayOf('.', ',') -> groupingSeparator
        fallbackSeparator in raw -> fallbackSeparator
        else -> null
    }

    if (decimalSeparator == null) {
        return normalizeLocalizedWhole(raw, groupingSeparator)?.toBigDecimalOrNull()
    }
    if (raw.count { it == decimalSeparator } > 1) return null

    val parts = raw.split(decimalSeparator, limit = 2)
    val whole = normalizeLocalizedWhole(parts[0], groupingSeparator) ?: return null
    val fraction = parts.getOrNull(1)
    if (fraction != null && (fraction.isEmpty() || !fraction.all(Char::isDigit))) return null
    val normalized = if (fraction == null) whole else "$whole.$fraction"
    return normalized.toBigDecimalOrNull()
}

private fun normalizeLocalizedWhole(value: String, groupingSeparator: Char): String? {
    if (groupingSeparator !in value) {
        return when {
            value.isEmpty() -> "0"
            value.all(Char::isDigit) -> value
            else -> null
        }
    }
    val groups = value.split(groupingSeparator)
    val first = groups.firstOrNull() ?: return null
    if (first.length !in 1..3 || first.any { !it.isDigit() }) return null
    if (groups.drop(1).any { it.length != 3 || it.any { character -> !character.isDigit() } }) return null
    return groups.joinToString("")
}

@Composable
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
        else -> date.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
    }

fun formatMonth(month: String): String =
    runCatching {
        YearMonth.parse(month).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    }.getOrDefault(month)

fun String.toDisplayDate(): String =
    runCatching {
        LocalDate.parse(take(10)).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }.getOrDefault(this)

fun String.displayDateToIso(): String {
    val trimmed = trim()
    return runCatching {
        LocalDate.parse(trimmed, DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
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
    val distanceSquared = (dx * dx) + (dy * dy)
    val innerRadius = radius * 0.62f
    if (distanceSquared > radius * radius || distanceSquared < innerRadius * innerRadius) return null

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
