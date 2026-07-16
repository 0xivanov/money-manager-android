package org.moneymanager.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject
import org.moneymanager.model.AuthResult
import org.moneymanager.model.Budget
import org.moneymanager.model.BudgetRequest
import org.moneymanager.model.Category
import org.moneymanager.model.ImportResult
import org.moneymanager.model.InvestmentPortfolio
import org.moneymanager.model.InvestmentPortfolioHistory
import org.moneymanager.model.InvestmentPortfolioHistoryPoint
import org.moneymanager.model.InvestmentPosition
import org.moneymanager.model.InvestmentPriceRequest
import org.moneymanager.model.InvestmentSchedule
import org.moneymanager.model.InvestmentScheduleRequest
import org.moneymanager.model.InvestmentTrade
import org.moneymanager.model.InvestmentTradeRequest
import org.moneymanager.model.NotificationPreferences
import org.moneymanager.model.Transaction
import org.moneymanager.model.TransactionRequest
import org.moneymanager.model.TransactionSchedule
import org.moneymanager.model.TransactionScheduleRequest
import org.moneymanager.model.TransactionSummary
import org.moneymanager.model.User

internal fun parseSummary(json: JSONObject): TransactionSummary =
    TransactionSummary(
        month = json.getString("month"),
        income = json.getString("income").also(String::toBigDecimal),
        expense = json.getString("expense").also(String::toBigDecimal),
        balance = json.getString("balance").also(String::toBigDecimal),
        currency = json.getString("currency"),
        transactionCount = json.getInt("transaction_count"),
    )

internal fun parseCategory(json: JSONObject): Category =
    Category(
        id = json.getInt("id"),
        type = json.getString("type"),
        name = json.getString("name"),
        isDefault = json.getBoolean("is_default"),
    )

internal fun TransactionRequest.toJson(): JSONObject =
    JSONObject()
        .put("type", type)
        .put("category", category)
        .put("description", description.trim())
        .put("amount", amount)
        .put("currency", currency)
        .put("occurred_at", occurredAt.dateOnly())
        .put("excluded_from_budget", excludedFromBudget)

internal fun parseSchedule(json: JSONObject): TransactionSchedule = TransactionSchedule(
    id = json.getInt("id"),
    type = json.getString("type"),
    name = json.getString("name"),
    category = json.getString("category"),
    description = json.optString("description"),
    amount = json.getString("amount"),
    currency = json.getString("currency"),
    frequency = json.getString("frequency"),
    frequencyInterval = json.optInt("frequency_interval", 1),
    startDate = json.getString("start_date").dateOnly(),
    endDate = json.optNullableString("end_date")?.dateOnly(),
    dayOfWeek = json.optIntOrNull("day_of_week"),
    dayOfMonth = json.optIntOrNull("day_of_month"),
    timezone = json.optString("timezone"),
    autoPost = json.optBoolean("auto_post"),
    status = json.getString("status"),
    nextOccurrenceDate = json.optNullableString("next_occurrence_date")?.dateOnly(),
)

internal fun TransactionScheduleRequest.toJson(): JSONObject = JSONObject()
    .put("type", type)
    .put("name", name.trim())
    .put("category", category)
    .put("description", description.trim())
    .put("amount", amount)
    .put("currency", currency)
    .put("frequency", frequency)
    .put("frequency_interval", frequencyInterval)
    .put("start_date", startDate)
    .putOptional("end_date", endDate)
    .putOptional("day_of_week", dayOfWeek)
    .putOptional("day_of_month", dayOfMonth)
    .put("timezone", timezone)
    .put("auto_post", autoPost)

internal fun parseBudget(json: JSONObject): Budget = Budget(
    id = json.getInt("id"),
    name = json.getString("name"),
    category = json.optNullableString("category"),
    amount = json.getString("amount"),
    currency = json.getString("currency"),
    period = json.getString("period"),
    warningThreshold = json.getInt("warning_threshold"),
    status = json.getString("status"),
    periodStart = json.getString("period_start").dateOnly(),
    periodEnd = json.getString("period_end").dateOnly(),
    spentAmount = json.getString("spent_amount"),
    remainingAmount = json.getString("remaining_amount"),
    progressPercent = json.getString("progress_percent"),
    alertLevel = json.getString("alert_level"),
)

internal fun BudgetRequest.toJson(): JSONObject = JSONObject()
    .put("name", name.trim())
    .putOptional("category", category)
    .put("amount", amount)
    .put("currency", currency)
    .put("period", period)
    .put("warning_threshold", warningThreshold)

internal fun parseNotificationPreferences(json: JSONObject): NotificationPreferences = NotificationPreferences(
    bankSpending = json.optBoolean("bank_spending", true),
    budgetAlerts = json.optBoolean("budget_alerts", true),
    scheduledMoney = json.optBoolean("scheduled_money", true),
    investmentReminders = json.optBoolean("investment_reminders", true),
    quietHoursStart = json.optNullableString("quiet_hours_start"),
    quietHoursEnd = json.optNullableString("quiet_hours_end"),
    timezone = json.optString("timezone"),
)

internal fun NotificationPreferences.toJson(): JSONObject = JSONObject()
    .put("bank_spending", bankSpending)
    .put("budget_alerts", budgetAlerts)
    .put("scheduled_money", scheduledMoney)
    .put("investment_reminders", investmentReminders)
    .putOptional("quiet_hours_start", quietHoursStart)
    .putOptional("quiet_hours_end", quietHoursEnd)
    .put("timezone", timezone)

internal fun parseTrade(json: JSONObject): InvestmentTrade = InvestmentTrade(
    id = json.getInt("id"),
    assetType = json.getString("asset_type"),
    symbol = json.getString("symbol"),
    assetName = json.getString("asset_name"),
    broker = json.getString("broker"),
    side = json.getString("side"),
    amount = json.getString("amount"),
    quantity = json.getString("quantity"),
    pricePerUnit = json.getString("price_per_unit"),
    priceProvider = json.optNullableString("price_provider"),
    priceAsOf = json.optNullableString("price_as_of"),
    fees = json.getString("fees"),
    currency = json.getString("currency"),
    occurredAt = json.getString("occurred_at"),
    notes = json.optString("notes"),
)

internal fun InvestmentTradeRequest.toJson(): JSONObject = JSONObject()
    .put("asset_type", assetType)
    .put("symbol", symbol.trim().uppercase())
    .put("asset_name", assetName.trim())
    .put("broker", broker)
    .put("side", side)
    .apply {
        if (amount.isNotBlank()) {
            put("amount", amount)
        } else {
            putOptional("quantity", quantity)
            putOptional("price_per_unit", pricePerUnit)
        }
    }
    .put("fees", fees)
    .put("currency", currency)
    .put("occurred_at", occurredAt)
    .put("notes", notes.trim())

internal fun parsePortfolio(json: JSONObject): InvestmentPortfolio {
    val positions = json.optJSONArray("positions") ?: JSONArray()
    return InvestmentPortfolio(
        positions = List(positions.length()) { parsePosition(positions.getJSONObject(it)) },
        investedAmount = json.getString("invested_amount"),
        currentValue = json.optNullableString("current_value"),
        unrealizedProfit = json.optNullableString("unrealized_profit"),
        realizedProfit = json.getString("realized_profit"),
        currency = json.getString("currency"),
        missingPrices = json.optInt("missing_prices"),
        unsupportedPositions = json.optInt("unsupported_positions"),
    )
}

internal fun parsePortfolioHistory(json: JSONObject): InvestmentPortfolioHistory {
    val points = json.optJSONArray("points") ?: JSONArray()
    return InvestmentPortfolioHistory(
        points = List(points.length()) { index ->
            val point = points.getJSONObject(index)
            InvestmentPortfolioHistoryPoint(
                asOf = point.getString("as_of"),
                value = point.getString("value"),
                investedAmount = point.getString("invested_amount"),
            )
        },
        currency = json.getString("currency"),
        range = json.getString("range"),
        unsupportedPositions = json.optInt("unsupported_positions"),
    )
}

internal fun parsePosition(json: JSONObject): InvestmentPosition = InvestmentPosition(
    assetType = json.getString("asset_type"),
    symbol = json.getString("symbol"),
    assetName = json.getString("asset_name"),
    broker = json.getString("broker"),
    quantity = json.getString("quantity"),
    averageCost = json.getString("average_cost"),
    investedAmount = json.getString("invested_amount"),
    currentPrice = json.optNullableString("current_price"),
    currentValue = json.optNullableString("current_value"),
    unrealizedProfit = json.optNullableString("unrealized_profit"),
    unrealizedPercent = json.optNullableString("unrealized_percent"),
    realizedProfit = json.getString("realized_profit"),
    currency = json.getString("currency"),
    priceAsOf = json.optNullableString("price_as_of"),
    priceProvider = json.optNullableString("price_provider"),
    priceStatus = json.getString("price_status"),
)

internal fun InvestmentPriceRequest.toJson(): JSONObject = JSONObject()
    .put("asset_type", assetType)
    .put("symbol", symbol.trim().uppercase())
    .put("currency", currency)
    .put("price", price)
    .putOptional("as_of", asOf)

internal fun parseInvestmentSchedule(json: JSONObject): InvestmentSchedule = InvestmentSchedule(
    id = json.getInt("id"),
    assetType = json.getString("asset_type"),
    symbol = json.getString("symbol"),
    assetName = json.getString("asset_name"),
    broker = json.getString("broker"),
    amount = json.getString("amount"),
    currency = json.getString("currency"),
    frequency = json.getString("frequency"),
    frequencyInterval = json.optInt("frequency_interval", 1),
    startDate = json.getString("start_date").dateOnly(),
    endDate = json.optNullableString("end_date")?.dateOnly(),
    dayOfWeek = json.optIntOrNull("day_of_week"),
    dayOfMonth = json.optIntOrNull("day_of_month"),
    timezone = json.optString("timezone"),
    status = json.getString("status"),
    nextOccurrence = json.optNullableString("next_occurrence")?.dateOnly(),
)

internal fun InvestmentScheduleRequest.toJson(): JSONObject = JSONObject()
    .put("asset_type", assetType)
    .put("symbol", symbol.trim().uppercase())
    .put("asset_name", assetName.trim())
    .put("broker", broker)
    .put("amount", amount)
    .put("currency", currency)
    .put("frequency", frequency)
    .put("frequency_interval", frequencyInterval)
    .put("start_date", startDate)
    .putOptional("end_date", endDate)
    .putOptional("day_of_week", dayOfWeek)
    .putOptional("day_of_month", dayOfMonth)
    .put("timezone", timezone)

internal fun <T> String.jsonArray(transform: (JSONObject) -> T): List<T> {
    val array = JSONArray(this)
    return List(array.length()) { transform(array.getJSONObject(it)) }
}

internal fun JSONObject.optNullableString(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf(String::isNotBlank)

internal fun JSONObject.optIntOrNull(key: String): Int? =
    if (!has(key) || isNull(key)) null else getInt(key)

internal fun JSONObject.putOptional(key: String, value: Any?): JSONObject = apply {
    if (value != null) put(key, value)
}

internal fun String.dateOnly(): String = take(10)

internal fun String.queryEncoded(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

internal fun String.errorMessage(): String =
    runCatching { JSONObject(this).optString("error") }
        .getOrDefault(this)
        .ifBlank { this }
