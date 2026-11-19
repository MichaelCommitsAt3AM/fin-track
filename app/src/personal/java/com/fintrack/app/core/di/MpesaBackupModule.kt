package com.fintrack.app.core.di

import android.content.Context
import com.fintrack.app.core.backup.BackupKeyCache
import com.fintrack.app.core.backup.MpesaBackupPrefs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MpesaBackupModule {

    @Provides
    @Singleton
    fun provideBackupKeyCache(@ApplicationContext context: Context): BackupKeyCache = BackupKeyCache(context)

    @Provides
    @Singleton
    fun provideMpesaBackupPrefs(@ApplicationContext context: Context): MpesaBackupPrefs = MpesaBackupPrefs(context)
}
