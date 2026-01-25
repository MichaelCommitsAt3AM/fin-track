package com.fintrack.app.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fintrack.app.core.data.local.dao.BudgetDao
import com.fintrack.app.core.data.local.dao.CategoryDao
import com.fintrack.app.core.data.local.dao.TransactionDao
import com.fintrack.app.core.data.local.dao.PaymentMethodDao
import com.fintrack.app.core.data.local.dao.MpesaCategoryMappingDao
import com.fintrack.app.core.data.local.model.BudgetEntity
import com.fintrack.app.core.data.local.model.CategoryEntity
import com.fintrack.app.core.data.local.model.TransactionEntity
import com.fintrack.app.core.data.local.model.PaymentMethodEntity
import com.fintrack.app.core.data.local.model.MpesaCategoryMappingEntity
import com.fintrack.app.core.data.local.model.RecurringTransactionEntity
import com.fintrack.app.core.data.local.dao.RecurringTransactionDao
import com.fintrack.app.core.data.local.model.UserEntity
import com.fintrack.app.core.data.local.dao.UserDao
import com.fintrack.app.core.data.local.model.SavingEntity
import com.fintrack.app.core.data.local.model.DebtEntity
import com.fintrack.app.core.data.local.model.ContributionEntity
import com.fintrack.app.core.data.local.model.PaymentEntity
import com.fintrack.app.core.data.local.dao.SavingDao
import com.fintrack.app.core.data.local.dao.DebtDao
import com.fintrack.app.core.data.local.dao.ContributionDao
import com.fintrack.app.core.data.local.dao.PaymentDao
import com.fintrack.app.core.data.local.entity.NotificationEntity
import com.fintrack.app.core.data.local.dao.NotificationDao

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
        PaymentMethodEntity::class,
        MpesaCategoryMappingEntity::class
    ],
    version = 2
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
    abstract fun mpesaCategoryMappingDao(): MpesaCategoryMappingDao



    // This is often used for creating a Singleton instance
    companion object {
        const val DATABASE_NAME = "finance_db"
    }
}