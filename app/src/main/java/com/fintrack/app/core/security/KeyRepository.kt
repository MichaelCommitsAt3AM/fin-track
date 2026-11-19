package com.fintrack.app.core.security

import javax.crypto.SecretKey

interface KeyRepository {
    /** The user whose master key [getOrGenerateMasterKey] returns, or null when signed out. */
    fun currentUserId(): String?

    suspend fun getOrGenerateMasterKey(): SecretKey
}
