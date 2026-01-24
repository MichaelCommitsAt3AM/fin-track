package com.example.fintrack.core.di

import com.example.fintrack.core.data.repository.SmsTransactionRepository
import com.example.fintrack.core.domain.repository.ExternalTransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for the personal build flavor.
 * 
 * Binds the personal-specific SMS implementation of ExternalTransactionRepository.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FlavorTransactionModule {
    
    @Binds
    @Singleton
    abstract fun bindExternalTransactionRepository(
        smsTransactionRepository: SmsTransactionRepository
    ): ExternalTransactionRepository

    @Binds
    @Singleton
    abstract fun bindAppFlavorIntegration(
        impl: PersonalFlavorIntegrationImpl
    ): AppFlavorIntegration
}
