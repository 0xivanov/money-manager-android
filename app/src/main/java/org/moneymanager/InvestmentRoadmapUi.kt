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
internal fun InvestmentScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("WEALTH", color = mutedText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("Invest", color = nearBlack, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                }
                Surface(color = stocksColor.copy(alpha = 0.13f), shape = CircleShape) {
                    Text(
                        "PREVIEW",
                        color = stocksColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                    )
                }
            }
        }
        item { InvestmentPortfolioCard() }
        item { InvestmentAllocation() }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Holdings", color = nearBlack, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                InvestmentAssetRow("A", "Apple", "AAPL · 4.2 shares", "€812.40", "+2.41%", stocksColor)
                InvestmentAssetRow("B", "Bitcoin", "BTC · 0.084 BTC", "€5,420.10", "+12.8%", cryptoColor)
            }
        }
        item { InvestmentConnectionCard() }
    }
}

@Composable
private fun InvestmentPortfolioCard() {
    AppCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("PORTFOLIO VALUE", color = mutedText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("€15,600.80", color = nearBlack, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
                Text("+8.24%", color = incomeColor, fontWeight = FontWeight.Bold)
            }
            val lineColor = stocksColor
            val chartSurface = stocksColor.copy(alpha = 0.12f)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .background(chartSurface, RoundedCornerShape(16.dp))
                    .semantics { contentDescription = "Preview portfolio chart rising from 11,900 euros to 15,600 euros" },
            ) {
                val values = listOf(0.12f, 0.28f, 0.22f, 0.55f, 0.72f, 0.92f)
                val horizontalPadding = 18.dp.toPx()
                val verticalPadding = 20.dp.toPx()
                val step = (size.width - horizontalPadding * 2) / (values.size - 1)
                values.zipWithNext().forEachIndexed { index, (startValue, endValue) ->
                    val start = Offset(horizontalPadding + step * index, size.height - verticalPadding - startValue * (size.height - verticalPadding * 2))
                    val end = Offset(horizontalPadding + step * (index + 1), size.height - verticalPadding - endValue * (size.height - verticalPadding * 2))
                    drawLine(lineColor, start, end, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                }
                values.forEachIndexed { index, value ->
                    drawCircle(
                        color = lineColor,
                        radius = 4.dp.toPx(),
                        center = Offset(horizontalPadding + step * index, size.height - verticalPadding - value * (size.height - verticalPadding * 2)),
                    )
                }
            }
        }
    }
}

@Composable
private fun InvestmentAllocation() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Allocation", color = nearBlack, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().height(10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.weight(0.68f).height(10.dp).background(stocksColor, RoundedCornerShape(5.dp)))
            Box(Modifier.weight(0.32f).height(10.dp).background(cryptoColor, RoundedCornerShape(5.dp)))
        }
        Row(Modifier.fillMaxWidth()) {
            InvestmentAllocationLabel(stocksColor, "Stocks", "68%", Modifier.weight(1f))
            InvestmentAllocationLabel(cryptoColor, "Crypto", "32%", Modifier.weight(1f))
        }
    }
}

@Composable
private fun InvestmentAllocationLabel(color: Color, title: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(9.dp).background(color, CircleShape))
        Text(title, color = mutedText, style = MaterialTheme.typography.bodySmall)
        Text(value, color = nearBlack, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InvestmentAssetRow(
    symbol: String,
    name: String,
    detail: String,
    value: String,
    change: String,
    color: Color,
) {
    AppCard {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(42.dp).background(color, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(symbol, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, color = nearBlack, fontWeight = FontWeight.Bold)
                Text(detail, color = mutedText, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(value, color = nearBlack, fontWeight = FontWeight.SemiBold)
                Text(change, color = incomeColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InvestmentConnectionCard() {
    Surface(color = invertedSurface, shape = cardShape) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text("Connect your investments", color = inverseText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Broker and exchange integrations are planned. Your existing money tracking remains fully functional.",
                color = inverseText.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InvestmentSoonPill("STOCK BROKERS")
                InvestmentSoonPill("CRYPTO")
                Spacer(Modifier.weight(1f))
                Text("SOON", color = cryptoColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InvestmentSoonPill(title: String) {
    Text(
        title,
        color = inverseText,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(inverseText.copy(alpha = 0.08f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}
