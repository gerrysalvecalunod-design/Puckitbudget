package com.example.data.repository

import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryLimitEntity
import com.example.data.local.PucketDao
import com.example.data.local.SavingsContributionEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.CurrencyOption
import com.example.data.model.ExpenseCategory
import com.example.data.model.IncomeCategory
import com.example.data.model.ThemePreference
import com.example.data.model.TransactionType
import com.example.data.preferences.SettingsDataStore
import com.example.data.preferences.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class BudgetRepository(
    private val dao: PucketDao,
    private val settingsDataStore: SettingsDataStore
) {
    val transactions: Flow<List<TransactionEntity>> = dao.observeAllTransactions()
    val budget: Flow<BudgetEntity> = dao.observeBudget().map { it ?: BudgetEntity() }
    val categoryLimits: Flow<List<CategoryLimitEntity>> = dao.observeCategoryLimits()
    val savingsGoals: Flow<List<SavingsGoalEntity>> = dao.observeSavingsGoals()
    val savingsContributions: Flow<List<SavingsContributionEntity>> = dao.observeAllContributions()
    val userSettings: Flow<UserSettings> = settingsDataStore.userSettings

    // --- Transactions ---
    suspend fun addTransaction(transaction: TransactionEntity): Long =
        dao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        dao.updateTransaction(transaction)

    suspend fun deleteTransaction(id: Long) =
        dao.deleteTransactionById(id)

    // --- Budgets ---
    suspend fun saveBudget(
        dailyLimit: Double,
        weeklyLimit: Double,
        monthlyLimit: Double,
        warningThresholdPercent: Int
    ) {
        dao.upsertBudget(
            BudgetEntity(
                id = 1,
                dailyLimit = dailyLimit.coerceAtLeast(0.0),
                weeklyLimit = weeklyLimit.coerceAtLeast(0.0),
                monthlyLimit = monthlyLimit.coerceAtLeast(0.0),
                warningThresholdPercent = warningThresholdPercent.coerceIn(50, 100),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveCategoryLimit(category: ExpenseCategory, monthlyLimit: Double) {
        if (monthlyLimit <= 0.0) {
            dao.deleteCategoryLimit(category.name)
        } else {
            dao.upsertCategoryLimit(CategoryLimitEntity(category.name, monthlyLimit))
        }
    }

    // --- Savings Goals ---
    suspend fun addSavingsGoal(
        title: String,
        targetAmount: Double,
        initialAmount: Double,
        targetDateMillis: Long?,
        colorIndex: Int
    ) {
        val goalId = dao.insertSavingsGoal(
            SavingsGoalEntity(
                title = title.trim(),
                targetAmount = targetAmount.coerceAtLeast(1.0),
                currentAmount = initialAmount.coerceAtLeast(0.0),
                targetDateMillis = targetDateMillis,
                colorIndex = colorIndex
            )
        )
        if (initialAmount > 0.0) {
            dao.insertContribution(
                SavingsContributionEntity(
                    goalId = goalId,
                    amount = initialAmount,
                    note = "Initial deposit"
                )
            )
        }
    }

    suspend fun updateSavingsGoal(goal: SavingsGoalEntity) =
        dao.updateSavingsGoal(goal)

    suspend fun deleteSavingsGoal(id: Long) =
        dao.deleteSavingsGoalById(id)

    suspend fun contributeToSavingsGoal(goalId: Long, amount: Double, note: String) {
        if (amount > 0.0) {
            dao.addSavingsContribution(goalId, amount, note.trim())
        }
    }

    // --- Settings ---
    suspend fun setCurrency(currency: CurrencyOption) = settingsDataStore.setCurrency(currency)
    suspend fun setThemePreference(theme: ThemePreference) = settingsDataStore.setThemePreference(theme)
    suspend fun setBudgetNotificationsEnabled(enabled: Boolean) =
        settingsDataStore.setBudgetNotificationsEnabled(enabled)

    // --- Data Utilities ---
    suspend fun clearAllData() {
        dao.clearAllTransactions()
        dao.clearBudget()
        dao.clearCategoryLimits()
        dao.clearSavingsGoals()
    }

    suspend fun loadSampleData() {
        val now = System.currentTimeMillis()
        fun daysAgo(days: Int): Long {
            return Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -days)
            }.timeInMillis
        }

        dao.clearAllTransactions()
        dao.clearSavingsGoals()
        dao.clearCategoryLimits()

        dao.upsertBudget(
            BudgetEntity(
                id = 1,
                dailyLimit = 80.0,
                weeklyLimit = 450.0,
                monthlyLimit = 1800.0,
                warningThresholdPercent = 80,
                updatedAt = now
            )
        )

        dao.upsertCategoryLimit(CategoryLimitEntity(ExpenseCategory.FOOD.name, 500.0))
        dao.upsertCategoryLimit(CategoryLimitEntity(ExpenseCategory.BILLS.name, 400.0))
        dao.upsertCategoryLimit(CategoryLimitEntity(ExpenseCategory.TRANSPORTATION.name, 200.0))

        val sampleTransactions = listOf(
            TransactionEntity(type = TransactionType.INCOME.name, amount = 2400.0, category = IncomeCategory.SALARY.name, description = "Monthly Salary Deposit", timestamp = daysAgo(12)),
            TransactionEntity(type = TransactionType.INCOME.name, amount = 420.0, category = IncomeCategory.FREELANCE.name, description = "UI Design Consultation", timestamp = daysAgo(5)),
            TransactionEntity(type = TransactionType.EXPENSE.name, amount = 185.0, category = ExpenseCategory.BILLS.name, description = "Electricity & Internet Bill", timestamp = daysAgo(10)),
            TransactionEntity(type = TransactionType.EXPENSE.name, amount = 68.50, category = ExpenseCategory.FOOD.name, description = "Weekly Fresh Groceries", timestamp = daysAgo(6)),
            TransactionEntity(type = TransactionType.EXPENSE.name, amount = 34.0, category = ExpenseCategory.TRANSPORTATION.name, description = "Fuel & Transit Pass", timestamp = daysAgo(4)),
            TransactionEntity(type = TransactionType.EXPENSE.name, amount = 95.0, category = ExpenseCategory.EDUCATION.name, description = "Online Course & Textbooks", timestamp = daysAgo(3)),
            TransactionEntity(type = TransactionType.EXPENSE.name, amount = 54.25, category = ExpenseCategory.SHOPPING.name, description = "Household Essentials", timestamp = daysAgo(2)),
            TransactionEntity(type = TransactionType.EXPENSE.name, amount = 42.0, category = ExpenseCategory.HEALTH.name, description = "Pharmacy & Vitamins", timestamp = daysAgo(1)),
            TransactionEntity(type = TransactionType.EXPENSE.name, amount = 28.0, category = ExpenseCategory.ENTERTAINMENT.name, description = "Movie Night & Snacks", timestamp = now),
            TransactionEntity(type = TransactionType.EXPENSE.name, amount = 19.75, category = ExpenseCategory.FOOD.name, description = "Lunch Bento & Coffee", timestamp = now)
        )
        dao.insertTransactions(sampleTransactions)

        val emergencyId = dao.insertSavingsGoal(
            SavingsGoalEntity(
                title = "Emergency Fund",
                targetAmount = 3000.0,
                currentAmount = 1450.0,
                targetDateMillis = Calendar.getInstance().apply { add(Calendar.MONTH, 6) }.timeInMillis,
                colorIndex = 0
            )
        )
        dao.insertContribution(SavingsContributionEntity(goalId = emergencyId, amount = 1450.0, note = "Initial reserve"))

        val laptopId = dao.insertSavingsGoal(
            SavingsGoalEntity(
                title = "New Workstation Laptop",
                targetAmount = 1200.0,
                currentAmount = 780.0,
                targetDateMillis = Calendar.getInstance().apply { add(Calendar.MONTH, 3) }.timeInMillis,
                colorIndex = 1
            )
        )
        dao.insertContribution(SavingsContributionEntity(goalId = laptopId, amount = 780.0, note = "Freelance savings"))
    }
}
