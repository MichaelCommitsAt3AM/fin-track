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

        // 1. SENT MONEY - handles both regular sends and agent transactions
        private val SENT_MONEY_PATTERN = Regex(
            """(?i)confirmed\.?\s*ksh\.?\s*([\d,]+\.?\d*)\s+sent\s+to\s+(.+?)\s+(\d+)?.*?on\s+([\d/]+)""",
            RegexOption.IGNORE_CASE
        )

        private val WALLET_TRANSFER_PATTERN = Regex(
            """(?i)confirmed\.?\s*ksh\.?\s*([\d,]+\.?\d*)\s+sent\s+to\s+(AIRTEL MONEY|T\-?KASH|MTN|ORANGE MONEY|PAYPAL|EAZZY PAY).*?account\s+(\d{9,12})""",
            RegexOption.IGNORE_CASE
        )

        // 2. RECEIVED MONEY - simplified to catch all variations
        private val RECEIVED_MONEY_PATTERN = Regex(
            """(?i)confirmed\.?\s*(?:you\s+have\s+)?received\s+ksh\.?\s*([\d,]+\.?\d*)\s+from\s+(.+?)\s+(?:Bulk\s+)?Account\s+(\d+)\s+on""",
            RegexOption.IGNORE_CASE
        )

        // Alternative pattern for simple received money (no account number)
        private val RECEIVED_MONEY_SIMPLE_PATTERN = Regex(
            """(?i)confirmed\.?\s*you\s+have\s+received\s+ksh\.?\s*([\d,]+\.?\d*)\s+from\s+(.+?)\s+(\d+)?\s+on""",
            RegexOption.IGNORE_CASE
        )

        // 3. PAYBILL - more flexible matching
        private val PAYBILL_PATTERN = Regex(
            """(?i)confirmed\.?\s*ksh\.?\s*([\d,]+\.?\d*)\s+paid\s+to\s+(.+?)\.?\s+(?:for\s+account\s+|account\s+)?([^\s.]+?)?\s+on""",
            RegexOption.IGNORE_CASE
        )

        // 4. TILL NUMBER
        private val TILL_PATTERN = Regex(
            """(?i)confirmed\.?\s*ksh\.?\s*([\d,]+\.?\d*)\s+paid\s+(?:to|for)\s+till\s+(?:number\s+)?(\d+)""",
            RegexOption.IGNORE_CASE
        )

        // 5. BUY GOODS / GENERIC MERCHANT (maps to PAYBILL or TILL)
        private val BUY_GOODS_PATTERN = Regex(
            """(?i)confirmed\.?\s*ksh\.?\s*([\d,]+\.?\d*)\s+paid\s+to\s+([^.]+?)(?:\s+on\s+[\d/]+|\.)""",
            RegexOption.IGNORE_CASE
        )

        // 6. AIRTIME
        private val SAFARICOM_AIRTIME_PATTERN = Regex(
            """(?i)confirmed.*?ksh\.?\s*([\d,]+\.?\d*).*?airtime""",
            RegexOption.IGNORE_CASE
        )

        private val MSHWARI_TRANSFER_PATTERN = Regex(
            """(?i)confirmed\.?\s*ksh\.?\s*([\d,]+\.?\d*)\s+transferred\s+(from|to)\s+m-?shwari\s+account""",
            RegexOption.IGNORE_CASE
        )

        // 7. WITHDRAW (from agent)
        private val WITHDRAW_PATTERN = Regex(
            """(?i)confirmed\.?\s*ksh\.?\s*([\d,]+\.?\d*)\s+withdrawn.*?from\s+(.+?)\s+""",
            RegexOption.IGNORE_CASE
        )

        // 8. DEPOSIT (to agent)
        private val DEPOSIT_PATTERN = Regex(
            """(?i)confirmed\.?\s*ksh\.?\s*([\d,]+\.?\d*)\s+deposited.*?to\s+(.+?)\s+""",
            RegexOption.IGNORE_CASE
        )

        // 9. FULIZA (M-Pesa overdraft) - maps to WITHDRAW since it's money spent
        private val FULIZA_PATTERN = Regex(
            """(?i)confirmed\.?\s*ksh\.?\s*([\d,]+\.?\d*)\s+.*?fuliza""",
            RegexOption.IGNORE_CASE
        )

        // 10. DATA BUNDLES
        private val DATA_BUNDLES_PATTERN = Regex(
            """(?i)confirmed\.?\s*ksh\.?\s*([\d,]+\.?\d*)\s+sent\s+to\s+SAFARICOM\s+DATA\s+BUNDLES""",
            RegexOption.IGNORE_CASE
        )

        // 11. GLOBAL PAY / VIRTUAL CARD
        private val GLOBAL_PAY_PATTERN = Regex(
            """(?i)confirmed\.?\s*ksh\.?\s*([\d,]+\.?\d*)\s+sent\s+to\s+M-PESA\s+CARD\s+for\s+account\s+(.+?)\s+(\d+).*?on""",
            RegexOption.IGNORE_CASE
        )

        // RECEIPT NUMBER extraction
        private val RECEIPT_PATTERN = Regex(
            """(?i)(?:receipt\s+)?([A-Z0-9]{10,})""",
            RegexOption.IGNORE_CASE
        )

        // MERCHANT NAME extraction (fallback)
        private val MERCHANT_PATTERN = Regex(
            """(?i)(?:paid\s+to|from)\s+(.+?)(?:\s+on\s+|\.)""",
            RegexOption.IGNORE_CASE
        )

        // TRANSACTION COST extraction
        private val TRANSACTION_COST_PATTERN = Regex(
            """(?i)transaction\s+cost[.:,]?\s*ksh\.?\s*([\d,]+\.?\d*)""",
            RegexOption.IGNORE_CASE
        )

        // NEW BALANCE extraction
        private val BALANCE_PATTERN = Regex(
            """(?i)new\s+M-PESA\s+balance\s+is\s+ksh\.?\s*([\d,]+\.?\d*)""",
            RegexOption.IGNORE_CASE
        )

        // DATE extraction
        private val DATE_PATTERN = Regex(
            """(?i)on\s+([\d/]+)\s+at\s+([\d:]+\s+[AP]M)""",
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

        // Try to parse as different transaction types in priority order
        parseWalletTransfer(smsBody)?.let { return it }
        parseMshwariTransfer(smsBody)?.let { return it }
        parseGlobalPay(smsBody)?.let { return it } // Must come before Sent Money
        parseDataBundles(smsBody)?.let { return it }
        parseSentMoney(smsBody)?.let { return it }
        parseReceivedMoney(smsBody)?.let { return it }
        parsePaybill(smsBody)?.let { return it }
        parseTill(smsBody)?.let { return it }
        parseAirtime(smsBody)?.let { return it }
        parseWithdraw(smsBody)?.let { return it }
        parseDeposit(smsBody)?.let { return it }
        parseFuliza(smsBody)?.let { return it }
        parseBuyGoods(smsBody)?.let { return it }

        return null
    }

    private fun parseSentMoney(smsBody: String): ParsedMpesaTransaction? {
        val match = SENT_MONEY_PATTERN.find(smsBody) ?: return null

        val amount = parseAmount(match.groupValues[1])
        val rawName = match.groupValues[2]
        val merchantName = rawName.cleanMerchantName()

        val phoneNumber = match.groupValues.getOrNull(3)?.trim()
        val receipt = extractReceipt(smsBody)
        val transactionCost = extractTransactionCost(smsBody)
        val newBalance = extractBalance(smsBody)
        val smartClues = smartClueDetector.detectClues(merchantName, smsBody)

        return ParsedMpesaTransaction(
            mpesaReceiptNumber = receipt ?: "UNKNOWN",
            amount = amount,
            type = TransactionType.EXPENSE,
            transactionType = MpesaTransactionType.SEND_MONEY,
            merchantName = merchantName,
            phoneNumber = phoneNumber,
            transactionCost = transactionCost,
            newBalance = newBalance,
            smartClues = smartClues
        )
    }

    private fun parseWalletTransfer(smsBody: String): ParsedMpesaTransaction? {
        val match = WALLET_TRANSFER_PATTERN.find(smsBody) ?: return null

        val amount = parseAmount(match.groupValues[1])
        val wallet = match.groupValues[2].uppercase(Locale.ROOT)
        val account = match.groupValues[3]

        val receipt = extractReceipt(smsBody)
        val transactionCost = extractTransactionCost(smsBody)
        val newBalance = extractBalance(smsBody)

        val merchantName = wallet.cleanMerchantName()

        val smartClues = listOf(
            "TRANSFER:WALLET",
            "TRANSFER:$wallet"
        )

        return ParsedMpesaTransaction(
            mpesaReceiptNumber = receipt ?: "UNKNOWN",
            amount = amount,
            type = TransactionType.EXPENSE,
            transactionType = MpesaTransactionType.SEND_MONEY,
            merchantName = merchantName,
            accountNumber = account,
            transactionCost = transactionCost,
            newBalance = newBalance,
            smartClues = smartClues
        )
    }


    private fun parseReceivedMoney(smsBody: String): ParsedMpesaTransaction? {
        // Try the full pattern first (with Account number)
        var match = RECEIVED_MONEY_PATTERN.find(smsBody)

        if (match == null) {
            // Try simple pattern
            match = RECEIVED_MONEY_SIMPLE_PATTERN.find(smsBody) ?: return null
        }

        val amount = parseAmount(match.groupValues[1])
        val rawName = match.groupValues[2]
        val merchantName = rawName.cleanMerchantName()

        val accountNumber = match.groupValues.getOrNull(3)?.trim()
        val phoneNumber = if (accountNumber?.length == 10) accountNumber else null

        val receipt = extractReceipt(smsBody)
        val transactionCost = extractTransactionCost(smsBody)
        val newBalance = extractBalance(smsBody)
        val smartClues = smartClueDetector.detectClues(merchantName, smsBody)

        return ParsedMpesaTransaction(
            mpesaReceiptNumber = receipt ?: "UNKNOWN",
            amount = amount,
            type = TransactionType.INCOME,
            transactionType = MpesaTransactionType.RECEIVE_MONEY,
            merchantName = merchantName,
            phoneNumber = phoneNumber,
            accountNumber = accountNumber,
            transactionCost = transactionCost,
            newBalance = newBalance,
            smartClues = smartClues
        )
    }

    private fun parseMshwariTransfer(smsBody: String): ParsedMpesaTransaction? {
        val match = MSHWARI_TRANSFER_PATTERN.find(smsBody) ?: return null

        val amount = parseAmount(match.groupValues[1])
        val direction = match.groupValues[2].lowercase(Locale.ROOT)

        val receipt = extractReceipt(smsBody)
        val transactionCost = extractTransactionCost(smsBody)
        val newBalance = extractBalance(smsBody)

        val isIncoming = direction == "from"   // from M-Shwari → M-Pesa

        val smartClues = listOf(
            "SAVINGS:MSHWARI",
            if (isIncoming) "TRANSFER:IN" else "TRANSFER:OUT"
        )

        return ParsedMpesaTransaction(
            mpesaReceiptNumber = receipt ?: "UNKNOWN",
            amount = amount,
            type = if (isIncoming) TransactionType.INCOME else TransactionType.EXPENSE,
            transactionType = MpesaTransactionType.SEND_MONEY, // or a new SAVINGS_TRANSFER if you add one
            merchantName = "M-SHWARI",
            transactionCost = transactionCost,
            newBalance = newBalance,
            smartClues = smartClues
        )
    }


    private fun parsePaybill(smsBody: String): ParsedMpesaTransaction? {
        // Skip if it explicitly mentions "Till"
        if (smsBody.contains("till", ignoreCase = true)) {
            return null
        }

        val match = PAYBILL_PATTERN.find(smsBody) ?: return null

        val amount = parseAmount(match.groupValues[1])
        val rawMerchant = match.groupValues[2]
        val merchantName = rawMerchant.cleanMerchantName()

        val accountNumber = match.groupValues.getOrNull(3)?.trim()

        val receipt = extractReceipt(smsBody)
        val transactionCost = extractTransactionCost(smsBody)
        val newBalance = extractBalance(smsBody)
        val smartClues = smartClueDetector.detectClues(merchantName, smsBody)

        // Try to extract paybill number from message
        val paybillNumber = extractPaybillNumber(smsBody, merchantName)

        return ParsedMpesaTransaction(
            mpesaReceiptNumber = receipt ?: "UNKNOWN",
            amount = amount,
            type = TransactionType.EXPENSE,
            transactionType = MpesaTransactionType.PAYBILL,
            merchantName = merchantName,
            paybillNumber = paybillNumber,
            accountNumber = accountNumber,
            transactionCost = transactionCost,
            newBalance = newBalance,
            smartClues = smartClues
        )
    }

    private fun parseTill(smsBody: String): ParsedMpesaTransaction? {
        val match = TILL_PATTERN.find(smsBody) ?: return null

        val amount = parseAmount(match.groupValues[1])
        val tillNumber = match.groupValues[2]

        val receipt = extractReceipt(smsBody)
        val transactionCost = extractTransactionCost(smsBody)
        val newBalance = extractBalance(smsBody)

        // Try to extract merchant name from the message
        val merchantName = extractMerchantName(smsBody) ?: "Till $tillNumber"
        val smartClues = smartClueDetector.detectClues(merchantName, smsBody)

        return ParsedMpesaTransaction(
            mpesaReceiptNumber = receipt ?: "UNKNOWN",
            amount = amount,
            type = TransactionType.EXPENSE,
            transactionType = MpesaTransactionType.TILL,
            merchantName = merchantName,
            tillNumber = tillNumber,
            transactionCost = transactionCost,
            newBalance = newBalance,
            smartClues = smartClues
        )
    }

    private fun parseBuyGoods(smsBody: String): ParsedMpesaTransaction? {
        val match = BUY_GOODS_PATTERN.find(smsBody) ?: return null

        val amount = parseAmount(match.groupValues[1])
        val rawMerchant = match.groupValues[2]
        val merchantName = rawMerchant.cleanMerchantName()

        val receipt = extractReceipt(smsBody)
        val transactionCost = extractTransactionCost(smsBody)
        val newBalance = extractBalance(smsBody)
        val smartClues = smartClueDetector.detectClues(merchantName, smsBody)

        // Buy Goods is essentially a paybill transaction without explicit "paybill" keyword
        // Map it to PAYBILL transaction type
        return ParsedMpesaTransaction(
            mpesaReceiptNumber = receipt ?: "UNKNOWN",
            amount = amount,
            type = TransactionType.EXPENSE,
            transactionType = MpesaTransactionType.PAYBILL,
            merchantName = merchantName,
            transactionCost = transactionCost,
            newBalance = newBalance,
            smartClues = smartClues
        )
    }

    private fun parseAirtime(smsBody: String): ParsedMpesaTransaction? {
        val match = SAFARICOM_AIRTIME_PATTERN.find(smsBody) ?: return null

        val amount = parseAmount(match.groupValues[1])
        val receipt = extractReceipt(smsBody)
        val transactionCost = extractTransactionCost(smsBody)
        val newBalance = extractBalance(smsBody)
        val smartClues = listOf("AIRTIME:AIRTIME")

        return ParsedMpesaTransaction(
            mpesaReceiptNumber = receipt ?: "UNKNOWN",
            amount = amount,
            type = TransactionType.EXPENSE,
            transactionType = MpesaTransactionType.AIRTIME,
            merchantName = "Airtime Purchase",
            transactionCost = transactionCost,
            newBalance = newBalance,
            smartClues = smartClues
        )
    }

    private fun parseWithdraw(smsBody: String): ParsedMpesaTransaction? {
        val match = WITHDRAW_PATTERN.find(smsBody) ?: return null

        val amount = parseAmount(match.groupValues[1])
        val agentInfo = match.groupValues.getOrNull(2)?.trim()

        val receipt = extractReceipt(smsBody)
        val transactionCost = extractTransactionCost(smsBody)
        val newBalance = extractBalance(smsBody)

        return ParsedMpesaTransaction(
            mpesaReceiptNumber = receipt ?: "UNKNOWN",
            amount = amount,
            type = TransactionType.EXPENSE,
            transactionType = MpesaTransactionType.WITHDRAW,
            merchantName = agentInfo?.let { "Agent $it" } ?: "M-Pesa Agent",
            transactionCost = transactionCost,
            newBalance = newBalance,
            smartClues = emptyList()
        )
    }

    private fun parseDeposit(smsBody: String): ParsedMpesaTransaction? {
        val match = DEPOSIT_PATTERN.find(smsBody) ?: return null

        val amount = parseAmount(match.groupValues[1])
        val agentInfo = match.groupValues.getOrNull(2)?.trim()

        val receipt = extractReceipt(smsBody)
        val transactionCost = extractTransactionCost(smsBody)
        val newBalance = extractBalance(smsBody)

        return ParsedMpesaTransaction(
            mpesaReceiptNumber = receipt ?: "UNKNOWN",
            amount = amount,
            type = TransactionType.INCOME,
            transactionType = MpesaTransactionType.DEPOSIT,
            merchantName = agentInfo?.let { "Agent $it" } ?: "M-Pesa Agent",
            transactionCost = transactionCost,
            newBalance = newBalance,
            smartClues = emptyList()
        )
    }

    private fun parseFuliza(smsBody: String): ParsedMpesaTransaction? {
        val match = FULIZA_PATTERN.find(smsBody) ?: return null

        val amount = parseAmount(match.groupValues[1])
        val receipt = extractReceipt(smsBody)
        val transactionCost = extractTransactionCost(smsBody)
        val newBalance = extractBalance(smsBody)

        // Fuliza is a loan/overdraft, so it's technically income (money received)
        // but for a personal finance tracker, you might want to track it as DEPOSIT
        // since it increases your available balance temporarily
        return ParsedMpesaTransaction(
            mpesaReceiptNumber = receipt ?: "UNKNOWN",
            amount = amount,
            type = TransactionType.INCOME, // Money received (even though it's a loan)
            transactionType = MpesaTransactionType.DEPOSIT, // Closest match
            merchantName = "Fuliza M-Pesa",
            transactionCost = transactionCost,
            newBalance = newBalance,
            smartClues = listOf("LOAN:FULIZA")
        )
    }

    private fun parseDataBundles(smsBody: String): ParsedMpesaTransaction? {
        val match = DATA_BUNDLES_PATTERN.find(smsBody) ?: return null

        val amount = parseAmount(match.groupValues[1])
        val receipt = extractReceipt(smsBody)
        val transactionCost = extractTransactionCost(smsBody)
        val newBalance = extractBalance(smsBody)
        val smartClues = listOf("DATA:BUNDLES", "SAFARICOM:DATA")

        return ParsedMpesaTransaction(
            mpesaReceiptNumber = receipt ?: "UNKNOWN",
            amount = amount,
            type = TransactionType.EXPENSE,
            transactionType = MpesaTransactionType.PAYBILL, // Treating as a bill payment
            merchantName = "DATA BUNDLES",
            transactionCost = transactionCost,
            newBalance = newBalance,
            smartClues = smartClues
        )
    }

    private fun parseGlobalPay(smsBody: String): ParsedMpesaTransaction? {
        val match = GLOBAL_PAY_PATTERN.find(smsBody) ?: return null

        val amount = parseAmount(match.groupValues[1])
        val rawMerchant = match.groupValues[2]
        val merchantName = rawMerchant.cleanMerchantName()
        
        // This is usually a reference number or part of the account string
        val refNumber = match.groupValues[3] 

        val receipt = extractReceipt(smsBody)
        val transactionCost = extractTransactionCost(smsBody)
        val newBalance = extractBalance(smsBody)
        val smartClues = smartClueDetector.detectClues(merchantName, smsBody)

        return ParsedMpesaTransaction(
            mpesaReceiptNumber = receipt ?: "UNKNOWN",
            amount = amount,
            type = TransactionType.EXPENSE,
            transactionType = MpesaTransactionType.PAYBILL, // Treat as merchant payment
            merchantName = merchantName,
            accountNumber = refNumber, // Store the number as account number
            transactionCost = transactionCost,
            newBalance = newBalance,
            smartClues = smartClues
        )
    }

    // Helper methods

    private fun parseAmount(amountString: String): Double {
        return amountString.replace(",", "").toDoubleOrNull() ?: 0.0
    }

    private fun extractReceipt(smsBody: String): String? {
        return RECEIPT_PATTERN.findAll(smsBody)
            .map { it.groupValues[1] }
            .firstOrNull { it.length >= 10 && it.matches(Regex("[A-Z0-9]+")) }
    }

    private fun extractMerchantName(smsBody: String): String? {
        return MERCHANT_PATTERN.find(smsBody)?.groupValues?.get(1)?.cleanMerchantName()
    }

    private fun extractPaybillNumber(smsBody: String, merchantName: String): String? {
        // Try to find a number that looks like a paybill (usually 5-7 digits)
        val paybillMatch = Regex("""(?i)paybill\s+(\d{5,7})""").find(smsBody)
        if (paybillMatch != null) {
            return paybillMatch.groupValues[1]
        }

        // Sometimes the merchant name IS the paybill number
        if (merchantName.matches(Regex("""\d{5,7}"""))) {
            return merchantName
        }

        return null
    }

    private fun extractTransactionCost(smsBody: String): Double? {
        val match = TRANSACTION_COST_PATTERN.find(smsBody) ?: return null
        return parseAmount(match.groupValues[1])
    }

    private fun extractBalance(smsBody: String): Double? {
        val match = BALANCE_PATTERN.find(smsBody) ?: return null
        return parseAmount(match.groupValues[1])
    }

    private fun String.cleanMerchantName(): String {
        return this
            .trim()
            // Strip wallet account suffix (e.g. "AIRTEL MONEY for account 2547...")
            .replace(Regex("""\s+for\s+account\s+\d+.*$""", RegexOption.IGNORE_CASE), "")
            // Remove "- AGENT" suffix if present
            .replace(Regex("""\s*-\s*AGENT.*$""", RegexOption.IGNORE_CASE), "")
            // Normalize whitespace
            .replace(Regex("""\s+"""), " ")
            // Remove trailing dots and commas
            .replace(Regex("""[.,]+$"""), "")
            .trim()
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
    val transactionCost: Double? = null,
    val newBalance: Double? = null,
    val smartClues: List<String> = emptyList()
)