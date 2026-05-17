package org.moneymanager

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MoneyManagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = financeGreen,
            onPrimary = Color.White,
            background = appBackground,
            onBackground = nearBlack,
            surface = appSurface,
            onSurface = nearBlack,
            surfaceVariant = softGreenSurface,
            onSurfaceVariant = mutedText,
            error = expenseColor,
        ),
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(20.dp),
        ),
        typography = Typography(),
        content = content,
    )
}

val cardShape = RoundedCornerShape(14.dp)
val buttonShape = RoundedCornerShape(14.dp)
val inputShape = RoundedCornerShape(14.dp)

val appBackground = Color(0xFFF4F7F1)
val appSurface = Color.White
val softGreenSurface = Color(0xFFEFF5ED)
val softGreenCard = Color(0xFFE1F0E3)
val financeGreen = Color(0xFF0F5A3A)
val nearBlack = Color(0xFF15221A)
val mutedText = Color(0xFF657268)
val softDivider = Color(0xFFDDE7DC)
val expenseColor = Color(0xFFC2414B)
val incomeColor = Color(0xFF087A4B)
