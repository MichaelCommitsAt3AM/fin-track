package com.fintrack.app.core.di

import com.fintrack.app.core.security.EncryptionManager
import com.fintrack.app.core.security.EncryptionManagerImpl
import com.fintrack.app.core.security.KeyRepository
import com.fintrack.app.core.security.KeyRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindKeyRepository(
        keyRepositoryImpl: KeyRepositoryImpl
    ): KeyRepository

    @Binds
    @Singleton
    abstract fun bindEncryptionManager(
        encryptionManagerImpl: EncryptionManagerImpl
    ): EncryptionManager
}
