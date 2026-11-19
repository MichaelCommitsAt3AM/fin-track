package com.fintrack.app.core.profile

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Sync is allowed only when an account is linked, the Cloud sync switch is on, and the signed-in
 * Firebase user is the linked one (so a stale or different session can never write to another
 * account's path).
 */
class SyncGateImpl(
    private val profileManager: ProfileManager,
    private val authUid: () -> String?,
    authUidFlow: Flow<String?>
) : SyncGate {

    override fun cloudUidOrNull(): String? = gate(profileManager.state.value, authUid())

    override val canSync: Flow<Boolean> =
        combine(profileManager.state, authUidFlow) { s, uid -> gate(s, uid) != null }.distinctUntilChanged()

    private fun gate(s: ProfileState, uid: String?): String? =
        s.linkedUid?.takeIf { s.mode == AccountMode.ACCOUNT && s.cloudSyncEnabled && uid == it }
}
