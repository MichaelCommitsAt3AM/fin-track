package com.example.fintrack.core.di

import com.example.fintrack.core.data.repository.StoreTransactionRepository
import com.example.fintrack.core.domain.repository.ExternalTransactionRepository
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
    abstract fun bindSettingsIntegration(
        impl: com.example.fintrack.presentation.settings.StoreSettingsIntegrationImpl
    ): com.example.fintrack.presentation.navigation.SettingsIntegration
}
