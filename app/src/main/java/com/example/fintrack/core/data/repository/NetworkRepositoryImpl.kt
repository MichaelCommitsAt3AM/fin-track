package com.example.fintrack.core.data.repository

import android.content.Context
import com.example.fintrack.core.domain.repository.NetworkRepository
import com.example.fintrack.util.NetworkUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkRepository {

    override fun observeNetworkConnectivity(): Flow<Boolean> {
        return NetworkUtils.observeNetworkConnectivity(context)
    }

    override fun isNetworkAvailable(): Boolean {
        return NetworkUtils.isNetworkAvailable(context)
    }
}
