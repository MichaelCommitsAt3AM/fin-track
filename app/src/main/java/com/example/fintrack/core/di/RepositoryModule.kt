package com.example.fintrack.core.di

import com.example.fintrack.core.data.repository.AuthRepositoryImpl
import com.example.fintrack.core.data.repository.BudgetRepositoryImpl
import com.example.fintrack.core.data.repository.CategoryRepositoryImpl
import com.example.fintrack.core.data.repository.NetworkRepositoryImpl
import com.example.fintrack.core.data.repository.TransactionRepositoryImpl
import com.example.fintrack.core.data.repository.SavingRepositoryImpl
import com.example.fintrack.core.data.repository.DebtRepositoryImpl
import com.example.fintrack.core.data.repository.NotificationRepositoryImpl
import com.example.fintrack.core.domain.repository.AuthRepository
import com.example.fintrack.core.domain.repository.BudgetRepository
import com.example.fintrack.core.domain.repository.CategoryRepository
import com.example.fintrack.core.domain.repository.NetworkRepository
import com.example.fintrack.core.domain.repository.TransactionRepository
import com.example.fintrack.core.domain.repository.SavingRepository
import com.example.fintrack.core.domain.repository.DebtRepository
import com.example.fintrack.core.domain.repository.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        // Create this class first, following the pattern from the others
        budgetRepositoryImpl: BudgetRepositoryImpl
    ): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindNetworkRepository(
        networkRepositoryImpl: NetworkRepositoryImpl
    ): NetworkRepository

    @Binds
    @Singleton
    abstract fun bindSavingRepository(
        savingRepositoryImpl: SavingRepositoryImpl
    ): SavingRepository

    @Binds
    @Singleton
    abstract fun bindDebtRepository(
        debtRepositoryImpl: DebtRepositoryImpl
    ): DebtRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: NotificationRepositoryImpl
    ): NotificationRepository
}