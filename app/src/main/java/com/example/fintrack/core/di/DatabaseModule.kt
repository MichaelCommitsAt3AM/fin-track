package com.example.fintrack.core.di

import android.content.Context
import androidx.room.Room
import com.example.fintrack.core.data.local.FinanceDatabase
import com.example.fintrack.core.data.local.dao.BudgetDao
import com.example.fintrack.core.data.local.dao.CategoryDao
import com.example.fintrack.core.data.local.dao.TransactionDao
import com.example.fintrack.core.data.local.dao.UserDao
import com.example.fintrack.core.data.local.dao.SavingDao
import com.example.fintrack.core.data.local.dao.DebtDao
import com.example.fintrack.core.data.local.dao.ContributionDao
import com.example.fintrack.core.data.local.dao.PaymentDao
import com.example.fintrack.core.domain.repository.UserRepository
import com.example.fintrack.core.data.repository.UserRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // This module lives as long as the app does
object DatabaseModule {

    @Provides
    @Singleton // Ensures only one instance of the database is created
    fun provideFinanceDatabase(
        @ApplicationContext context: Context
    ): FinanceDatabase {
        return Room.databaseBuilder(
            context,
            FinanceDatabase::class.java,
            FinanceDatabase.DATABASE_NAME // "finance_db"
        )
            // In a real app, you'd add migration strategies here
            .fallbackToDestructiveMigration() // For now, just rebuild if schema changes
            .build()
    }

    // --- Provide all the DAOs ---

    @Provides
    @Singleton // DAOs are tied to the singleton database instance
    fun provideTransactionDao(database: FinanceDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: FinanceDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideBudgetDao(database: FinanceDatabase): BudgetDao {
        return database.budgetDao()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: FinanceDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideSavingDao(database: FinanceDatabase): SavingDao {
        return database.savingDao()
    }

    @Provides
    @Singleton
    fun provideDebtDao(database: FinanceDatabase): DebtDao {
        return database.debtDao()
    }

    @Provides
    @Singleton
    fun provideContributionDao(database: FinanceDatabase): ContributionDao {
        return database.contributionDao()
    }

    @Provides
    @Singleton
    fun providePaymentDao(database: FinanceDatabase): PaymentDao {
        return database.paymentDao()
    }

    @Provides
    @Singleton
    fun provideNotificationDao(database: FinanceDatabase) = database.notificationDao()

    @Provides
    @Singleton
    fun provideUserRepository(
        userDao: UserDao,
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): UserRepository {
        return UserRepositoryImpl(userDao, firebaseAuth, firestore)
    }
}