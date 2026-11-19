package com.fintrack.app.core.domain.util

import com.fintrack.app.core.domain.model.Transaction
import com.fintrack.app.core.domain.model.TransactionType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Pure date/aggregation helpers for weekly and monthly spending.
 *
 * Kept free of Android and repository dependencies so the Home card, the
 * Transactions screen and Reports all compute weeks the same way, and so
 * the logic can be unit tested.
 */
object SpendingPeriods {

    /** First day of the week for the user's locale (e.g. Monday in most of the world). */
    fun localeFirstDayOfWeek(locale: Locale = Locale.getDefault()): DayOfWeek =
        WeekFields.of(locale).firstDayOfWeek

    fun weekStartOf(date: LocalDate, firstDayOfWeek: DayOfWeek): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))

    fun Transaction.localDate(zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(date).atZone(zone).toLocalDate()

    /**
     * Summary for the 7-day week starting at [weekStart]. Days after [today]
     * are reported as future so the UI can render them differently.
     */
    fun weeklySummary(
        transactions: List<Transaction>,
        weekStart: LocalDate,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()
    ): WeeklySpendingSummary {
        val weekEnd = weekStart.plusDays(6)
        val previousStart = weekStart.minusWeeks(1)

        val spentByDay = mutableMapOf<LocalDate, Double>()
        var income = 0.0
        var expenseCount = 0
        var previousTotal = 0.0

        for (tx in transactions) {
            val day = tx.localDate(zone)
            when {
                day in weekStart..weekEnd -> {
                    if (tx.type == TransactionType.EXPENSE) {
                        spentByDay[day] = (spentByDay[day] ?: 0.0) + tx.amount
                        expenseCount++
                    } else {
                        income += tx.amount
                    }
                }
                day >= previousStart && day < weekStart && tx.type == TransactionType.EXPENSE -> {
                    previousTotal += tx.amount
                }
            }
        }

        val days = (0L..6L).map { offset ->
            val day = weekStart.plusDays(offset)
            DaySpending(date = day, amount = spentByDay[day] ?: 0.0, isFuture = day.isAfter(today))
        }

        return WeeklySpendingSummary(
            weekStart = weekStart,
            weekEnd = weekEnd,
            totalSpent = days.sumOf { it.amount },
            totalIncome = income,
            previousWeekSpent = previousTotal,
            expenseCount = expenseCount,
            days = days,
            isCurrentWeek = today in weekStart..weekEnd
        )
    }

    /** Expense totals for [count] consecutive weeks ending with the week starting at [lastWeekStart]. Empty weeks are kept as 0. */
    fun weeklyExpenseTrend(
        transactions: List<Transaction>,
        lastWeekStart: LocalDate,
        count: Int,
        firstDayOfWeek: DayOfWeek,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<PeriodSpending> {
        val firstWeekStart = lastWeekStart.minusWeeks((count - 1).toLong())
        val totals = transactions.asSequence()
            .filter { it.type == TransactionType.EXPENSE }
            .map { weekStartOf(it.localDate(zone), firstDayOfWeek) to it.amount }
            .filter { (week, _) -> week >= firstWeekStart && week <= lastWeekStart }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, amounts) -> amounts.sum() }

        return (0 until count).map { i ->
            val start = firstWeekStart.plusWeeks(i.toLong())
            PeriodSpending(start = start, end = start.plusDays(6), amount = totals[start] ?: 0.0)
        }
    }

    /** Expense totals for [count] consecutive months ending with [lastMonth]. Empty months are kept as 0. */
    fun monthlyExpenseTrend(
        transactions: List<Transaction>,
        lastMonth: YearMonth,
        count: Int,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<PeriodSpending> {
        val firstMonth = lastMonth.minusMonths((count - 1).toLong())
        val totals = transactions.asSequence()
            .filter { it.type == TransactionType.EXPENSE }
            .map { YearMonth.from(it.localDate(zone)) to it.amount }
            .filter { (month, _) -> month >= firstMonth && month <= lastMonth }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, amounts) -> amounts.sum() }

        return (0 until count).map { i ->
            val month = firstMonth.plusMonths(i.toLong())
            PeriodSpending(start = month.atDay(1), end = month.atEndOfMonth(), amount = totals[month] ?: 0.0)
        }
    }

    /** Percentage change from [previous] to [current], or null when there is no baseline to compare against. */
    fun percentChange(current: Double, previous: Double): Double? =
        if (previous <= 0.0) null else (current - previous) / previous * 100
}

data class DaySpending(
    val date: LocalDate,
    val amount: Double,
    val isFuture: Boolean
)

data class WeeklySpendingSummary(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val totalSpent: Double,
    val totalIncome: Double,
    val previousWeekSpent: Double,
    val expenseCount: Int,
    val days: List<DaySpending>,
    val isCurrentWeek: Boolean
) {
    val changePercent: Double? get() = SpendingPeriods.percentChange(totalSpent, previousWeekSpent)

    /** Average per elapsed day, so a half-finished week is not under-reported. */
    val dailyAverage: Double
        get() {
            val elapsed = days.count { !it.isFuture }.coerceAtLeast(1)
            return totalSpent / elapsed
        }

    companion object {
        fun empty(weekStart: LocalDate, today: LocalDate) = WeeklySpendingSummary(
            weekStart = weekStart,
            weekEnd = weekStart.plusDays(6),
            totalSpent = 0.0,
            totalIncome = 0.0,
            previousWeekSpent = 0.0,
            expenseCount = 0,
            days = (0L..6L).map { DaySpending(weekStart.plusDays(it), 0.0, weekStart.plusDays(it).isAfter(today)) },
            isCurrentWeek = today in weekStart..weekStart.plusDays(6)
        )
    }
}

data class PeriodSpending(
    val start: LocalDate,
    val end: LocalDate,
    val amount: Double
)
