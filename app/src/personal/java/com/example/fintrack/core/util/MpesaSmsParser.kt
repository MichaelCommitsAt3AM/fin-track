package com.example.fintrack.core.util

import com.example.fintrack.core.domain.model.MpesaTransactionType
import com.example.fintrack.core.domain.model.TransactionType
import java.util.Locale

/**
 * Parser for M-Pesa SMS messages.
 * Extracts transaction details using regex patterns.
 */
class MpesaSmsParser(
    private val smartClueDetector: SmartClueDetector
) {
    
    companion object {
        // Common M-Pesa SMS senders
        private val MPESA_SENDERS = setOf("MPESA", "M-PESA", "SAFARICOM")
        
        // Regex patterns for different transaction types
        
        // Pattern: "Confirmed. Ksh1,000.00 sent to JOHN DOE 0712345678 on 21/1/26..."
        private val SENT_MONEY_PATTERN = Regex(
            """(?i)confirmed\.?\s+ksh\.?\s*([\d,]+\.?\d*)\s+sent\s+to\s+([^0-9]+?)\s*(\d{10})""",
            RegexOption.IGNORE_CASE
        )
        
        // Pattern: "Confirmed. You have received Ksh2,000.00 from JANE DOE 0712345678..."
        private val RECEIVED_MONEY_PATTERN = Regex(
            """(?i)confirmed\.?\s+you\s+have\s+received\s+ksh\.?\s*([\d,]+\.?\d*)\s+from\s+([^0-9]+?)\s*(\d{10})?""",
            RegexOption.IGNORE_CASE
        )
        
        // Pattern: "Confirmed. Ksh300.00 paid to Paybill 400200, account number 12345..."
        private val PAYBILL_PATTERN = Regex(
            """(?i)confirmed\.?\s+ksh\.?\s*([\d,]+\.?\d*)\s+paid\s+to\s+(?:paybill\s+)?(\d+)(?:.*?account\s+(?:number\s+)?([^\s.]+))?""",
            RegexOption.IGNORE_CASE
        )
        
        // Pattern: "Confirmed. Ksh150.00 paid for till number 123456..."
        private val TILL_PATTERN = Regex(
            """(?i)confirmed\.?\s+ksh\.?\s*([\d,]+\.?\d*)\s+paid\s+(?:to|for)\s+till\s+(?:number\s+)?(\d+)""",
            RegexOption.IGNORE_CASE
        )
        
        // Pattern: "Confirmed. Ksh100.00 airtime purchase..."
        private val AIRTIME_PATTERN = Regex(
            """(?i)confirmed\.?\s+ksh\.?\s*([\d,]+\.?\d*)\s+airtime""",
            RegexOption.IGNORE_CASE
        )
        
        // Pattern: "Confirmed. Ksh5,000.00 withdrawn from agent 12345..."
        private val WITHDRAW_PATTERN = Regex(
            """(?i)confirmed\.?\s+ksh\.?\s*([\d,]+\.?\d*)\s+withdrawn\s+from(?:\s+agent)?\s+(\d+)?""",
            RegexOption.IGNORE_CASE
        )
        
        // Pattern: "Confirmed. Ksh2,000.00 deposited to agent 12345..."
        private val DEPOSIT_PATTERN = Regex(
            """(?i)confirmed\.?\s+ksh\.?\s*([\d,]+\.?\d*)\s+deposited\s+to(?:\s+agent)?\s+(\d+)?""",
            RegexOption.IGNORE_CASE
        )
        
        // Pattern for extracting M-Pesa receipt number (e.g., QK87ABCD12)
        private val RECEIPT_PATTERN = Regex(
            """([A-Z]{2}\d{2}[A-Z0-9]{6,10})""",
            RegexOption.IGNORE_CASE
        )
        
        // Pattern for extracting merchant/business name from various contexts
        private val MERCHANT_PATTERN = Regex(
            """(?:paid\s+to|from)\s+([A-Z][A-Z\s&]+?)(?:\s+on|\s+for|\.|,|\s+transaction|$)""",
            RegexOption.IGNORE_CASE
        )
    }
    
    /**
     * Check if an SMS is from M-Pesa.
     */
    fun isMpesaSms(sender: String?): Boolean {
        return sender?.uppercase(Locale.ROOT)?.let { upper ->
            MPESA_SENDERS.any { upper.contains(it) }
        } ?: false
    }
    
    /**
     * Parse M-Pesa SMS and extract transaction details.
     * 
     * @param smsBody The SMS message body
     * @return ParsedMpesaTransaction if successfully parsed, null otherwise
     */
    fun parseSms(smsBody: String): ParsedMpesaTransaction? {
        if (!smsBody.contains("Confirmed", ignoreCase = true)) {
            return null // Not a confirmed M-Pesa transaction
        }
        
        // Try to parse as different transaction types
        parseSentMoney(smsBody)?.let { return it }
        parseReceivedMoney(smsBody)?.let { return it }
        parsePaybill(smsBody)?.let { return it }
        parseTill(smsBody)?.let { return it }
        parseAirtime(smsBody)?.let { return it }
        parseWithdraw(smsBody)?.let { return it }
        parseDeposit(smsBody)?.let { return it }
        
        return null
    }
    
    private fun parseSentMoney(smsBody: String): ParsedMpesaTransaction? {
        val match = SENT_MONEY_PATTERN.find(smsBody) ?: return null
        
        val amount = parseAmount(match.groupValues[1])
        val merchantName = match.groupValues[2].trim().cleanMerchantName()
        val phoneNumber = match.groupValues.getOrNull(3)?.trim()
        val receipt = extractReceipt(smsBody)
        val smartClues = smartClueDetector.detectClues(merchantName, smsBody)
        
        return ParsedMpesaTransaction(
            mpesaReceiptNumber = receipt ?: "UNKNOWN",
            amount = amount,
            type = TransactionType.EXPENSE,
            transactionType = MpesaTransactionType.SEND_MONEY,
            merchantName = merchantName,
            phoneNumber = phoneNumber,
            smartClues = smartClues
        )
    }
    
    private fun parseReceivedMoney(smsBody: String): ParsedMpesaTransaction? {
        val match = RECEIVED_MONEY_PATTERN.find(smsBody) ?: return null
        
        val amount = parseAmount(match.groupValues[1])
        val merchantName = match.groupValues[2].trim().cleanMerchantName()
        val phoneNumber = match.groupValues.getOrNull(3)?.trim()
        val receipt = extractReceipt(smsBody)
        val smartClues = smartClueDetector.detectClues(merchantName, smsBody)
        
        return ParsedMpesaTransaction(
            mpesaReceiptNumber = receipt ?: "UNKNOWN",
            amount = amount,
            type = TransactionType.INCOME,
            transactionType = MpesaTransactionType.RECEIVE_MONEY,
            merchantName = merchantName,
            phoneNumber = phoneNumber,
            smartClues = smartClues
        )
    }
    
    private fun parsePaybill(smsBody: String): ParsedMpesaTransaction? {
        val match = PAYBILL_PATTERN.find(smsBody) ?: return null
        
        val amount = parseAmount(match.groupValues[1])
        val paybillNumber = match.groupValues[2]
        val accountNumber = match.groupValues.getOrNull(3)?.trim()
        val receipt = extractReceipt(smsBody)
        val merchantName = extractMerchantName(smsBody)
        val smartClues = smartClueDetector.detectClues(merchantName, smsBody)
        
        return ParsedMpesaTransaction(
            mpesaReceiptNumber = receipt ?: "UNKNOWN",
            amount = amount,
            type = TransactionType.EXPENSE,
            transactionType = MpesaTransactionType.PAYBILL,
            merchantName = merchantName,
            paybillNumber = paybillNumber,
            accountNumber = accountNumber,
            smartClues = smartClues
        )
    }
    
    private fun parseTill(smsBody: String): ParsedMpesaTransaction? {
        val match = TILL_PATTERN.find(smsBody) ?: return null
        
        val amount = parseAmount(match.groupValues[1])
        val tillNumber = match.groupValues[2]
        val receipt = extractReceipt(smsBody)
        val merchantName = extractMerchantName(smsBody)
        val smartClues = smartClueDetector.detectClues(merchantName, smsBody)
        
        return ParsedMpesaTransaction(
            mpesaReceiptNumber = receipt ?: "UNKNOWN",
            amount = amount,
            type = TransactionType.EXPENSE,
            transactionType = MpesaTransactionType.TILL,
            merchantName = merchantName,
            tillNumber = tillNumber,
            smartClues = smartClues
        )
    }
    
    private fun parseAirtime(smsBody: String): ParsedMpesaTransaction? {
        val match = AIRTIME_PATTERN.find(smsBody) ?: return null
        
        val amount = parseAmount(match.groupValues[1])
        val receipt = extractReceipt(smsBody)
        val smartClues = listOf("AIRTIME:AIRTIME")
        
        return ParsedMpesaTransaction(
            mpesaReceiptNumber = receipt ?: "UNKNOWN",
            amount = amount,
            type = TransactionType.EXPENSE,
            transactionType = MpesaTransactionType.AIRTIME,
            merchantName = "Airtime Purchase",
            smartClues = smartClues
        )
    }
    
    private fun parseWithdraw(smsBody: String): ParsedMpesaTransaction? {
        val match = WITHDRAW_PATTERN.find(smsBody) ?: return null
        
        val amount = parseAmount(match.groupValues[1])
        val agentNumber = match.groupValues.getOrNull(2)?.trim()
        val receipt = extractReceipt(smsBody)
        
        return ParsedMpesaTransaction(
            mpesaReceiptNumber = receipt ?: "UNKNOWN",
            amount = amount,
            type = TransactionType.EXPENSE,
            transactionType = MpesaTransactionType.WITHDRAW,
            merchantName = agentNumber?.let { "Agent $it" } ?: "M-Pesa Agent",
            smartClues = emptyList()
        )
    }
    
    private fun parseDeposit(smsBody: String): ParsedMpesaTransaction? {
        val match = DEPOSIT_PATTERN.find(smsBody) ?: return null
        
        val amount = parseAmount(match.groupValues[1])
        val agentNumber = match.groupValues.getOrNull(2)?.trim()
        val receipt = extractReceipt(smsBody)
        
        return ParsedMpesaTransaction(
            mpesaReceiptNumber = receipt ?: "UNKNOWN",
            amount = amount,
            type = TransactionType.INCOME,
            transactionType = MpesaTransactionType.DEPOSIT,
            merchantName = agentNumber?.let { "Agent $it" } ?: "M-Pesa Agent",
            smartClues = emptyList()
        )
    }
    
    private fun parseAmount(amountString: String): Double {
        return amountString.replace(",", "").toDoubleOrNull() ?: 0.0
    }
    
    private fun extractReceipt(smsBody: String): String? {
        return RECEIPT_PATTERN.find(smsBody)?.value
    }
    
    private fun extractMerchantName(smsBody: String): String? {
        return MERCHANT_PATTERN.find(smsBody)?.groupValues?.get(1)?.trim()?.cleanMerchantName()
    }
    
    private fun String.cleanMerchantName(): String {
        return this
            .trim()
            .replace(Regex("""\s+"""), " ") // Replace multiple spaces with single space
            .replace(Regex("""[^\w\s&-]"""), "") // Remove special characters except &, -, and spaces
            .uppercase(Locale.ROOT)
    }
}

/**
 * Data class representing a parsed M-Pesa transaction.
 */
data class ParsedMpesaTransaction(
    val mpesaReceiptNumber: String,
    val amount: Double,
    val type: TransactionType,
    val transactionType: MpesaTransactionType,
    val merchantName: String?,
    val phoneNumber: String? = null,
    val paybillNumber: String? = null,
    val tillNumber: String? = null,
    val accountNumber: String? = null,
    val smartClues: List<String> = emptyList()
)
