package com.fintrack.app.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fintrack.app.core.data.local.dao.MpesaTransactionDao
import com.fintrack.app.core.data.local.dao.MerchantCategoryDao
import com.fintrack.app.core.data.local.model.MerchantCategoryEntity
import com.fintrack.app.core.data.local.model.MpesaTransactionEntity

/**
 * Personal flavor database for M-Pesa transactions.
 * 
 * This database is SEPARATE from the main FinanceDatabase to ensure
 * M-Pesa data remains local-only and is never synced to Firestore.
 */
@Database(
    entities = [
        MpesaTransactionEntity::class,
        MerchantCategoryEntity::class
    ],
    version = 2, // Incrementing version (destructive migration enabled)
    exportSchema = false
)
abstract class MpesaDatabase : RoomDatabase() {
    
    abstract fun mpesaTransactionDao(): MpesaTransactionDao
    abstract fun merchantCategoryDao(): MerchantCategoryDao
    
    companion object {
        const val DATABASE_NAME = "mpesa_db"
    }
}
