package com.fintrack.app.core.di

import com.fintrack.app.presentation.navigation.SettingsIntegration
import com.fintrack.app.presentation.settings.StoreSettingsIntegrationImpl
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
    ): com.fintrack.app.presentation.navigation.SettingsIntegration
    
    @Binds
    @Singleton
    abstract fun bindOnboardingIntegration(
        impl: com.fintrack.app.presentation.onboarding.StoreOnboardingIntegrationImpl
    ): com.fintrack.app.presentation.navigation.OnboardingIntegration
}
