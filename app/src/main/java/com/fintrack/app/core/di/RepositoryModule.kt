package com.fintrack.app.core.di

import com.fintrack.app.core.data.repository.AuthRepositoryImpl
import com.fintrack.app.core.data.repository.BudgetRepositoryImpl
import com.fintrack.app.core.data.repository.CategoryRepositoryImpl
import com.fintrack.app.core.data.repository.NetworkRepositoryImpl
import com.fintrack.app.core.data.repository.TransactionRepositoryImpl
import com.fintrack.app.core.data.repository.SavingRepositoryImpl
import com.fintrack.app.core.data.repository.DebtRepositoryImpl
import com.fintrack.app.core.data.repository.NotificationRepositoryImpl
import com.fintrack.app.core.data.repository.PaymentMethodRepositoryImpl
import com.fintrack.app.core.domain.repository.AuthRepository
import com.fintrack.app.core.domain.repository.BudgetRepository
import com.fintrack.app.core.domain.repository.CategoryRepository
import com.fintrack.app.core.domain.repository.NetworkRepository
import com.fintrack.app.core.domain.repository.TransactionRepository
import com.fintrack.app.core.domain.repository.SavingRepository
import com.fintrack.app.core.domain.repository.DebtRepository
import com.fintrack.app.core.domain.repository.NotificationRepository
import com.fintrack.app.core.domain.repository.PaymentMethodRepository
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

    @Binds
    @Singleton
    abstract fun bindPaymentMethodRepository(
        paymentMethodRepositoryImpl: PaymentMethodRepositoryImpl
    ): PaymentMethodRepository
}