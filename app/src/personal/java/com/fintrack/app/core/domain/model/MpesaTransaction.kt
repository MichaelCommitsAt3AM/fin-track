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
    val transactionCost: Double?, // M-Pesa transaction fee
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
 * How far back to scan the SMS inbox for M-Pesa messages.
 *
 * Single source of truth for the options, their labels and the default:
 * settings, onboarding, the worker and preferences all go through this enum.
 *
 * @param months Number of months to scan, or [ALL_MONTHS] for the whole inbox.
 *               This value is what gets persisted in preferences / worker input.
 */
enum class LookbackPeriod(val months: Int, val label: String) {
    ONE_MONTH(1, "1 month"),
    THREE_MONTHS(3, "3 months"),
    SIX_MONTHS(6, "6 months"),
    ONE_YEAR(12, "1 year"),
    TWO_YEARS(24, "2 years"),
    THREE_YEARS(36, "3 years"),
    ALL_TIME(ALL_MONTHS, "All messages");

    /**
     * Calculate the start timestamp for this lookback period.
     */
    fun getStartTimestamp(currentTime: Long = System.currentTimeMillis()): Long {
        if (this == ALL_TIME) return 0L
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = currentTime
        calendar.add(java.util.Calendar.MONTH, -months)
        return calendar.timeInMillis
    }

    companion object {
        val DEFAULT = ONE_YEAR

        /**
         * Window used by the routine background sync. It only needs to catch messages
         * the real-time receiver missed, so it doesn't rescan the whole lookback period.
         */
        val ROUTINE_SYNC = ONE_MONTH

        /** Maps a persisted month count back to an option, falling back to [DEFAULT] for unknown values. */
        fun fromMonths(months: Int): LookbackPeriod = entries.firstOrNull { it.months == months } ?: DEFAULT
    }
}

/** Persisted value for [LookbackPeriod.ALL_TIME]. */
const val ALL_MONTHS = 0
