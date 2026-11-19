package com.fintrack.app.core.profile

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * @param currentAuthUid the signed-in Firebase UID, used only for the one-time migration of
 *   installs that pre-date profiles.
 */
class ProfileManagerImpl(
    private val store: ProfileStore,
    private val currentAuthUid: () -> String?,
    private val newId: () -> String = { UUID.randomUUID().toString() }
) : ProfileManager {

    private val mutex = Mutex()
    private val _state = MutableStateFlow(ProfileState())
    private var profileByUid: Map<String, String> = emptyMap()

    @Volatile
    private var loaded = false

    /**
     * Loads (and, on the first run after the update, migrates) the persisted state. Called at
     * app start so [profileId] works in non-suspending code; also runs lazily as a safety net.
     */
    suspend fun ensureLoaded() {
        if (loaded) return
        mutex.withLock {
            if (loaded) return
            val persisted = store.load()
            if (persisted != null) {
                profileByUid = persisted.profileByUid
                _state.value = persisted.state
            } else {
                // First launch since profiles were introduced: an already signed-in user keeps
                // their data by using the UID as the profile ID, so no Room row changes.
                val uid = currentAuthUid()
                val migrated = if (uid != null) {
                    profileByUid = mapOf(uid to uid)
                    ProfileState(uid, AccountMode.ACCOUNT, uid, cloudSyncEnabled = true)
                } else {
                    ProfileState()
                }
                _state.value = migrated
                store.save(PersistedProfile(migrated, profileByUid))
            }
            loaded = true
        }
    }

    private fun ensureLoadedBlocking() {
        if (!loaded) runBlocking { ensureLoaded() }
    }

    override val state: StateFlow<ProfileState>
        get() {
            ensureLoadedBlocking()
            return _state.asStateFlow()
        }

    override fun profileId(): String? {
        ensureLoadedBlocking()
        return _state.value.profileId
    }

    private suspend fun update(newState: ProfileState, newMap: Map<String, String> = profileByUid) {
        profileByUid = newMap
        _state.value = newState
        store.save(PersistedProfile(newState, newMap))
    }

    override suspend fun createLocalProfile(): String {
        ensureLoaded()
        return mutex.withLock {
            val s = _state.value
            if (s.mode == AccountMode.LOCAL_ONLY && s.profileId != null) return@withLock s.profileId
            val id = newId()
            update(ProfileState(id, AccountMode.LOCAL_ONLY, null, false))
            id
        }
    }

    override suspend fun linkAccount(uid: String) {
        ensureLoaded()
        mutex.withLock {
            val s = _state.value
            val profileId = when {
                s.mode == AccountMode.LOCAL_ONLY && s.profileId != null -> s.profileId
                s.mode == AccountMode.ACCOUNT && s.linkedUid == uid && s.profileId != null -> s.profileId
                else -> profileByUid[uid] ?: uid
            }
            update(
                ProfileState(profileId, AccountMode.ACCOUNT, uid, cloudSyncEnabled = true),
                profileByUid + (uid to profileId)
            )
        }
    }

    override suspend fun setCloudSyncEnabled(enabled: Boolean) {
        ensureLoaded()
        mutex.withLock {
            val s = _state.value
            if (s.mode != AccountMode.ACCOUNT) return@withLock
            update(s.copy(cloudSyncEnabled = enabled))
        }
    }

    override suspend fun signOutToFreshState() {
        ensureLoaded()
        mutex.withLock { update(ProfileState()) }
    }
}
