package com.fintrack.app.core.domain.util

import com.fintrack.app.core.domain.model.Transaction
import com.fintrack.app.core.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class SpendingPeriodsTest {

    private val zone = ZoneOffset.UTC

    private fun tx(date: LocalDate, amount: Double, type: TransactionType = TransactionType.EXPENSE, hour: Int = 12) =
        Transaction(
            id = "$date-$amount-$type",
            userId = "u",
            type = type,
            amount = amount,
            category = "Food",
            date = date.atTime(hour, 0).toInstant(zone).toEpochMilli(),
            notes = null,
            paymentMethod = null,
            tags = null
        )

    @Test
    fun `week start respects first day of week`() {
        val wednesday = LocalDate.of(2026, 11, 18)
        assertEquals(LocalDate.of(2026, 11, 16), SpendingPeriods.weekStartOf(wednesday, DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 11, 15), SpendingPeriods.weekStartOf(wednesday, DayOfWeek.SUNDAY))
        // A day that is the first day of the week maps to itself
        assertEquals(wednesday.minusDays(2), SpendingPeriods.weekStartOf(wednesday.minusDays(2), DayOfWeek.MONDAY))
    }

    @Test
    fun `weekly summary splits days, income and previous week`() {
        val weekStart = LocalDate.of(2026, 11, 16) // Monday
        val today = LocalDate.of(2026, 11, 18)
        val txs = listOf(
            tx(weekStart, 100.0),
            tx(weekStart, 50.0, hour = 23),
            tx(today, 25.0),
            tx(today, 1000.0, TransactionType.INCOME),
            tx(weekStart.minusDays(1), 70.0), // previous week (Sunday)
            tx(weekStart.minusDays(7), 30.0), // previous week (Monday)
            tx(weekStart.minusDays(8), 999.0), // two weeks ago, ignored
            tx(weekStart.plusDays(7), 999.0) // next week, ignored
        )

        val summary = SpendingPeriods.weeklySummary(txs, weekStart, today, zone)

        assertEquals(175.0, summary.totalSpent, 0.001)
        assertEquals(1000.0, summary.totalIncome, 0.001)
        assertEquals(100.0, summary.previousWeekSpent, 0.001)
        assertEquals(3, summary.expenseCount)
        assertEquals(7, summary.days.size)
        assertEquals(150.0, summary.days[0].amount, 0.001)
        assertEquals(25.0, summary.days[2].amount, 0.001)
        assertFalse(summary.days[2].isFuture)
        assertTrue(summary.days[3].isFuture)
        assertTrue(summary.isCurrentWeek)
        assertEquals(75.0, summary.changePercent!!, 0.001)
        // 3 elapsed days (Mon-Wed)
        assertEquals(175.0 / 3, summary.dailyAverage, 0.001)
    }

    @Test
    fun `change percent is null without a previous baseline`() {
        assertNull(SpendingPeriods.percentChange(50.0, 0.0))
        assertEquals(-50.0, SpendingPeriods.percentChange(50.0, 100.0)!!, 0.001)
    }

    @Test
    fun `weekly trend keeps empty weeks`() {
        val lastWeek = LocalDate.of(2026, 11, 16)
        val txs = listOf(
            tx(lastWeek.plusDays(2), 10.0),
            tx(lastWeek.minusWeeks(2).plusDays(6), 20.0),
            tx(lastWeek.minusWeeks(5), 999.0), // outside window
            tx(lastWeek, 500.0, TransactionType.INCOME) // income ignored
        )

        val trend = SpendingPeriods.weeklyExpenseTrend(txs, lastWeek, 4, DayOfWeek.MONDAY, zone)

        assertEquals(listOf(0.0, 20.0, 0.0, 10.0), trend.map { it.amount })
        assertEquals(lastWeek.minusWeeks(3), trend.first().start)
        assertEquals(lastWeek.plusDays(6), trend.last().end)
    }

    @Test
    fun `monthly trend keeps empty months and crosses year boundary`() {
        val txs = listOf(
            tx(LocalDate.of(2026, 1, 31), 40.0),
            tx(LocalDate.of(2025, 11, 1), 60.0),
            tx(LocalDate.of(2025, 9, 30), 999.0) // outside window
        )

        val trend = SpendingPeriods.monthlyExpenseTrend(txs, YearMonth.of(2026, 1), 4, zone)

        assertEquals(listOf(0.0, 60.0, 0.0, 40.0), trend.map { it.amount })
        assertEquals(LocalDate.of(2025, 10, 1), trend.first().start)
        assertEquals(LocalDate.of(2026, 1, 31), trend.last().end)
    }
}
