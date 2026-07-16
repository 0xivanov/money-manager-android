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
        parsePortfolio(JSONObject(request("GET", "/investments/portfolio", token, readTimeoutMillis = 30_000)))

    override fun getInvestmentPortfolioHistory(token: String, range: String): InvestmentPortfolioHistory =
        parsePortfolioHistory(
            JSONObject(
                request(
                    "GET",
                    "/investments/portfolio/history?range=${range.queryEncoded()}",
                    token,
                    readTimeoutMillis = 30_000,
                ),
            ),
        )

    override fun getInvestmentTrades(token: String): List<InvestmentTrade> =
        request("GET", "/investments/trades", token = token).jsonArray(::parseTrade)

    override fun createInvestmentTrade(token: String, trade: InvestmentTradeRequest): InvestmentTrade =
        parseTrade(
            JSONObject(
                request(
                    "POST",
                    "/investments/trades",
                    token,
                    trade.toJson().toString(),
                    readTimeoutMillis = 30_000,
                ),
            ),
        )

    override fun deleteInvestmentTrade(token: String, id: Int) {
        request("DELETE", "/investments/trades/$id", token)
    }

    override fun setInvestmentPrice(token: String, price: InvestmentPriceRequest) {
        request("PUT", "/investments/prices", token, price.toJson().toString())
    }

    override fun exportInvestmentsCsv(token: String, from: String, to: String): String =
        request(
            "GET",
            "/investments/export?from=${from.queryEncoded()}&through=${to.queryEncoded()}",
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
        readTimeoutMillis: Int = 10_000,
    ): String {
        val connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = readTimeoutMillis
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

}
