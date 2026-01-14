package com.example.fintrack.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.fintrack.core.data.local.dao.BudgetDao
import com.example.fintrack.core.data.local.dao.CategoryDao
import com.example.fintrack.core.data.local.dao.TransactionDao
import com.example.fintrack.core.data.local.dao.PaymentMethodDao
import com.example.fintrack.core.data.local.model.BudgetEntity
import com.example.fintrack.core.data.local.model.CategoryEntity
import com.example.fintrack.core.data.local.model.TransactionEntity
import com.example.fintrack.core.data.local.model.PaymentMethodEntity
import com.example.fintrack.core.data.local.model.RecurringTransactionEntity
import com.example.fintrack.core.data.local.dao.RecurringTransactionDao
import com.example.fintrack.core.data.local.model.UserEntity
import com.example.fintrack.core.data.local.dao.UserDao
import com.example.fintrack.core.data.local.model.SavingEntity
import com.example.fintrack.core.data.local.model.DebtEntity
import com.example.fintrack.core.data.local.model.ContributionEntity
import com.example.fintrack.core.data.local.model.PaymentEntity
import com.example.fintrack.core.data.local.dao.SavingDao
import com.example.fintrack.core.data.local.dao.DebtDao
import com.example.fintrack.core.data.local.dao.ContributionDao
import com.example.fintrack.core.data.local.dao.PaymentDao
import com.example.fintrack.core.data.local.entity.NotificationEntity
import com.example.fintrack.core.data.local.dao.NotificationDao

// This annotation defines the database
@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        UserEntity::class,
        SavingEntity::class,
        DebtEntity::class,
        ContributionEntity::class,
        PaymentEntity::class,
        NotificationEntity::class,
        PaymentMethodEntity::class
    ],
    version = 5 // Added isPlanned field to TransactionEntity
)
@TypeConverters(Converters::class) // We'll create this file next
abstract class FinanceDatabase : RoomDatabase() {

    // Room will auto-generate the code for these abstract functions
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun userDao(): UserDao
    abstract fun savingDao(): SavingDao
    abstract fun debtDao(): DebtDao
    abstract fun contributionDao(): ContributionDao
    abstract fun paymentDao(): PaymentDao
    abstract fun notificationDao(): NotificationDao
    abstract fun paymentMethodDao(): PaymentMethodDao



    // This is often used for creating a Singleton instance
    companion object {
        const val DATABASE_NAME = "finance_db"
        
        // Migration from version 1 to 2: Add payment_methods table
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS payment_methods (
                        name TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        iconName TEXT NOT NULL,
                        colorHex TEXT NOT NULL,
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(name, userId)
                    )""".trimIndent()
                )
            }
        }
        
        // Migration from version 2 to 3: Add isActive column to payment_methods
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """ALTER TABLE payment_methods ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1"""
                )
            }
        }
        
        // Migration from version 3 to 4: Add isSynced column to transactions
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """ALTER TABLE transactions ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0"""
                )
            }
        }
        
        // Migration from version 4 to 5: Add isPlanned column to transactions
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Add the isPlanned column
                database.execSQL(
                    """ALTER TABLE transactions ADD COLUMN isPlanned INTEGER NOT NULL DEFAULT 0"""
                )
                
                // 2. Mark existing future transactions as planned
                val currentTime = System.currentTimeMillis()
                database.execSQL(
                    """UPDATE transactions SET isPlanned = 1 WHERE date > $currentTime"""
                )
            }
        }
    }
}