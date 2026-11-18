package com.fintrack.app.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionTest {

    private val now = 1_700_000_000_000L

    private fun tx(date: Long, isPlanned: Boolean, amount: Double = 100.0) = Transaction(
        userId = "u1",
        type = TransactionType.EXPENSE,
        amount = amount,
        category = "Housing",
        date = date,
        notes = null,
        paymentMethod = null,
        tags = null,
        isPlanned = isPlanned
    )

    @Test
    fun `future planned transaction has not occurred`() {
        assertFalse(tx(date = now + 1, isPlanned = true).hasOccurred(now))
    }

    @Test
    fun `planned transaction whose date has passed has occurred`() {
        assertTrue(tx(date = now - 1, isPlanned = true).hasOccurred(now))
        assertTrue(tx(date = now, isPlanned = true).hasOccurred(now))
    }

    @Test
    fun `future-dated row without the planned flag still has not occurred`() {
        // e.g. an existing transaction whose date was edited into the future
        assertFalse(tx(date = now + 86_400_000, isPlanned = false).hasOccurred(now))
    }

    @Test
    fun `month total ignores rent planned for later this month`() {
        val month = listOf(
            tx(date = now - 3_600_000, isPlanned = false, amount = 1_200.0), // groceries, earlier today
            tx(date = now + 10 * 86_400_000L, isPlanned = true, amount = 30_000.0) // rent, in 10 days
        )

        val spent = month.filter { it.hasOccurred(now) }.sumOf { it.amount }

        assertEquals(1_200.0, spent, 0.001)
    }
}
