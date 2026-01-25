package com.fintrack.app.core.util

import android.util.Log
import com.fintrack.app.BuildConfig

/**
 * Production-safe logging wrapper for FinTrack.
 * 
 * Features:
 * - Automatically disables debug/verbose logs in release builds
 * - Provides PII sanitization helpers
 * - Centralized logging control
 * - ProGuard will strip debug calls entirely from release APK
 * 
 * Usage:
 * ```
 * AppLogger.d("MyTag", "Debug message")  // Only in debug builds
 * AppLogger.e("MyTag", "Error message", exception)  // Always logged
 * AppLogger.d("MyTag", "User: ${AppLogger.sanitizeUserId(userId)}")  // Sanitized
 * ```
 */
object AppLogger {
    
    private const val MAX_TAG_LENGTH = 23  // Android log tag limit
    
    /**
     * Debug log - only appears in debug builds.
     * Completely stripped from release builds by ProGuard.
     */
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(truncateTag(tag), message)
        }
    }
    
    /**
     * Verbose log - only appears in debug builds.
     * Completely stripped from release builds by ProGuard.
     */
    fun v(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.v(truncateTag(tag), message)
        }
    }
    
    /**
     * Info log - appears in all builds.
     * Use sparingly for important production events.
     */
    fun i(tag: String, message: String) {
        Log.i(truncateTag(tag), message)
    }
    
    /**
     * Warning log - appears in all builds.
     * Use for recoverable errors or unexpected conditions.
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(truncateTag(tag), message, throwable)
        } else {
            Log.w(truncateTag(tag), message)
        }
    }
    
    /**
     * Error log - appears in all builds.
     * Use for errors and exceptions that need investigation.
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(truncateTag(tag), message, throwable)
        } else {
            Log.e(truncateTag(tag), message)
        }
    }
    
    // ========== PII Sanitization Helpers ==========
    
    /**
     * Sanitizes a user ID for logging.
     * Shows first 4 characters + asterisks for debugging while protecting privacy.
     * 
     * Example: "abc123def456" -> "abc1********"
     */
    fun sanitizeUserId(userId: String?): String {
        if (userId == null) return "null"
        if (userId.length <= 4) return "****"
        return "${userId.take(4)}${"*".repeat(8)}"
    }
    
    /**
     * Sanitizes an email address for logging.
     * Shows first character of local part + domain.
     * 
     * Example: "user@example.com" -> "u***@example.com"
     */
    fun sanitizeEmail(email: String?): String {
        if (email == null) return "null"
        val parts = email.split("@")
        if (parts.size != 2) return "***@***"
        
        val localPart = parts[0]
        val domain = parts[1]
        val sanitizedLocal = if (localPart.isNotEmpty()) {
            "${localPart.first()}${"*".repeat(3)}"
        } else {
            "***"
        }
        
        return "$sanitizedLocal@$domain"
    }
    
    /**
     * Sanitizes an authentication token for logging.
     * Only shows if token exists, never the actual value.
     * 
     * Example: "eyJhbGciOiJIUzI1..." -> "[TOKEN_PRESENT]"
     */
    fun sanitizeToken(token: String?): String {
        return if (token.isNullOrBlank()) "[NO_TOKEN]" else "[TOKEN_PRESENT]"
    }
    
    /**
     * Sanitizes financial amounts for logging.
     * Rounds to nearest 100 to prevent exact amount exposure.
     * 
     * Example: 1234.56 -> "~1200"
     */
    fun sanitizeAmount(amount: Double): String {
        val rounded = (amount / 100).toInt() * 100
        return "~$rounded"
    }
    
    /**
     * Generic sanitizer for any sensitive string data.
     * Shows only length and first character.
     * 
     * Example: "SensitiveData123" -> "S*** (len=16)"
     */
    fun sanitize(data: String?): String {
        if (data == null) return "null"
        if (data.isEmpty()) return "[EMPTY]"
        return "${data.first()}*** (len=${data.length})"
    }
    
    // ========== Private Helpers ==========
    
    /**
     * Truncates log tags to Android's 23-character limit.
     */
    private fun truncateTag(tag: String): String {
        return if (tag.length > MAX_TAG_LENGTH) {
            tag.substring(0, MAX_TAG_LENGTH)
        } else {
            tag
        }
    }
}
