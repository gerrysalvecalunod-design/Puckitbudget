package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "EXPENSE" or "INCOME"
    val amount: Double,
    val category: String,
    val description: String,
    val timestamp: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: Int = 1,
    val dailyLimit: Double = 0.0,
    val weeklyLimit: Double = 0.0,
    val monthlyLimit: Double = 0.0,
    val warningThresholdPercent: Int = 80,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "category_limits")
data class CategoryLimitEntity(
    @PrimaryKey val category: String,
    val monthlyLimit: Double
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDateMillis: Long? = null,
    val colorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "savings_contributions",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["goalId"])]
)
data class SavingsContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val amount: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
