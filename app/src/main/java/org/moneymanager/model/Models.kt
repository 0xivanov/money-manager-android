package org.moneymanager.model

data class User(
    val id: Int,
    val email: String,
)

data class AuthResult(
    val token: String,
    val user: User,
)

data class Transaction(
    val id: Int,
    val type: String,
    val category: String,
    val description: String,
    val amount: String,
    val currency: String,
    val occurredAt: String,
    val source: String = "manual",
    val status: String = "posted",
    val excludedFromBudget: Boolean = false,
    val scheduleOccurrenceId: Int? = null,
)

data class TransactionRequest(
    val type: String,
    val category: String,
    val description: String = "",
    val amount: String,
    val currency: String = "EUR",
    val occurredAt: String,
    val excludedFromBudget: Boolean = false,
)

data class Category(
    val id: Int,
    val type: String,
    val name: String,
    val isDefault: Boolean,
)

data class TransactionSummary(
    val month: String,
    val income: String,
    val expense: String,
    val balance: String,
    val currency: String,
    val transactionCount: Int,
)

data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val ignored: Int,
)

val expenseCategories = listOf(
    "groceries",
    "dining_out",
    "going_out",
    "transport",
    "housing",
    "utilities",
    "health",
    "entertainment",
    "shopping",
    "travel",
    "education",
    "beauty",
    "other",
)

val incomeCategories = listOf("salary", "freelance", "gift", "investment", "refund", "other")
