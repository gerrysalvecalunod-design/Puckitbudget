package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PucketDao {

    // --- Transactions ---
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    // --- Budgets ---
    @Query("SELECT * FROM budgets WHERE id = 1 LIMIT 1")
    fun observeBudget(): Flow<BudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets")
    suspend fun clearBudget()

    // --- Category Limits ---
    @Query("SELECT * FROM category_limits")
    fun observeCategoryLimits(): Flow<List<CategoryLimitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategoryLimit(limit: CategoryLimitEntity)

    @Query("DELETE FROM category_limits WHERE category = :category")
    suspend fun deleteCategoryLimit(category: String)

    @Query("DELETE FROM category_limits")
    suspend fun clearCategoryLimits()

    // --- Savings Goals ---
    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC")
    fun observeSavingsGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
    suspend fun getSavingsGoalById(id: Long): SavingsGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoalEntity): Long

    @Update
    suspend fun updateSavingsGoal(goal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteSavingsGoalById(id: Long)

    @Query("DELETE FROM savings_goals")
    suspend fun clearSavingsGoals()

    // --- Savings Contributions ---
    @Query("SELECT * FROM savings_contributions ORDER BY timestamp DESC")
    fun observeAllContributions(): Flow<List<SavingsContributionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: SavingsContributionEntity)

    @Transaction
    suspend fun addSavingsContribution(goalId: Long, amount: Double, note: String) {
        val goal = getSavingsGoalById(goalId) ?: return
        insertContribution(
            SavingsContributionEntity(
                goalId = goalId,
                amount = amount,
                note = note,
                timestamp = System.currentTimeMillis()
            )
        )
        updateSavingsGoal(
            goal.copy(currentAmount = (goal.currentAmount + amount).coerceAtLeast(0.0))
        )
    }
}
