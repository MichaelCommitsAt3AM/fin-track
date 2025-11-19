package com.example.fintrack.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.fintrack.core.data.local.dao.BudgetDao
import com.example.fintrack.core.data.local.dao.CategoryDao
import com.example.fintrack.core.data.local.dao.TransactionDao
import com.example.fintrack.core.data.local.model.BudgetEntity
import com.example.fintrack.core.data.local.model.CategoryEntity
import com.example.fintrack.core.data.local.model.TransactionEntity
import com.example.fintrack.data.local.model.RecurringTransactionEntity
import com.example.fintrack.data.local.dao.RecurringTransactionDao

// This annotation defines the database
@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class
    ],
    version = 3 // Increment this number if you change the schema
)
@TypeConverters(Converters::class) // We'll create this file next
abstract class FinanceDatabase : RoomDatabase() {

    // Room will auto-generate the code for these abstract functions
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao


    // This is often used for creating a Singleton instance
    companion object {
        const val DATABASE_NAME = "finance_db"
    }
}