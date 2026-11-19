package com.fintrack.app.core.backup

import com.fintrack.app.core.data.local.model.MerchantCategoryEntity
import com.fintrack.app.core.data.local.model.MpesaCategoryMappingEntity
import com.fintrack.app.core.data.local.model.MpesaTransactionEntity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MpesaBackupPayloadTest {

    private val entity = MpesaTransactionEntity(
        mpesaReceiptNumber = "QK87ABCD12",
        smsId = "SMS-ID-SECRET-123",
        amount = 1500.0,
        type = "EXPENSE",
        merchantName = "JAVA HOUSE",
        phoneNumber = "0712345678",
        paybillNumber = "123456",
        tillNumber = null,
        accountNumber = "ACC-9",
        transactionType = "PAYBILL",
        rawBody = "QK87ABCD12 Confirmed. Ksh1,500.00 paid to JAVA HOUSE. New M-PESA balance is Ksh9,999.00",
        smartClues = "FOOD:java",
        parserVersion = 3,
        timestamp = 1_700_000_000_000L,
        transactionCost = 11.0
    )

    @Test
    fun `payload contains only the allow-listed fields`() {
        val keys = JSONObject(MpesaBackupPayload.transactionJson(entity)).keys().asSequence().toSet()
        assertEquals(MpesaBackupPayload.TRANSACTION_FIELDS, keys)
    }

    @Test
    fun `payload has no rawBody, phoneNumber, smsId or smartClues`() {
        val json = MpesaBackupPayload.transactionJson(entity)
        listOf("rawBody", "phoneNumber", "smsId", "smartClues", "balance").forEach {
            assertFalse("payload must not have a '$it' field", json.contains("\"$it\""))
        }
        // ...nor their values: SMS text, balance, phone number, sms id, clues
        listOf("0712345678", "SMS-ID-SECRET-123", "New M-PESA balance", "9,999", "FOOD:java", "Confirmed").forEach {
            assertFalse("payload must not contain '$it'", json.contains(it))
        }
    }

    @Test
    fun `payload keeps what the consent screen promises`() {
        val o = JSONObject(MpesaBackupPayload.transactionJson(entity))
        assertEquals("QK87ABCD12", o.getString("receipt"))
        assertEquals(1500.0, o.getDouble("amount"), 0.0)
        assertEquals("EXPENSE", o.getString("type"))
        assertEquals("PAYBILL", o.getString("transactionType"))
        assertEquals("JAVA HOUSE", o.getString("merchantName"))
        assertEquals("123456", o.getString("paybillNumber"))
        assertTrue(o.isNull("tillNumber"))
        assertEquals("ACC-9", o.getString("accountNumber"))
        assertEquals(11.0, o.getDouble("transactionCost"), 0.0)
        assertEquals(1_700_000_000_000L, o.getLong("timestamp"))
        assertEquals(3, o.getInt("parserVersion"))
    }

    @Test
    fun `restored rows have no sms data and are marked as backup`() {
        val restored = MpesaBackupPayload.transactionFromJson(MpesaBackupPayload.transactionJson(entity), now = 42L)
        assertEquals("QK87ABCD12", restored.mpesaReceiptNumber)
        assertEquals("backup:QK87ABCD12", restored.smsId)
        assertEquals("", restored.rawBody)
        assertNull(restored.smartClues)
        assertNull(restored.phoneNumber)
        assertNull(restored.tillNumber)
        assertEquals("BACKUP", restored.source)
        assertEquals(entity.amount, restored.amount, 0.0)
        assertEquals(entity.merchantName, restored.merchantName)
        assertEquals(entity.transactionCost, restored.transactionCost)
        assertEquals(entity.timestamp, restored.timestamp)
        assertEquals(entity.parserVersion, restored.parserVersion)
    }

    @Test
    fun `category choices round-trip with a kind`() {
        val receipt = MpesaBackupPayload.receiptMappingJson(MpesaCategoryMappingEntity("QK1", "Food"))
        assertEquals("receipt", MpesaBackupPayload.mappingKind(receipt))
        assertEquals("Food", MpesaBackupPayload.receiptMappingFromJson(receipt).categoryName)
        assertEquals("QK1", MpesaBackupPayload.receiptMappingFromJson(receipt).mpesaReceiptNumber)

        val merchant = MpesaBackupPayload.merchantRuleJson(MerchantCategoryEntity("JAVA", "Food", true, 7L))
        assertEquals("merchant", MpesaBackupPayload.mappingKind(merchant))
        val back = MpesaBackupPayload.merchantRuleFromJson(merchant)
        assertEquals("JAVA", back.merchantName)
        assertEquals("Food", back.categoryName)
        assertTrue(back.isUserConfirmed)
        assertEquals(7L, back.updatedAt)
    }
}
