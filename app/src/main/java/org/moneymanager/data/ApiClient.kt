package org.moneymanager.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject
import org.moneymanager.model.AuthResult
import org.moneymanager.model.Transaction
import org.moneymanager.model.TransactionRequest
import org.moneymanager.model.TransactionSummary
import org.moneymanager.model.User

class ApiClient(private val baseUrl: String) {
    fun register(email: String, password: String): AuthResult =
        auth("/auth/register", email, password)

    fun login(email: String, password: String): AuthResult =
        auth("/auth/login", email, password)

    fun getSummary(token: String, month: String): TransactionSummary {
        val json = request("GET", "/transactions/summary?month=$month", token = token)
        return parseSummary(JSONObject(json))
    }

    fun getTransactions(
        token: String,
        month: String,
        type: String?,
        category: String?,
    ): List<Transaction> {
        val query = buildList {
            add("month=$month")
            if (!type.isNullOrBlank()) add("type=$type")
            if (!category.isNullOrBlank()) add("category=$category")
        }.joinToString("&")
        val json = request("GET", "/transactions?$query", token = token)
        val array = JSONArray(json)
        return List(array.length()) { index -> parseTransaction(array.getJSONObject(index)) }
    }

    fun createTransaction(token: String, transaction: TransactionRequest): Transaction {
        val json = request(
            method = "POST",
            path = "/transactions",
            token = token,
            body = transaction.toJson().toString(),
        )
        return parseTransaction(JSONObject(json))
    }

    fun updateTransaction(token: String, id: Int, transaction: TransactionRequest): Transaction {
        val json = request(
            method = "PUT",
            path = "/transactions/$id",
            token = token,
            body = transaction.toJson().toString(),
        )
        return parseTransaction(JSONObject(json))
    }

    fun deleteTransaction(token: String, id: Int) {
        request("DELETE", "/transactions/$id", token = token)
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
        connection.disconnect()

        if (status !in 200..299) {
            throw IOException(response.ifBlank { "Request failed with HTTP $status" })
        }
        return response
    }

    private fun parseTransaction(json: JSONObject): Transaction =
        Transaction(
            id = json.getInt("id"),
            type = json.getString("type"),
            category = json.getString("category"),
            amount = json.getString("amount"),
            currency = json.getString("currency"),
            occurredAt = json.getString("occurred_at").dateOnly(),
        )

    private fun parseSummary(json: JSONObject): TransactionSummary =
        TransactionSummary(
            month = json.getString("month"),
            income = json.getString("income"),
            expense = json.getString("expense"),
            balance = json.getString("balance"),
            currency = json.getString("currency"),
            transactionCount = json.getInt("transaction_count"),
        )

    private fun TransactionRequest.toJson(): JSONObject =
        JSONObject()
            .put("type", type)
            .put("category", category)
            .put("amount", amount)
            .put("currency", currency)
            .put("occurred_at", occurredAt.dateOnly())

    private fun String.dateOnly(): String = take(10)
}
