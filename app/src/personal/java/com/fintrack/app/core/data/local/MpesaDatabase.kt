package com.fintrack.app.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fintrack.app.core.data.local.dao.MpesaTransactionDao
import com.fintrack.app.core.data.local.dao.MerchantCategoryDao
import com.fintrack.app.core.data.local.dao.MpesaBackupStateDao
import com.fintrack.app.core.data.local.model.MpesaBackupStateEntity
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
        MerchantCategoryEntity::class,
        MpesaBackupStateEntity::class
    ],
    version = 4, // v4: source column + mpesa_backup_state table (encrypted cloud backup)
    exportSchema = false
)
abstract class MpesaDatabase : RoomDatabase() {
    
    abstract fun mpesaTransactionDao(): MpesaTransactionDao
    abstract fun merchantCategoryDao(): MerchantCategoryDao
    abstract fun mpesaBackupStateDao(): MpesaBackupStateDao
    
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

        /**
         * Migration from version 3 to 4 (encrypted cloud backup): `source` marks rows restored
         * from a backup (no SMS body), and `mpesa_backup_state` tracks what was uploaded.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE mpesa_transactions ADD COLUMN source TEXT NOT NULL DEFAULT 'SMS'"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS mpesa_backup_state (" +
                        "docId TEXT NOT NULL, kind TEXT NOT NULL, contentHash TEXT NOT NULL, " +
                        "PRIMARY KEY(docId))"
                )
            }
        }
    }
}
