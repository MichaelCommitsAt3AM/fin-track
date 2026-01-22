package com.example.fintrack.core.di

import com.example.fintrack.presentation.navigation.SettingsIntegration
import com.example.fintrack.presentation.settings.mpesa.MpesaSettingsIntegrationImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Settings integration in the personal build flavor.
 * Binds the M-Pesa-specific implementation of SettingsIntegration.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {
    
    @Binds
    @Singleton
    abstract fun bindSettingsIntegration(
        impl: MpesaSettingsIntegrationImpl
    ): SettingsIntegration
}
