package com.fintrack.app.core.profile

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** NONE = fresh install (or signed out), the user hasn't chosen a mode yet. */
enum class AccountMode { NONE, LOCAL_ONLY, ACCOUNT }

/**
 * @property profileId owns every Room row (the `userId` column). Never changes for a profile.
 * @property linkedUid Firebase UID naming the Firestore path; only set in [AccountMode.ACCOUNT].
 */
data class ProfileState(
    val profileId: String? = null,
    val mode: AccountMode = AccountMode.NONE,
    val linkedUid: String? = null,
    val cloudSyncEnabled: Boolean = false
)

interface ProfileManager {
    val state: StateFlow<ProfileState>

    /** Non-null once a mode is chosen. Safe to call from non-suspending code. */
    fun profileId(): String?

    /** Starts a LOCAL_ONLY profile with a new UUID (returns the existing one if already local). */
    suspend fun createLocalProfile(): String

    /**
     * Records [uid] as the cloud account for this profile and enables sync.
     * - From NONE: the profile becomes the account's profile (the one previously used by this
     *   account on this phone, otherwise `uid` itself, as for installs that pre-date profiles).
     * - From LOCAL_ONLY: the local profile keeps its ID (no Room row is re-keyed).
     */
    suspend fun linkAccount(uid: String)

    suspend fun setCloudSyncEnabled(enabled: Boolean)

    /**
     * Forgets the current profile and goes back to NONE (Login). Room rows are left in place,
     * keyed by their profile ID, so a different account never sees them. The same account
     * signing in again gets its profile back.
     */
    suspend fun signOutToFreshState()
}

interface SyncGate {
    /** UID to sync to, or null when sync must not happen (no account, sync off, signed out). */
    fun cloudUidOrNull(): String?

    val canSync: Flow<Boolean>
}
