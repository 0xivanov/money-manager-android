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

class ApiException(val status: Int, message: String) : IOException(message)

interface MoneyManagerApi {
    fun checkHealth(): Boolean
    fun register(email: String, password: String): AuthResult
    fun login(email: String, password: String): AuthResult
    fun getSummary(token: String, month: String): TransactionSummary
    fun getTransactions(token: String, month: String, type: String?, category: String?): List<Transaction>
    fun getCategories(token: String, type: String): List<Category>
    fun createCategory(token: String, type: String, name: String): Category
    fun deleteCategory(token: String, id: Int)
    fun exportTransactionsCsv(token: String, from: String, to: String): String
    fun importRevolutCsv(token: String, contents: ByteArray): ImportResult
    fun createTransaction(token: String, transaction: TransactionRequest): Transaction
    fun updateTransaction(token: String, id: Int, transaction: TransactionRequest): Transaction
    fun deleteTransaction(token: String, id: Int)
    fun deleteAccount(token: String)
}

interface GrowthApi {
    fun getSchedules(token: String): List<TransactionSchedule>
    fun createSchedule(token: String, schedule: TransactionScheduleRequest): TransactionSchedule
    fun pauseSchedule(token: String, id: Int): TransactionSchedule
    fun resumeSchedule(token: String, id: Int): TransactionSchedule
    fun deleteSchedule(token: String, id: Int)
    fun getBudgets(token: String): List<Budget>
    fun createBudget(token: String, budget: BudgetRequest): Budget
    fun deleteBudget(token: String, id: Int)
    fun getNotificationPreferences(token: String): NotificationPreferences
    fun updateNotificationPreferences(token: String, preferences: NotificationPreferences): NotificationPreferences
    fun registerPushDevice(token: String, deviceToken: String): Int
    fun deletePushDevice(token: String, id: Int)
    fun getInvestmentPortfolio(token: String): InvestmentPortfolio
    fun getInvestmentTrades(token: String): List<InvestmentTrade>
    fun createInvestmentTrade(token: String, trade: InvestmentTradeRequest): InvestmentTrade
    fun deleteInvestmentTrade(token: String, id: Int)
    fun setInvestmentPrice(token: String, price: InvestmentPriceRequest)
    fun exportInvestmentsCsv(token: String, from: String, to: String): String
    fun getInvestmentSchedules(token: String): List<InvestmentSchedule>
    fun createInvestmentSchedule(token: String, schedule: InvestmentScheduleRequest): InvestmentSchedule
    fun pauseInvestmentSchedule(token: String, id: Int): InvestmentSchedule
    fun resumeInvestmentSchedule(token: String, id: Int): InvestmentSchedule
    fun deleteInvestmentSchedule(token: String, id: Int)
}

class ApiClient(private val baseUrl: String) : MoneyManagerApi, GrowthApi {
    override fun checkHealth(): Boolean = request("GET", "/health").trim() == "ok"

    override fun register(email: String, password: String): AuthResult =
        auth("/auth/register", email, password)

    override fun login(email: String, password: String): AuthResult =
        auth("/auth/login", email, password)

    override fun getSummary(token: String, month: String): TransactionSummary {
        val json = request("GET", "/transactions/summary?month=${month.queryEncoded()}", token = token)
        return parseSummary(JSONObject(json))
    }

    override fun getTransactions(
        token: String,
        month: String,
        type: String?,
        category: String?,
    ): List<Transaction> {
        val query = buildList {
            add("month=${month.queryEncoded()}")
            if (!type.isNullOrBlank()) add("type=${type.queryEncoded()}")
            if (!category.isNullOrBlank()) add("category=${category.queryEncoded()}")
        }.joinToString("&")
        val json = request("GET", "/transactions?$query", token = token)
        val array = JSONArray(json)
        return List(array.length()) { index -> parseTransaction(array.getJSONObject(index)) }
    }

    override fun getCategories(token: String, type: String): List<Category> {
        val json = request("GET", "/categories?type=${type.queryEncoded()}", token = token)
        val array = JSONArray(json)
        return List(array.length()) { index -> parseCategory(array.getJSONObject(index)) }
    }

    override fun createCategory(token: String, type: String, name: String): Category {
        val body = JSONObject()
            .put("type", type)
            .put("name", name)
            .toString()
        val json = request("POST", "/categories", token = token, body = body)
        return parseCategory(JSONObject(json))
    }

    override fun deleteCategory(token: String, id: Int) {
        request("DELETE", "/categories/$id", token = token)
    }

    override fun exportTransactionsCsv(token: String, from: String, to: String): String =
        request(
            method = "GET",
            path = "/transactions/export?from=${from.queryEncoded()}&to=${to.queryEncoded()}",
            token = token,
        )

    override fun importRevolutCsv(token: String, contents: ByteArray): ImportResult {
        val json = requestBytes("POST", "/transactions/import/revolut", token, contents, "text/csv")
        val result = JSONObject(json)
        return ImportResult(result.getInt("imported"), result.getInt("skipped"), result.getInt("ignored"))
    }

    override fun createTransaction(token: String, transaction: TransactionRequest): Transaction {
        val json = request(
            method = "POST",
            path = "/transactions",
            token = token,
            body = transaction.toJson().toString(),
        )
        return parseTransaction(JSONObject(json))
    }

    override fun updateTransaction(token: String, id: Int, transaction: TransactionRequest): Transaction {
        val json = request(
            method = "PUT",
            path = "/transactions/$id",
            token = token,
            body = transaction.toJson().toString(),
        )
        return parseTransaction(JSONObject(json))
    }

    override fun deleteTransaction(token: String, id: Int) {
        request("DELETE", "/transactions/$id", token = token)
    }

    override fun deleteAccount(token: String) {
        request("DELETE", "/me", token = token)
    }

    override fun getSchedules(token: String): List<TransactionSchedule> =
        request("GET", "/schedules", token = token).jsonArray(::parseSchedule)

    override fun createSchedule(token: String, schedule: TransactionScheduleRequest): TransactionSchedule =
        parseSchedule(JSONObject(request("POST", "/schedules", token, schedule.toJson().toString())))

    override fun pauseSchedule(token: String, id: Int): TransactionSchedule =
        parseSchedule(JSONObject(request("POST", "/schedules/$id/pause", token)))

    override fun resumeSchedule(token: String, id: Int): TransactionSchedule =
        parseSchedule(JSONObject(request("POST", "/schedules/$id/resume", token)))

    override fun deleteSchedule(token: String, id: Int) {
        request("DELETE", "/schedules/$id", token)
    }

    override fun getBudgets(token: String): List<Budget> =
        request("GET", "/budgets?include_inactive=true", token = token).jsonArray(::parseBudget)

    override fun createBudget(token: String, budget: BudgetRequest): Budget =
        parseBudget(JSONObject(request("POST", "/budgets", token, budget.toJson().toString())))

    override fun deleteBudget(token: String, id: Int) {
        request("DELETE", "/budgets/$id", token)
    }

    override fun getNotificationPreferences(token: String): NotificationPreferences =
        parseNotificationPreferences(JSONObject(request("GET", "/notification-preferences", token)))

    override fun updateNotificationPreferences(
        token: String,
        preferences: NotificationPreferences,
    ): NotificationPreferences = parseNotificationPreferences(
        JSONObject(request("PUT", "/notification-preferences", token, preferences.toJson().toString())),
    )

    override fun registerPushDevice(token: String, deviceToken: String): Int {
        val body = JSONObject()
            .put("platform", "android")
            .put("device_token", deviceToken)
            .put("app_id", "org.moneymanager")
            .put("environment", if (org.moneymanager.BuildConfig.DEBUG) "sandbox" else "production")
        return JSONObject(request("POST", "/push-devices", token, body.toString())).getInt("id")
    }

    override fun deletePushDevice(token: String, id: Int) {
        request("DELETE", "/push-devices/$id", token)
    }

    override fun getInvestmentPortfolio(token: String): InvestmentPortfolio =
        parsePortfolio(JSONObject(request("GET", "/investments/portfolio", token)))

    override fun getInvestmentTrades(token: String): List<InvestmentTrade> =
        request("GET", "/investments/trades", token = token).jsonArray(::parseTrade)

    override fun createInvestmentTrade(token: String, trade: InvestmentTradeRequest): InvestmentTrade =
        parseTrade(JSONObject(request("POST", "/investments/trades", token, trade.toJson().toString())))

    override fun deleteInvestmentTrade(token: String, id: Int) {
        request("DELETE", "/investments/trades/$id", token)
    }

    override fun setInvestmentPrice(token: String, price: InvestmentPriceRequest) {
        request("PUT", "/investments/prices", token, price.toJson().toString())
    }

    override fun exportInvestmentsCsv(token: String, from: String, to: String): String =
        request(
            "GET",
            "/investments/export?from=${from.queryEncoded()}&to=${to.queryEncoded()}",
            token,
        )

    override fun getInvestmentSchedules(token: String): List<InvestmentSchedule> =
        request("GET", "/investment-schedules", token = token).jsonArray(::parseInvestmentSchedule)

    override fun createInvestmentSchedule(
        token: String,
        schedule: InvestmentScheduleRequest,
    ): InvestmentSchedule = parseInvestmentSchedule(
        JSONObject(request("POST", "/investment-schedules", token, schedule.toJson().toString())),
    )

    override fun pauseInvestmentSchedule(token: String, id: Int): InvestmentSchedule =
        parseInvestmentSchedule(JSONObject(request("POST", "/investment-schedules/$id/pause", token)))

    override fun resumeInvestmentSchedule(token: String, id: Int): InvestmentSchedule =
        parseInvestmentSchedule(JSONObject(request("POST", "/investment-schedules/$id/resume", token)))

    override fun deleteInvestmentSchedule(token: String, id: Int) {
        request("DELETE", "/investment-schedules/$id", token)
    }

    private fun auth(path: String, email: String, password: String): AuthResult {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .toString()
        val json = request("POST", path, body = body)
        val root = JSONObject(json)
        val user = root.getJSONObject("user")
        return AuthResult(
            token = root.getString("token"),
            user = User(id = user.getInt("id"), email = user.getString("email")),
        )
    }

    private fun request(
        method: String,
        path: String,
        token: String? = null,
        body: String? = null,
    ): String {
        val connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/json")
            if (token != null) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray()) }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw ApiException(status, response.errorMessage().ifBlank { "Request failed with HTTP $status" })
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun parseTransaction(json: JSONObject): Transaction =
        Transaction(
            id = json.getInt("id"),
            type = json.getString("type"),
            category = json.getString("category"),
            description = json.optString("description"),
            amount = json.getString("amount").also(String::toBigDecimal),
            currency = json.getString("currency"),
            occurredAt = json.getString("occurred_at").dateOnly(),
            source = json.optString("source", "manual"),
            status = json.optString("status", "posted"),
            excludedFromBudget = json.optBoolean("excluded_from_budget", false),
            scheduleOccurrenceId = json.optIntOrNull("schedule_occurrence_id"),
        )

    private fun requestBytes(
        method: String,
        path: String,
        token: String,
        body: ByteArray,
        contentType: String,
    ): String {
        val connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 30_000
            connection.doOutput = true
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", contentType)
            connection.outputStream.use { it.write(body) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw ApiException(status, response.errorMessage().ifBlank { "Request failed with HTTP $status" })
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun parseSummary(json: JSONObject): TransactionSummary =
        TransactionSummary(
            month = json.getString("month"),
            income = json.getString("income").also(String::toBigDecimal),
            expense = json.getString("expense").also(String::toBigDecimal),
            balance = json.getString("balance").also(String::toBigDecimal),
            currency = json.getString("currency"),
            transactionCount = json.getInt("transaction_count"),
        )

    private fun parseCategory(json: JSONObject): Category =
        Category(
            id = json.getInt("id"),
            type = json.getString("type"),
            name = json.getString("name"),
            isDefault = json.getBoolean("is_default"),
        )

    private fun TransactionRequest.toJson(): JSONObject =
        JSONObject()
            .put("type", type)
            .put("category", category)
            .put("description", description.trim())
            .put("amount", amount)
            .put("currency", currency)
            .put("occurred_at", occurredAt.dateOnly())
            .put("excluded_from_budget", excludedFromBudget)

    private fun parseSchedule(json: JSONObject): TransactionSchedule = TransactionSchedule(
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

    private fun TransactionScheduleRequest.toJson(): JSONObject = JSONObject()
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

    private fun parseBudget(json: JSONObject): Budget = Budget(
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

    private fun BudgetRequest.toJson(): JSONObject = JSONObject()
        .put("name", name.trim())
        .putOptional("category", category)
        .put("amount", amount)
        .put("currency", currency)
        .put("period", period)
        .put("warning_threshold", warningThreshold)

    private fun parseNotificationPreferences(json: JSONObject): NotificationPreferences = NotificationPreferences(
        bankSpending = json.optBoolean("bank_spending", true),
        budgetAlerts = json.optBoolean("budget_alerts", true),
        scheduledMoney = json.optBoolean("scheduled_money", true),
        investmentReminders = json.optBoolean("investment_reminders", true),
        quietHoursStart = json.optNullableString("quiet_hours_start"),
        quietHoursEnd = json.optNullableString("quiet_hours_end"),
        timezone = json.optString("timezone"),
    )

    private fun NotificationPreferences.toJson(): JSONObject = JSONObject()
        .put("bank_spending", bankSpending)
        .put("budget_alerts", budgetAlerts)
        .put("scheduled_money", scheduledMoney)
        .put("investment_reminders", investmentReminders)
        .putOptional("quiet_hours_start", quietHoursStart)
        .putOptional("quiet_hours_end", quietHoursEnd)
        .put("timezone", timezone)

    private fun parseTrade(json: JSONObject): InvestmentTrade = InvestmentTrade(
        id = json.getInt("id"),
        assetType = json.getString("asset_type"),
        symbol = json.getString("symbol"),
        assetName = json.getString("asset_name"),
        broker = json.getString("broker"),
        side = json.getString("side"),
        quantity = json.getString("quantity"),
        pricePerUnit = json.getString("price_per_unit"),
        fees = json.getString("fees"),
        currency = json.getString("currency"),
        occurredAt = json.getString("occurred_at").dateOnly(),
        notes = json.optString("notes"),
    )

    private fun InvestmentTradeRequest.toJson(): JSONObject = JSONObject()
        .put("asset_type", assetType)
        .put("symbol", symbol.trim().uppercase())
        .put("asset_name", assetName.trim())
        .put("broker", broker)
        .put("side", side)
        .put("quantity", quantity)
        .put("price_per_unit", pricePerUnit)
        .put("fees", fees)
        .put("currency", currency)
        .put("occurred_at", occurredAt)
        .put("notes", notes.trim())

    private fun parsePortfolio(json: JSONObject): InvestmentPortfolio {
        val positions = json.optJSONArray("positions") ?: JSONArray()
        return InvestmentPortfolio(
            positions = List(positions.length()) { parsePosition(positions.getJSONObject(it)) },
            investedAmount = json.getString("invested_amount"),
            currentValue = json.optNullableString("current_value"),
            unrealizedProfit = json.optNullableString("unrealized_profit"),
            realizedProfit = json.getString("realized_profit"),
            currency = json.getString("currency"),
            missingPrices = json.optInt("missing_prices"),
        )
    }

    private fun parsePosition(json: JSONObject): InvestmentPosition = InvestmentPosition(
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
        priceStatus = json.getString("price_status"),
    )

    private fun InvestmentPriceRequest.toJson(): JSONObject = JSONObject()
        .put("asset_type", assetType)
        .put("symbol", symbol.trim().uppercase())
        .put("currency", currency)
        .put("price", price)
        .putOptional("as_of", asOf)

    private fun parseInvestmentSchedule(json: JSONObject): InvestmentSchedule = InvestmentSchedule(
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

    private fun InvestmentScheduleRequest.toJson(): JSONObject = JSONObject()
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

    private fun <T> String.jsonArray(transform: (JSONObject) -> T): List<T> {
        val array = JSONArray(this)
        return List(array.length()) { transform(array.getJSONObject(it)) }
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf(String::isNotBlank)

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (!has(key) || isNull(key)) null else getInt(key)

    private fun JSONObject.putOptional(key: String, value: Any?): JSONObject = apply {
        if (value != null) put(key, value)
    }

    private fun String.dateOnly(): String = take(10)

    private fun String.queryEncoded(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

    private fun String.errorMessage(): String =
        runCatching { JSONObject(this).optString("error") }
            .getOrDefault(this)
            .ifBlank { this }
}
