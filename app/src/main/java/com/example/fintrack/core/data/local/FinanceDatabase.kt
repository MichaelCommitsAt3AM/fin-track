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
        NotificationEntity::class
    ],
    version = 1 // Production baseline with all entities
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



    // This is often used for creating a Singleton instance
    companion object {
        const val DATABASE_NAME = "finance_db"
    }
}