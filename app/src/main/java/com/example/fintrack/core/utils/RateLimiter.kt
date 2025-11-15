package com.example.fintrack.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EmailVerificationRateLimiter {
    private val _lastSentTime = MutableStateFlow(0L)
    private val _attemptCount = MutableStateFlow(0)
    private val _cooldownRemaining = MutableStateFlow(0L)

    val attemptCount: StateFlow<Int> = _attemptCount.asStateFlow()
    val cooldownRemaining: StateFlow<Long> = _cooldownRemaining.asStateFlow()

    companion object {
        private const val BASE_COOLDOWN_MS = 60_000L // 60 seconds
        private const val MAX_ATTEMPTS_PER_SESSION = 5
    }

    fun canSendEmail(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastSent = currentTime - _lastSentTime.value
        val cooldown = calculateCooldown()

        updateCooldownRemaining(currentTime)

        return timeSinceLastSent >= cooldown && _attemptCount.value < MAX_ATTEMPTS_PER_SESSION
    }

    fun recordAttempt() {
        _lastSentTime.value = System.currentTimeMillis()
        _attemptCount.value += 1
    }

    fun getRemainingAttempts(): Int {
        return MAX_ATTEMPTS_PER_SESSION - _attemptCount.value
    }

    fun getCooldownSeconds(): Long {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastSent = currentTime - _lastSentTime.value
        val cooldown = calculateCooldown()
        val remaining = (cooldown - timeSinceLastSent) / 1000
        return if (remaining > 0) remaining else 0
    }

    private fun calculateCooldown(): Long {
        // Exponential backoff: 60s, 120s, 180s, 240s, 300s
        return BASE_COOLDOWN_MS * (_attemptCount.value + 1)
    }

    private fun updateCooldownRemaining(currentTime: Long) {
        val timeSinceLastSent = currentTime - _lastSentTime.value
        val cooldown = calculateCooldown()
        _cooldownRemaining.value = maxOf(0, cooldown - timeSinceLastSent)
    }

    fun reset() {
        _lastSentTime.value = 0L
        _attemptCount.value = 0
        _cooldownRemaining.value = 0L
    }
}
