package com.fintrack.app.core.di

import android.content.Context
import androidx.room.Room
import com.fintrack.app.core.data.local.MpesaDatabase
import com.fintrack.app.core.data.local.dao.MerchantCategoryDao
import com.fintrack.app.core.data.local.dao.MpesaTransactionDao
import com.fintrack.app.core.data.repository.MpesaTransactionRepositoryImpl
import com.fintrack.app.core.domain.repository.MpesaTransactionRepository
import com.fintrack.app.core.util.MpesaSmsParser
import com.fintrack.app.core.util.SmartClueDetector
import dagger.Module
import dagger.Provides
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for M-Pesa-specific dependencies.
 * Only available in the personal build variant.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MpesaModule {
    
    @Binds
    @Singleton
    abstract fun bindMpesaTransactionRepository(
        impl: MpesaTransactionRepositoryImpl
    ): MpesaTransactionRepository

    @Binds
    @Singleton
    abstract fun bindOnboardingIntegration(
        impl: com.fintrack.app.presentation.onboarding.MpesaOnboardingIntegrationImpl
    ): com.fintrack.app.presentation.navigation.OnboardingIntegration
    
    companion object {
        
        @Provides
        @Singleton
        fun provideMpesaDatabase(
            @ApplicationContext context: Context
        ): MpesaDatabase {
            return Room.databaseBuilder(
                context,
                MpesaDatabase::class.java,
                MpesaDatabase.DATABASE_NAME
            )
                .fallbackToDestructiveMigration() // For development
                .build()
        }
        
        @Provides
        @Singleton
        fun provideMpesaOnboardingPreferences(
            @ApplicationContext context: Context
        ): com.fintrack.app.core.data.preferences.MpesaOnboardingPreferences {
            return com.fintrack.app.core.data.preferences.MpesaOnboardingPreferences(context)
        }
        
        @Provides
        @Singleton
        fun provideMpesaTransactionDao(
            database: MpesaDatabase
        ): MpesaTransactionDao {
            return database.mpesaTransactionDao()
        }
        
        @Provides
        @Singleton
        fun provideMerchantCategoryDao(
            database: MpesaDatabase
        ): MerchantCategoryDao {
            return database.merchantCategoryDao()
        }
        
        @Provides
        @Singleton
        fun provideSmartClueDetector(): SmartClueDetector {
            return SmartClueDetector()
        }
        
        @Provides
        @Singleton
        fun provideMpesaSmsParser(
            smartClueDetector: SmartClueDetector
        ): MpesaSmsParser {
            return MpesaSmsParser(smartClueDetector)
        }
    }
}
