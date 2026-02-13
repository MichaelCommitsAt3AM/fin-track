package com.fintrack.app.core.security

import javax.crypto.SecretKey

interface KeyRepository {
    suspend fun getOrGenerateMasterKey(): SecretKey
}
