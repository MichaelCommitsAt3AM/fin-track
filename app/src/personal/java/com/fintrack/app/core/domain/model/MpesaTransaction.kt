package com.fintrack.app.core.domain.model

/**
 * Domain model for M-Pesa transactions.
 * Represents parsed M-Pesa SMS data in the domain layer.
 */
data class MpesaTransaction(
    val smsId: String, // Transport reference only (Android SMS ID)
    val mpesaReceiptNumber: String, // Primary identifier (globally unique)
    val amount: Double,
    val type: TransactionType,
    val merchantName: String?,
    val phoneNumber: String?,
    val paybillNumber: String?,
    val tillNumber: String?,
    val accountNumber: String?,
    val transactionType: MpesaTransactionType,
    val rawBody: String,
    val smartClues: List<String>,
    val parserVersion: Int, // Parser version used (for selective re-parsing)
    val timestamp: Long,
    val createdAt: Long
)

/**
 * Types of M-Pesa transactions based on SMS parsing.
 */
enum class MpesaTransactionType {
    SEND_MONEY,      // Sent money to another person
    RECEIVE_MONEY,   // Received money from another person
    PAYBILL,         // Paid to a paybill number
    TILL,            // Paid to a till number
    AIRTIME,         // Airtime purchase
    WITHDRAW,        // Cash withdrawal
    DEPOSIT,         // Cash deposit
    UNKNOWN          // Unable to parse transaction type
}

/**
 * Lookback period for SMS scanning.
 */
enum class LookbackPeriod(val months: Int) {
    ONE_MONTH(1),
    THREE_MONTHS(3),    // Default
    SIX_MONTHS(6),
    ONE_YEAR(12);
    
    /**
     * Calculate the start timestamp for this lookback period.
     */
    fun getStartTimestamp(currentTime: Long = System.currentTimeMillis()): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = currentTime
        calendar.add(java.util.Calendar.MONTH, -months)
        return calendar.timeInMillis
    }
}
