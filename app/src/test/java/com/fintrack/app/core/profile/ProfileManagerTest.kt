package com.fintrack.app.core.profile

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeStore(var saved: PersistedProfile? = null) : ProfileStore {
    override suspend fun load() = saved
    override suspend fun save(profile: PersistedProfile) {
        saved = profile
    }
}

class ProfileManagerTest {

    private fun manager(store: ProfileStore = FakeStore(), uid: String? = null, ids: List<String> = listOf("uuid-1", "uuid-2")): ProfileManagerImpl {
        val queue = ids.toMutableList()
        return ProfileManagerImpl(store, { uid }, { queue.removeAt(0) })
    }

    @Test
    fun `fresh install is NONE with no profile`() {
        val m = manager()
        assertEquals(AccountMode.NONE, m.state.value.mode)
        assertNull(m.profileId())
    }

    @Test
    fun `existing signed-in user migrates to ACCOUNT with profileId equal to uid`() {
        val store = FakeStore()
        val m = manager(store, uid = "abc123")
        val s = m.state.value
        assertEquals(AccountMode.ACCOUNT, s.mode)
        assertEquals("abc123", s.profileId)
        assertEquals("abc123", s.linkedUid)
        assertTrue(s.cloudSyncEnabled)
        // persisted, so the migration only runs once
        assertNotNull(store.saved)
        val again = manager(store, uid = "someone-else")
        assertEquals("abc123", again.profileId())
    }

    @Test
    fun `createLocalProfile gives a stable LOCAL_ONLY uuid`() = runBlocking {
        val store = FakeStore()
        val m = manager(store)
        val id = m.createLocalProfile()
        assertEquals("uuid-1", id)
        assertEquals(AccountMode.LOCAL_ONLY, m.state.value.mode)
        assertEquals(id, m.createLocalProfile())
        assertFalse(m.state.value.cloudSyncEnabled)
        // survives a restart
        assertEquals(id, manager(store).profileId())
    }

    @Test
    fun `linking from local keeps the profile id`() = runBlocking {
        val m = manager()
        val local = m.createLocalProfile()
        m.linkAccount("uid-9")
        val s = m.state.value
        assertEquals(AccountMode.ACCOUNT, s.mode)
        assertEquals(local, s.profileId)
        assertEquals("uid-9", s.linkedUid)
        assertTrue(s.cloudSyncEnabled)
    }

    @Test
    fun `different account after sign out gets a different profile, same account gets its own back`() = runBlocking {
        val m = manager()
        val local = m.createLocalProfile()
        m.linkAccount("A")
        m.signOutToFreshState()
        assertNull(m.profileId())
        assertEquals(AccountMode.NONE, m.state.value.mode)
        m.linkAccount("B")
        assertEquals("B", m.profileId())
        assertNotEquals(local, m.profileId())
        m.signOutToFreshState()
        m.linkAccount("A")
        assertEquals(local, m.profileId())
    }

    @Test
    fun `cloud sync toggle only applies to accounts`() = runBlocking {
        val m = manager()
        m.createLocalProfile()
        m.setCloudSyncEnabled(true)
        assertFalse(m.state.value.cloudSyncEnabled)
        m.linkAccount("A")
        m.setCloudSyncEnabled(false)
        assertFalse(m.state.value.cloudSyncEnabled)
        m.setCloudSyncEnabled(true)
        assertTrue(m.state.value.cloudSyncEnabled)
    }
}

class SyncGateTest {

    private class Env(initialAuth: String?) {
        val auth = MutableStateFlow(initialAuth)
        val manager = ProfileManagerImpl(FakeStore(), { auth.value }, { "local-uuid" })
        val gate = SyncGateImpl(manager, { auth.value }, auth)
    }

    @Test
    fun `fresh install cannot sync`() {
        assertNull(Env(null).gate.cloudUidOrNull())
    }

    @Test
    fun `local only cannot sync`() = runBlocking {
        val e = Env(null)
        e.manager.createLocalProfile()
        assertNull(e.gate.cloudUidOrNull())
        assertFalse(e.gate.canSync.first())
    }

    @Test
    fun `account with sync on returns the uid`() = runBlocking {
        val e = Env("A")
        e.manager.linkAccount("A")
        assertEquals("A", e.gate.cloudUidOrNull())
        assertTrue(e.gate.canSync.first())
    }

    @Test
    fun `sync off returns null`() = runBlocking {
        val e = Env("A")
        e.manager.linkAccount("A")
        e.manager.setCloudSyncEnabled(false)
        assertNull(e.gate.cloudUidOrNull())
        assertFalse(e.gate.canSync.first())
    }

    @Test
    fun `linked uid that differs from the signed-in firebase user returns null`() = runBlocking {
        val e = Env("A")
        e.manager.linkAccount("A")
        e.auth.value = "B"
        assertNull(e.gate.cloudUidOrNull())
        assertFalse(e.gate.canSync.first())
        e.auth.value = null
        assertNull(e.gate.cloudUidOrNull())
    }
}
