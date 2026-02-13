package com.fintrack.app.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3, // Incremented for transactionCost field
    exportSchema = false
)
abstract class MpesaDatabase : RoomDatabase() {
    
    abstract fun mpesaTransactionDao(): MpesaTransactionDao
    abstract fun merchantCategoryDao(): MerchantCategoryDao
    
    companion object {
        const val DATABASE_NAME = "mpesa_db"
        
        /**
         * Migration from version 2 to 3: Add transactionCost column
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE mpesa_transactions ADD COLUMN transactionCost REAL DEFAULT NULL"
                )
            }
        }
    }
}
