package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TransactionEntity::class,
        BudgetEntity::class,
        CategoryLimitEntity::class,
        SavingsGoalEntity::class,
        SavingsContributionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PucketDatabase : RoomDatabase() {
    abstract fun pucketDao(): PucketDao

    companion object {
        @Volatile
        private var INSTANCE: PucketDatabase? = null

        fun getInstance(context: Context): PucketDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PucketDatabase::class.java,
                    "pucketbudget_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
