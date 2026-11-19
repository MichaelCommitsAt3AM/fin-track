package com.fintrack.app.core.di

import com.fintrack.app.core.data.local.MpesaDatabase
import com.fintrack.app.core.data.preferences.MpesaOnboardingPreferences
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Clears the on-device M-Pesa database and onboarding state. */
class PersonalDataEraser @Inject constructor(
    private val mpesaDatabase: MpesaDatabase,
    private val onboardingPreferences: MpesaOnboardingPreferences
) : FlavorDataEraser {
    override suspend fun eraseFlavorData() {
        withContext(Dispatchers.IO) { mpesaDatabase.clearAllTables() }
        onboardingPreferences.resetOnboarding()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FlavorDataEraserModule {
    @Binds
    @Singleton
    abstract fun bindFlavorDataEraser(impl: PersonalDataEraser): FlavorDataEraser
}
