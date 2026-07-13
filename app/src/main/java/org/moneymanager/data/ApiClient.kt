package org.moneymanager.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject
import org.moneymanager.model.AuthResult
import org.moneymanager.model.Category
import org.moneymanager.model.ImportResult
import org.moneymanager.model.Transaction
import org.moneymanager.model.TransactionRequest
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

class ApiClient(private val baseUrl: String) : MoneyManagerApi {
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

    private fun String.dateOnly(): String = take(10)

    private fun String.queryEncoded(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

    private fun String.errorMessage(): String =
        runCatching { JSONObject(this).optString("error") }
            .getOrDefault(this)
            .ifBlank { this }
}
