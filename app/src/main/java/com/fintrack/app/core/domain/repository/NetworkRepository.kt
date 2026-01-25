package com.fintrack.app.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface NetworkRepository {
    fun observeNetworkConnectivity(): Flow<Boolean>
    fun isNetworkAvailable(): Boolean
}
