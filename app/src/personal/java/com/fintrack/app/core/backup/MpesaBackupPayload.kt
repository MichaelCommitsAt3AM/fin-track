package com.fintrack.app.core.backup

import com.fintrack.app.core.data.local.model.MerchantCategoryEntity
import com.fintrack.app.core.data.local.model.MpesaCategoryMappingEntity
import com.fintrack.app.core.data.local.model.MpesaTransactionEntity
import org.json.JSONObject

/**
 * The plaintext that gets encrypted into a backup record. This is the single place that decides
 * what leaves the phone, and it is deliberately an allow-list:
 *
 * uploaded:  receipt, amount, type, transactionType, merchantName, paybillNumber, tillNumber,
 *            accountNumber, timestamp, transactionCost, parserVersion, and the user's category
 *            choices (receipt overrides and merchant rules).
 * NEVER:     rawBody (the SMS text), smsId, smartClues, phoneNumber, balances.
 */
object MpesaBackupPayload {
    const val SCHEMA_VERSION = 1

    /** Source value for rows restored from a backup (no SMS text exists for them). */
    const val SOURCE_SMS = "SMS"
    const val SOURCE_BACKUP = "BACKUP"

    /** Every field a transaction payload may contain. Tests assert nothing else appears. */
    val TRANSACTION_FIELDS = setOf(
        "v", "receipt", "amount", "type", "transactionType", "merchantName", "paybillNumber",
        "tillNumber", "accountNumber", "timestamp", "transactionCost", "parserVersion"
    )

    fun transactionJson(e: MpesaTransactionEntity): String = JSONObject().apply {
        put("v", SCHEMA_VERSION)
        put("receipt", e.mpesaReceiptNumber)
        put("amount", e.amount)
        put("type", e.type)
        put("transactionType", e.transactionType)
        put("merchantName", e.merchantName ?: JSONObject.NULL)
        put("paybillNumber", e.paybillNumber ?: JSONObject.NULL)
        put("tillNumber", e.tillNumber ?: JSONObject.NULL)
        put("accountNumber", e.accountNumber ?: JSONObject.NULL)
        put("timestamp", e.timestamp)
        put("transactionCost", e.transactionCost ?: JSONObject.NULL)
        put("parserVersion", e.parserVersion)
    }.toString()

    fun receiptMappingJson(m: MpesaCategoryMappingEntity): String = JSONObject().apply {
        put("v", SCHEMA_VERSION)
        put("kind", "receipt")
        put("receipt", m.mpesaReceiptNumber)
        put("category", m.categoryName)
    }.toString()

    fun merchantRuleJson(m: MerchantCategoryEntity): String = JSONObject().apply {
        put("v", SCHEMA_VERSION)
        put("kind", "merchant")
        put("merchantName", m.merchantName)
        put("category", m.categoryName)
        put("isUserConfirmed", m.isUserConfirmed)
        put("updatedAt", m.updatedAt)
    }.toString()

    /** Restored rows have no SMS: empty text, a synthetic SMS id, and [SOURCE_BACKUP]. */
    fun transactionFromJson(json: String, now: Long = System.currentTimeMillis()): MpesaTransactionEntity {
        val o = JSONObject(json)
        val receipt = o.getString("receipt")
        return MpesaTransactionEntity(
            mpesaReceiptNumber = receipt,
            smsId = "backup:$receipt",
            amount = o.getDouble("amount"),
            type = o.getString("type"),
            merchantName = o.optStringOrNull("merchantName"),
            phoneNumber = null,
            paybillNumber = o.optStringOrNull("paybillNumber"),
            tillNumber = o.optStringOrNull("tillNumber"),
            accountNumber = o.optStringOrNull("accountNumber"),
            transactionType = o.getString("transactionType"),
            rawBody = "",
            smartClues = null,
            parserVersion = o.getInt("parserVersion"),
            timestamp = o.getLong("timestamp"),
            transactionCost = if (o.isNull("transactionCost")) null else o.getDouble("transactionCost"),
            createdAt = now,
            source = SOURCE_BACKUP
        )
    }

    fun mappingKind(json: String): String = JSONObject(json).getString("kind")

    fun receiptMappingFromJson(json: String): MpesaCategoryMappingEntity {
        val o = JSONObject(json)
        return MpesaCategoryMappingEntity(
            mpesaReceiptNumber = o.getString("receipt"),
            categoryName = o.getString("category")
        )
    }

    fun merchantRuleFromJson(json: String): MerchantCategoryEntity {
        val o = JSONObject(json)
        return MerchantCategoryEntity(
            merchantName = o.getString("merchantName"),
            categoryName = o.getString("category"),
            isUserConfirmed = o.optBoolean("isUserConfirmed", true),
            updatedAt = o.optLong("updatedAt", 0L)
        )
    }

    private fun JSONObject.optStringOrNull(name: String): String? =
        if (isNull(name)) null else getString(name)
}
