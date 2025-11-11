package com.example.fintrack.di

import android.content.Context
import androidx.room.Room
import com.example.fintrack.data.local.FinanceDatabase
import com.example.fintrack.data.local.dao.BudgetDao
import com.example.fintrack.data.local.dao.CategoryDao
import com.example.fintrack.data.local.dao.TransactionDao
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
}