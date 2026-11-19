package com.fintrack.app.presentation.transactions

import com.fintrack.app.core.domain.model.Transaction
import com.fintrack.app.core.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionListFilterTest {

    private fun tx(id: String, date: Long, type: TransactionType = TransactionType.EXPENSE, notes: String? = null) =
        Transaction(
            id = id,
            userId = "u1",
            type = type,
            amount = 10.0,
            category = "Food",
            date = date,
            notes = notes,
            paymentMethod = null,
            tags = null
        )

    private val manual = (1..50).map { tx("m$it", date = it * 10L) }
    private val mpesa = (1..50).map { tx("p$it", date = it * 10L + 5, type = TransactionType.INCOME) }

    @Test
    fun `first page holds PAGE_SIZE items, newest first across both sources`() {
        val page = TransactionListFilter.apply(manual, mpesa, "All", "All", "", TransactionListFilter.PAGE_SIZE)

        assertEquals(TransactionListFilter.PAGE_SIZE, page.size)
        assertEquals("p50", page.first().id)
        assertEquals(page.sortedByDescending { it.date }, page)
    }

    @Test
    fun `load more grows the page by PAGE_SIZE`() {
        val limit = TransactionListFilter.PAGE_SIZE * 2
        val page = TransactionListFilter.apply(manual, mpesa, "All", "All", "", limit)

        assertEquals(60, page.size)
    }

    @Test
    fun `type filter applies to merged sources`() {
        val page = TransactionListFilter.apply(manual, mpesa, "Expense", "All", "", 100)

        assertEquals(50, page.size)
        assertEquals(setOf(TransactionType.EXPENSE), page.map { it.type }.toSet())
    }

    @Test
    fun `M-Pesa source is not truncated by the page limit`() {
        val page = TransactionListFilter.apply(manual, mpesa, "All", "M-Pesa", "", TransactionListFilter.PAGE_SIZE)

        assertEquals(50, page.size)
    }

    @Test
    fun `search matches notes and ignores the page limit`() {
        val withNotes = manual + listOf(tx("lunch", date = 1, notes = "Team lunch"))
        val page = TransactionListFilter.apply(withNotes, emptyList(), "All", "All", "lunch", limit = 1)

        assertEquals(listOf("lunch"), page.map { it.id })
    }
}
