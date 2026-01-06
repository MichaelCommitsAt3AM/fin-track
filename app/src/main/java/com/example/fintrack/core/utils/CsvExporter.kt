package com.example.fintrack.core.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.fintrack.core.domain.model.Transaction
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class CsvExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun generateTransactionCsv(transactions: List<Transaction>): Uri? {
        try {
            // Create a temporary file in the cache directory
            val fileName = "fintrack_export_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)

            // Write CSV Header
            writer.append("Date,Type,Category,Amount,Payment Method,Notes,Tags\n")

            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())

            // Write Transaction Rows
            transactions.forEach { transaction ->
                val dateStr = dateFormatter.format(Instant.ofEpochMilli(transaction.date))
                val typeStr = transaction.type.name
                val categoryStr = escapeCsv(transaction.category)
                val amountStr = transaction.amount.toString()
                val payMethod = escapeCsv(transaction.paymentMethod ?: "")
                val notes = escapeCsv(transaction.notes ?: "")
                val tags = escapeCsv(transaction.tags?.joinToString(";") ?: "")

                writer.append("$dateStr,$typeStr,$categoryStr,$amountStr,$payMethod,$notes,$tags\n")
            }

            writer.flush()
            writer.close()

            // Return the Uri using FileProvider
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // Helper to handle commas and quotes in data
    private fun escapeCsv(data: String): String {
        var escapedData = data.replace("\"", "\"\"")
        if (data.contains(",") || data.contains("\n") || data.contains("\"")) {
            escapedData = "\"$escapedData\""
        }
        return escapedData
    }
}