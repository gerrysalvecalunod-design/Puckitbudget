package com.example.data.model

enum class TransactionType(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income")
}

enum class ExpenseCategory(val label: String, val colorHex: Long) {
    FOOD("Food", 0xFFE67E22),
    TRANSPORTATION("Transportation", 0xFF2980B9),
    EDUCATION("Education", 0xFF8E44AD),
    BILLS("Bills", 0xFFC0392B),
    SHOPPING("Shopping", 0xFFD81B60),
    ENTERTAINMENT("Entertainment", 0xFF00897B),
    HEALTH("Health", 0xFF27AE60),
    OTHER("Other", 0xFF5D6D7E);

    companion object {
        fun fromNameOrLabel(value: String): ExpenseCategory {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true)
            } ?: OTHER
        }
    }
}

enum class IncomeCategory(val label: String, val colorHex: Long) {
    SALARY("Salary", 0xFF0E6B4E),
    FREELANCE("Freelance", 0xFF16A085),
    BUSINESS("Business", 0xFF2E86C1),
    ALLOWANCE("Allowance", 0xFFD4AC0D),
    INVESTMENT("Investment", 0xFF7D3C98),
    GIFT("Gift", 0xFFE74C3C),
    OTHER("Other", 0xFF566573);

    companion object {
        fun fromNameOrLabel(value: String): IncomeCategory {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true)
            } ?: OTHER
        }
    }
}

enum class BudgetPeriod(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

enum class SortOrder(val label: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    AMOUNT_DESC("Highest Amount"),
    AMOUNT_ASC("Lowest Amount")
}

enum class DateFilter(val label: String) {
    ALL("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    CUSTOM("Specific Date")
}

enum class ThemePreference(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode");

    companion object {
        fun fromName(name: String): ThemePreference =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

enum class CurrencyOption(val code: String, val symbol: String, val displayName: String) {
    USD("USD", "$", "US Dollar ($)"),
    PHP("PHP", "₱", "Philippine Peso (₱)"),
    EUR("EUR", "€", "Euro (€)"),
    GBP("GBP", "£", "British Pound (£)"),
    JPY("JPY", "¥", "Japanese Yen (¥)"),
    CAD("CAD", "C$", "Canadian Dollar (C$)"),
    AUD("AUD", "A$", "Australian Dollar (A$)"),
    SGD("SGD", "S$", "Singapore Dollar (S$)");

    companion object {
        fun fromCode(code: String): CurrencyOption =
            entries.firstOrNull { it.code == code } ?: USD
    }
}

data class BudgetProgress(
    val period: BudgetPeriod,
    val limit: Double,
    val spent: Double
) {
    val remaining: Double get() = limit - spent
    val ratio: Float get() = if (limit > 0.0) (spent / limit).toFloat() else 0f
    val percentUsed: Int get() = if (limit > 0.0) ((spent / limit) * 100).toInt() else 0
}

data class BudgetWarning(
    val id: String,
    val title: String,
    val message: String,
    val isExceeded: Boolean
)

data class CategoryBreakdown(
    val category: ExpenseCategory,
    val amount: Double,
    val percentage: Float
)

data class PeriodChartPoint(
    val label: String,
    val expense: Double,
    val income: Double = 0.0
)

data class ReportsData(
    val dailyPoints: List<PeriodChartPoint> = emptyList(),
    val weeklyPoints: List<PeriodChartPoint> = emptyList(),
    val monthlyPoints: List<PeriodChartPoint> = emptyList(),
    val dailyTotal: Double = 0.0,
    val dailyAverage: Double = 0.0,
    val weeklyTotal: Double = 0.0,
    val weeklyAverage: Double = 0.0,
    val monthlyTotal: Double = 0.0,
    val monthlyAverage: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netSavings: Double = 0.0,
    val savingsRatePercent: Int = 0,
    val categoryBreakdowns: List<CategoryBreakdown> = emptyList(),
    val totalSavingsGoalsCount: Int = 0,
    val completedSavingsGoalsCount: Int = 0,
    val totalSavedAmount: Double = 0.0,
    val totalSavingsTarget: Double = 0.0
)
