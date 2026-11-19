package com.fintrack.app.core.profile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/** Persistence for [ProfileManagerImpl]; swapped for an in-memory fake in unit tests. */
interface ProfileStore {
    /** Null when nothing was ever saved (first launch after install or update). */
    suspend fun load(): PersistedProfile?
    suspend fun save(profile: PersistedProfile)
}

/** [profileByUid] remembers which profile each linked account used on this phone. */
data class PersistedProfile(
    val state: ProfileState,
    val profileByUid: Map<String, String> = emptyMap()
)

private val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "profile_prefs")

class DataStoreProfileStore(private val context: Context) : ProfileStore {
    private object Keys {
        val INITIALIZED = booleanPreferencesKey("initialized")
        val PROFILE_ID = stringPreferencesKey("profile_id")
        val MODE = stringPreferencesKey("mode")
        val LINKED_UID = stringPreferencesKey("linked_uid")
        val SYNC = booleanPreferencesKey("cloud_sync_enabled")
        val PROFILE_BY_UID = stringPreferencesKey("profile_by_uid")
    }

    override suspend fun load(): PersistedProfile? {
        val p = context.profileDataStore.data.first()
        if (p[Keys.INITIALIZED] != true) return null
        val mode = runCatching { AccountMode.valueOf(p[Keys.MODE] ?: "NONE") }.getOrDefault(AccountMode.NONE)
        return PersistedProfile(
            ProfileState(p[Keys.PROFILE_ID], mode, p[Keys.LINKED_UID], p[Keys.SYNC] ?: false),
            decodeMap(p[Keys.PROFILE_BY_UID])
        )
    }

    override suspend fun save(profile: PersistedProfile) {
        context.profileDataStore.edit { p ->
            p[Keys.INITIALIZED] = true
            p[Keys.MODE] = profile.state.mode.name
            p[Keys.SYNC] = profile.state.cloudSyncEnabled
            val id = profile.state.profileId
            if (id != null) p[Keys.PROFILE_ID] = id else p.remove(Keys.PROFILE_ID)
            val uid = profile.state.linkedUid
            if (uid != null) p[Keys.LINKED_UID] = uid else p.remove(Keys.LINKED_UID)
            p[Keys.PROFILE_BY_UID] = encodeMap(profile.profileByUid)
        }
    }

    // Firebase UIDs and UUIDs never contain ',' or '='.
    private fun encodeMap(m: Map<String, String>) = m.entries.joinToString(",") { "${it.key}=${it.value}" }

    private fun decodeMap(s: String?): Map<String, String> =
        s.orEmpty().split(",").mapNotNull { entry ->
            entry.split("=").takeIf { it.size == 2 }?.let { it[0] to it[1] }
        }.toMap()
}
