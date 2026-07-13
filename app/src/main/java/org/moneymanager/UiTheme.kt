package org.moneymanager

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val brandGreen = Color(0xFF064C39)

@Immutable
data class MoneyManagerColors(
    val background: Color,
    val surface: Color,
    val softSurface: Color,
    val softCard: Color,
    val primary: Color,
    val text: Color,
    val mutedText: Color,
    val divider: Color,
    val expense: Color,
    val income: Color,
    val stocks: Color,
    val crypto: Color,
    val invertedSurface: Color,
    val inverseText: Color,
)

private val lightAppColors = MoneyManagerColors(
    background = Color(0xFFFBFAF7),
    surface = Color.White,
    softSurface = Color(0xFFF2F0EA),
    softCard = Color(0xFFD8FCE8),
    primary = Color(0xFF046B3B),
    text = Color(0xFF0B0B0A),
    mutedText = Color(0xFF5C625E),
    divider = Color(0xFFE6E4DD),
    expense = Color(0xFFA9362B),
    income = Color(0xFF046B3B),
    stocks = Color(0xFF1549D5),
    crypto = Color(0xFFF49812),
    invertedSurface = Color(0xFF0B0B0A),
    inverseText = Color.White,
)

private val darkAppColors = MoneyManagerColors(
    background = Color(0xFF0B0B0A),
    surface = Color(0xFF191917),
    softSurface = Color(0xFF21211E),
    softCard = Color(0xFF093720),
    primary = Color(0xFF20D982),
    text = Color(0xFFF8F7F3),
    mutedText = Color(0xFFAEB7B1),
    divider = Color(0xFF33332F),
    expense = Color(0xFFFF6B61),
    income = Color(0xFF20D982),
    stocks = Color(0xFF2D5AE7),
    crypto = Color(0xFFFFA61F),
    invertedSurface = Color(0xFFF8F7F3),
    inverseText = Color(0xFF0B0B0A),
)

private val LocalMoneyManagerColors = staticCompositionLocalOf { lightAppColors }

@Composable
fun MoneyManagerTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val colors = if (isDark) darkAppColors else lightAppColors
    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = Color(0xFF052F20),
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.softSurface,
            onSurfaceVariant = colors.mutedText,
            outline = colors.divider,
            error = colors.expense,
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = Color.White,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.softSurface,
            onSurfaceVariant = colors.mutedText,
            outline = colors.divider,
            error = colors.expense,
        )
    }

    CompositionLocalProvider(LocalMoneyManagerColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = Shapes(
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(20.dp),
            ),
            typography = Typography(),
            content = content,
        )
    }
}

val cardShape = RoundedCornerShape(22.dp)
val buttonShape = RoundedCornerShape(18.dp)
val inputShape = RoundedCornerShape(18.dp)

val appBackground: Color
    @Composable get() = LocalMoneyManagerColors.current.background
val appSurface: Color
    @Composable get() = LocalMoneyManagerColors.current.surface
val softGreenSurface: Color
    @Composable get() = LocalMoneyManagerColors.current.softSurface
val softGreenCard: Color
    @Composable get() = LocalMoneyManagerColors.current.softCard
val financeGreen: Color
    @Composable get() = LocalMoneyManagerColors.current.primary
val nearBlack: Color
    @Composable get() = LocalMoneyManagerColors.current.text
val mutedText: Color
    @Composable get() = LocalMoneyManagerColors.current.mutedText
val softDivider: Color
    @Composable get() = LocalMoneyManagerColors.current.divider
val expenseColor: Color
    @Composable get() = LocalMoneyManagerColors.current.expense
val incomeColor: Color
    @Composable get() = LocalMoneyManagerColors.current.income
val stocksColor: Color
    @Composable get() = LocalMoneyManagerColors.current.stocks
val cryptoColor: Color
    @Composable get() = LocalMoneyManagerColors.current.crypto
val invertedSurface: Color
    @Composable get() = LocalMoneyManagerColors.current.invertedSurface
val inverseText: Color
    @Composable get() = LocalMoneyManagerColors.current.inverseText
