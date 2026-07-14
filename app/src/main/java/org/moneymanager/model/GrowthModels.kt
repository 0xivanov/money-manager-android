package org.moneymanager.model

data class TransactionSchedule(
    val id: Int,
    val type: String,
    val name: String,
    val category: String,
    val description: String,
    val amount: String,
    val currency: String,
    val frequency: String,
    val frequencyInterval: Int,
    val startDate: String,
    val endDate: String?,
    val dayOfWeek: Int?,
    val dayOfMonth: Int?,
    val timezone: String,
    val autoPost: Boolean,
    val status: String,
    val nextOccurrenceDate: String?,
)

data class TransactionScheduleRequest(
    val type: String,
    val name: String,
    val category: String,
    val description: String = "",
    val amount: String,
    val currency: String = "EUR",
    val frequency: String,
    val frequencyInterval: Int = 1,
    val startDate: String,
    val endDate: String? = null,
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = null,
    val timezone: String,
    val autoPost: Boolean,
)

data class Budget(
    val id: Int,
    val name: String,
    val category: String?,
    val amount: String,
    val currency: String,
    val period: String,
    val warningThreshold: Int,
    val status: String,
    val periodStart: String,
    val periodEnd: String,
    val spentAmount: String,
    val remainingAmount: String,
    val progressPercent: String,
    val alertLevel: String,
)

data class BudgetRequest(
    val name: String,
    val category: String? = null,
    val amount: String,
    val currency: String = "EUR",
    val period: String,
    val warningThreshold: Int = 80,
)

data class NotificationPreferences(
    val bankSpending: Boolean,
    val budgetAlerts: Boolean,
    val scheduledMoney: Boolean,
    val investmentReminders: Boolean,
    val quietHoursStart: String?,
    val quietHoursEnd: String?,
    val timezone: String,
)

data class InvestmentTrade(
    val id: Int,
    val assetType: String,
    val symbol: String,
    val assetName: String,
    val broker: String,
    val side: String,
    val quantity: String,
    val pricePerUnit: String,
    val fees: String,
    val currency: String,
    val occurredAt: String,
    val notes: String,
)

data class InvestmentTradeRequest(
    val assetType: String,
    val symbol: String,
    val assetName: String,
    val broker: String,
    val side: String,
    val quantity: String,
    val pricePerUnit: String,
    val fees: String = "0",
    val currency: String = "EUR",
    val occurredAt: String,
    val notes: String = "",
)

data class InvestmentPosition(
    val assetType: String,
    val symbol: String,
    val assetName: String,
    val broker: String,
    val quantity: String,
    val averageCost: String,
    val investedAmount: String,
    val currentPrice: String?,
    val currentValue: String?,
    val unrealizedProfit: String?,
    val unrealizedPercent: String?,
    val realizedProfit: String,
    val currency: String,
    val priceAsOf: String?,
    val priceStatus: String,
)

data class InvestmentPortfolio(
    val positions: List<InvestmentPosition>,
    val investedAmount: String,
    val currentValue: String?,
    val unrealizedProfit: String?,
    val realizedProfit: String,
    val currency: String,
    val missingPrices: Int,
)

data class InvestmentPriceRequest(
    val assetType: String,
    val symbol: String,
    val currency: String = "EUR",
    val price: String,
    val asOf: String? = null,
)

data class InvestmentSchedule(
    val id: Int,
    val assetType: String,
    val symbol: String,
    val assetName: String,
    val broker: String,
    val amount: String,
    val currency: String,
    val frequency: String,
    val frequencyInterval: Int,
    val startDate: String,
    val endDate: String?,
    val dayOfWeek: Int?,
    val dayOfMonth: Int?,
    val timezone: String,
    val status: String,
    val nextOccurrence: String?,
)

data class InvestmentScheduleRequest(
    val assetType: String,
    val symbol: String,
    val assetName: String,
    val broker: String,
    val amount: String,
    val currency: String = "EUR",
    val frequency: String,
    val frequencyInterval: Int = 1,
    val startDate: String,
    val endDate: String? = null,
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = null,
    val timezone: String,
)
