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
)

data class TransactionRequest(
    val type: String,
    val category: String,
    val description: String = "",
    val amount: String,
    val currency: String = "EUR",
    val occurredAt: String,
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
    "food",
    "transport",
    "housing",
    "utilities",
    "health",
    "entertainment",
    "shopping",
    "travel",
    "education",
    "other",
)

val incomeCategories = listOf("salary", "freelance", "gift", "investment", "refund", "other")
