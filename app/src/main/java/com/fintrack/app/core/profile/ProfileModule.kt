package com.fintrack.app.core.profile

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProfileModule {

    @Provides
    @Singleton
    fun provideProfileManager(@ApplicationContext context: Context, auth: FirebaseAuth): ProfileManager =
        ProfileManagerImpl(DataStoreProfileStore(context), currentAuthUid = { auth.currentUser?.uid })

    @Provides
    @Singleton
    fun provideSyncGate(profileManager: ProfileManager, auth: FirebaseAuth): SyncGate =
        SyncGateImpl(
            profileManager,
            authUid = { auth.currentUser?.uid },
            authUidFlow = callbackFlow {
                val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
                auth.addAuthStateListener(listener)
                awaitClose { auth.removeAuthStateListener(listener) }
            }
        )
}
