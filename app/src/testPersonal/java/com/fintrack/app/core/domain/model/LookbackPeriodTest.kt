package com.fintrack.app.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class LookbackPeriodTest {

    @Test
    fun `default is one year`() {
        assertEquals(LookbackPeriod.ONE_YEAR, LookbackPeriod.DEFAULT)
    }

    @Test
    fun `persisted month counts round trip`() {
        LookbackPeriod.entries.forEach { period ->
            assertEquals(period, LookbackPeriod.fromMonths(period.months))
        }
    }

    @Test
    fun `unknown persisted value falls back to default`() {
        assertEquals(LookbackPeriod.DEFAULT, LookbackPeriod.fromMonths(5))
        assertEquals(LookbackPeriod.DEFAULT, LookbackPeriod.fromMonths(-1))
    }

    @Test
    fun `legacy stored values still map to their option`() {
        // Users who picked an option before the longer ones existed keep their choice
        assertEquals(LookbackPeriod.THREE_MONTHS, LookbackPeriod.fromMonths(3))
        assertEquals(LookbackPeriod.ONE_YEAR, LookbackPeriod.fromMonths(12))
    }

    @Test
    fun `all time scans from the epoch`() {
        assertEquals(0L, LookbackPeriod.ALL_TIME.getStartTimestamp())
    }

    @Test
    fun `two years starts 24 months back`() {
        val now = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 18, 12, 0, 0) }.timeInMillis
        val start = Calendar.getInstance().apply { timeInMillis = LookbackPeriod.TWO_YEARS.getStartTimestamp(now) }
        assertEquals(2024, start.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, start.get(Calendar.MONTH))
        assertTrue(start.timeInMillis < now)
    }
}
