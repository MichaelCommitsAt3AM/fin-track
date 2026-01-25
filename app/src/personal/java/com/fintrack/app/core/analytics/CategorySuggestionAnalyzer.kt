package com.fintrack.app.core.analytics

import com.fintrack.app.core.domain.model.CategorySuggestion
import com.fintrack.app.core.domain.model.MpesaTransaction
import com.fintrack.app.core.util.SmartClueDetector
import javax.inject.Inject

/**
 * Analyzes M-Pesa transactions to suggest categories for auto-creation.
 * Groups transactions by detected smart clues and generates category suggestions.
 */
class CategorySuggestionAnalyzer @Inject constructor(
    private val smartClueDetector: SmartClueDetector
) {
    
    /**
     * Category metadata for suggested categories.
     */
    private data class CategoryMetadata(
        val categoryName: String,
        val iconName: String,
        val colorHex: String
    )
    
    /**
     * Mapping of smart clue categories to UI-friendly category names with icons/colors.
     */
    private val categoryMapping = mapOf(
        "TRANSPORT" to CategoryMetadata("Transport", "directions_car", "#FF6B35"),
        "FOOD" to CategoryMetadata("Food", "restaurant", "#FFA726"),
        "UTILITIES" to CategoryMetadata("Utilities", "bolt", "#42A5F5"),
        "ENTERTAINMENT" to CategoryMetadata("Entertainment", "movie", "#AB47BC"),
        "HEALTH" to CategoryMetadata("Healthcare", "local_hospital", "#EF5350"),
        "SHOPPING" to CategoryMetadata("Online shopping", "shopping_bag", "#EC407A"),
        "SUPERMARKET" to CategoryMetadata("Supermarket", "shopping_cart", "#4CAF50"),
        "AIRTIME" to CategoryMetadata("Airtime & Data", "phone_android", "#26A69A"),
        "DATA" to CategoryMetadata("Airtime & Data", "phone_android", "#26A69A"),
        "EDUCATION" to CategoryMetadata("Education", "school", "#5C6BC0"),
        "TRANSFER" to CategoryMetadata("Transfers", "swap_horiz", "#78909C")
    )
    
    /**
     * Analyze M-Pesa transactions and generate category suggestions.
     * 
     * @param transactions List of M-Pesa transactions to analyze
     * @param minimumTransactionCount Minimum number of transactions required to suggest a category
     * @return List of category suggestions sorted by transaction count (descending)
     */
    fun analyzeSuggestions(
        transactions: List<MpesaTransaction>,
        minimumTransactionCount: Int = 2
    ): List<CategorySuggestion> {
        
        // Group transactions by detected category
        val categoryGroups = mutableMapOf<String, MutableList<MpesaTransaction>>()
        
        transactions.forEach { transaction ->
            // Get the primary category from smart clues
            val category = transaction.smartClues.firstOrNull()?.split(":")?.firstOrNull()
            
            if (category != null && categoryMapping.containsKey(category)) {
                categoryGroups.getOrPut(category) { mutableListOf() }.add(transaction)
            }
        }
        
        // Create suggestions for categories with enough transactions
        val suggestions = categoryGroups
            .filter { (_, transactions) -> transactions.size >= minimumTransactionCount }
            .map { (categoryKey, transactions) ->
                val metadata = categoryMapping[categoryKey]!!
                
                CategorySuggestion(
                    categoryName = metadata.categoryName,
                    iconName = metadata.iconName,
                    colorHex = metadata.colorHex,
                    transactionCount = transactions.size,
                    totalAmount = transactions.sumOf { it.amount },
                    mpesaReceiptNumbers = transactions.map { it.mpesaReceiptNumber }
                )
            }
            .sortedByDescending { it.transactionCount }
        
        return suggestions.mergeDuplicates()
    }
    
    /**
     * Merge duplicate categories (e.g., AIRTIME and DATA both map to "Airtime & Data").
     * Combines transaction counts and receipt numbers.
     */
    private fun List<CategorySuggestion>.mergeDuplicates(): List<CategorySuggestion> {
        val merged = mutableMapOf<String, CategorySuggestion>()
        
        forEach { suggestion ->
            val existing = merged[suggestion.categoryName]
            if (existing != null) {
                // Merge with existing
                merged[suggestion.categoryName] = existing.copy(
                    transactionCount = existing.transactionCount + suggestion.transactionCount,
                    totalAmount = existing.totalAmount + suggestion.totalAmount,
                    mpesaReceiptNumbers = existing.mpesaReceiptNumbers + suggestion.mpesaReceiptNumbers
                )
            } else {
                merged[suggestion.categoryName] = suggestion
            }
        }
        
        return merged.values.toList().sortedByDescending { it.transactionCount }
    }
}
