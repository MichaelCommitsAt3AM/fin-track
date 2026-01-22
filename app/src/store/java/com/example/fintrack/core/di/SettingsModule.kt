package com.example.fintrack.core.di

import com.example.fintrack.presentation.navigation.SettingsIntegration
import com.example.fintrack.presentation.settings.StoreSettingsIntegrationImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Settings integration in the store build flavor.
 * Binds the store-specific (empty) implementation of SettingsIntegration
 * and OnboardingIntegration.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {
    
    @Binds
    @Singleton
    abstract fun bindSettingsIntegration(
        impl: StoreSettingsIntegrationImpl
    ): com.example.fintrack.presentation.navigation.SettingsIntegration
    
    @Binds
    @Singleton
    abstract fun bindOnboardingIntegration(
        impl: com.example.fintrack.presentation.onboarding.StoreOnboardingIntegrationImpl
    ): com.example.fintrack.presentation.navigation.OnboardingIntegration
}
