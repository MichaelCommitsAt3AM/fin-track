package com.fintrack.app.presentation.transactions

import com.fintrack.app.core.domain.model.Transaction
import com.fintrack.app.core.domain.model.TransactionType

/**
 * Pure merge/filter/paging logic for the transaction list, kept out of the ViewModel so it
 * can be unit-tested.
 */
object TransactionListFilter {

    /** Items per page: the initial limit, the "load more" increment and the value filters reset to. */
    const val PAGE_SIZE = 30

    /**
     * Combines manual and M-Pesa transactions and applies the source filter ("All", "Manual",
     * "M-Pesa"), the type filter ("All", "Income", "Expense"), the search [query] and the page
     * [limit]. The limit isn't applied while searching or when showing M-Pesa only (that source
     * is already capped upstream).
     */
    fun apply(
        manual: List<Transaction>,
        mpesa: List<Transaction>,
        typeFilter: String,
        sourceFilter: String,
        query: String,
        limit: Int
    ): List<Transaction> {
        val bySource = when (sourceFilter) {
            "Manual" -> manual
            "M-Pesa" -> mpesa
            else -> (manual + mpesa).sortedByDescending { it.date }
        }

        val byType = when (typeFilter) {
            "Income" -> bySource.filter { it.type == TransactionType.INCOME }
            "Expense" -> bySource.filter { it.type == TransactionType.EXPENSE }
            else -> bySource
        }

        val bySearch = if (query.isNotEmpty()) {
            byType.filter { transaction ->
                transaction.notes?.contains(query, ignoreCase = true) == true ||
                    transaction.category.contains(query, ignoreCase = true) ||
                    transaction.paymentMethod?.contains(query, ignoreCase = true) == true
            }
        } else {
            byType
        }

        return if (query.isEmpty() && sourceFilter != "M-Pesa") bySearch.take(limit) else bySearch
    }
}
