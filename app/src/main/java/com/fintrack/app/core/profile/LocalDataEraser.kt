package com.fintrack.app.core.profile

import com.fintrack.app.core.data.local.FinanceDatabase
import com.fintrack.app.core.data.local.LocalAuthManager
import com.fintrack.app.core.data.local.SyncTimestampManager
import com.fintrack.app.core.di.FlavorDataEraser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Erase this app's data and start over" for local-only users who forgot their PIN. Removes
 * every Room row (including M-Pesa data in the personal flavor), the PIN/biometric settings and
 * the profile, leaving the app as on a fresh install. It never touches the cloud.
 */
@Singleton
class LocalDataEraser @Inject constructor(
    private val database: FinanceDatabase,
    private val localAuthManager: LocalAuthManager,
    private val syncTimestampManager: SyncTimestampManager,
    private val profileManager: ProfileManager,
    private val flavorDataEraser: FlavorDataEraser
) {
    suspend fun eraseAll() {
        withContext(Dispatchers.IO) { database.clearAllTables() }
        flavorDataEraser.eraseFlavorData()
        syncTimestampManager.clearAllTimestamps()
        localAuthManager.clearLocalAuth()
        profileManager.signOutToFreshState()
    }
}
