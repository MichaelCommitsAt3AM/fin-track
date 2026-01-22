package com.example.fintrack.core.di

import android.content.Context
import androidx.room.Room
import com.example.fintrack.core.data.local.MpesaDatabase
import com.example.fintrack.core.data.local.dao.MerchantCategoryDao
import com.example.fintrack.core.data.local.dao.MpesaTransactionDao
import com.example.fintrack.core.data.repository.MpesaTransactionRepositoryImpl
import com.example.fintrack.core.domain.repository.MpesaTransactionRepository
import com.example.fintrack.core.util.MpesaSmsParser
import com.example.fintrack.core.util.SmartClueDetector
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
        impl: com.example.fintrack.presentation.onboarding.MpesaOnboardingIntegrationImpl
    ): com.example.fintrack.presentation.navigation.OnboardingIntegration
    
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
        ): com.example.fintrack.core.data.preferences.MpesaOnboardingPreferences {
            return com.example.fintrack.core.data.preferences.MpesaOnboardingPreferences(context)
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
