package com.fintrack.app.core.di

import com.fintrack.app.core.data.repository.StoreTransactionRepository
import com.fintrack.app.core.domain.repository.ExternalTransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for the store build flavor.
 * 
 * Binds the store-specific no-op implementation of ExternalTransactionRepository.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FlavorTransactionModule {
    
    @Binds
    @Singleton
    abstract fun bindExternalTransactionRepository(
        storeTransactionRepository: StoreTransactionRepository
    ): ExternalTransactionRepository

    @Binds
    @Singleton
    abstract fun bindAppFlavorIntegration(
        impl: StoreFlavorIntegrationImpl
    ): AppFlavorIntegration
}
