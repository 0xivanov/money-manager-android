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
    fun getInvestmentPortfolioHistory(token: String, range: String = "1y"): InvestmentPortfolioHistory
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
