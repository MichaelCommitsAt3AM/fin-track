package com.example.fintrack.core.util

/**
 * Smart clue detector for M-Pesa transactions.
 * Identifies keywords and patterns in merchant names to help with future categorization.
 */
class SmartClueDetector {
    
    companion object {
        // Category-based keyword mappings
        private val TRANSPORT_KEYWORDS = setOf(
            "UBER", "BOLT", "LITTLE", "MATATU", "BUS", "TAXI", "PETROL", "FUEL",
            "SHELL", "TOTAL", "ASTROL", "LEADWAY", "ENGEN", "KOBIL", "RUBIS", "OLA", "PARKING"
        )
        
        private val FOOD_KEYWORDS = setOf(
            "RESTAURANT", "CAFE", "COFFEE", "PIZZA", "CHICKEN", "KFC", "SUBWAY", "JAVA",
            "ARTCAFFE", "NAIVAS", "CARREFOUR", "QUICKMART", "CHANDARANA",
            "TUSKYS", "UCHUMI", "SUPERMARKET", "GROCERY", "BUTCHERY"
        )
        
        private val UTILITIES_KEYWORDS = setOf(
            "KPLC", "POWER", "ELECTRICITY", "WATER", "NAIROBI WATER",
            "DSTV", "GOTV", "ZUKU", "TELKOM", "FAIBA",
            "INTERNET", "WIFI"
        )

        private val WALLET_KEYWORDS = setOf(
            "AIRTEL MONEY", "T-KASH", "MTN MONEY", "ORANGE MONEY", "PAYPAL"
        )


        private val ENTERTAINMENT_KEYWORDS = setOf(
            "CINEMA", "MOVIE", "IMAX", "NETFLIX", "SPOTIFY", "SHOWMAX",
            "GYM", "FITNESS", "CLUB", "BAR", "PUB"
        )
        
        private val HEALTH_KEYWORDS = setOf(
            "HOSPITAL", "CLINIC", "PHARMACY", "MEDICAL", "DOCTOR",
            "DENTIST", "LABORATORY", "LAB", "NHIF"
        )
        
        private val SHOPPING_KEYWORDS = setOf(
            "JUMIA", "AMAZON", "SHOP", "SUPERMARKET", "SUPERMARKETS", "STORE", "MALL", "BOUTIQUE",
            "FASHION", "CLOTHING", "SHOES"
        )
        
        private val AIRTIME_KEYWORDS = setOf(
            "AIRTIME", "SAFARICOM AIRTIME", "AIRTEL AIRTIME"
        )

        private val DATA_KEYWORDS = setOf(
            "SAFARICOM DATA BUNDLES", "AIRTEL DATA BUNDLES"
        )
        
        private val EDUCATION_KEYWORDS = setOf(
            "SCHOOL", "UNIVERSITY", "COLLEGE", "COURSERA", "UDEMY",
            "TRAINING", "TUITION", "BOOK"
        )
        
        // Transaction type specific keywords
        private val PAYBILL_INDICATORS = setOf(
            "PAYBILL", "BUSINESS NUMBER", "BUY GOODS"
        )
        
        private val TILL_INDICATORS = setOf(
            "TILL NUMBER", "TILL NO", "MERCHANT"
        )
        
        private val TRANSFER_INDICATORS = setOf(
            "SENT TO", "RECEIVED FROM", "TRANSFER"
        )
    }
    
    /**
     * Detect smart clues from merchant name and SMS body.
     * 
     * @param merchantName The cleaned merchant name
     * @param smsBody The full SMS body
     * @return List of detected keyword clues
     */
    fun detectClues(merchantName: String?, smsBody: String?): List<String> {
        val clues = mutableSetOf<String>()
        val searchText = "${merchantName?.uppercase() ?: ""} ${smsBody?.uppercase() ?: ""}"
        
        // Detect category keywords
        TRANSPORT_KEYWORDS.forEach { keyword ->
            if (searchText.contains(keyword)) {
                clues.add("TRANSPORT:$keyword")
            }
        }
        
        FOOD_KEYWORDS.forEach { keyword ->
            if (searchText.contains(keyword)) {
                clues.add("FOOD:$keyword")
            }
        }
        
        UTILITIES_KEYWORDS.forEach { keyword ->
            if (searchText.contains(keyword)) {
                clues.add("UTILITIES:$keyword")
            }
        }

        WALLET_KEYWORDS.forEach { keyword ->
            if (searchText.contains(keyword)) {
                clues.add("TRANSFER:$keyword")
            }
        }


        ENTERTAINMENT_KEYWORDS.forEach { keyword ->
            if (searchText.contains(keyword)) {
                clues.add("ENTERTAINMENT:$keyword")
            }
        }
        
        HEALTH_KEYWORDS.forEach { keyword ->
            if (searchText.contains(keyword)) {
                clues.add("HEALTH:$keyword")
            }
        }
        
        SHOPPING_KEYWORDS.forEach { keyword ->
            if (searchText.contains(keyword)) {
                clues.add("SHOPPING:$keyword")
            }
        }
        
        AIRTIME_KEYWORDS.forEach { keyword ->
            if (searchText.contains(keyword)) {
                clues.add("AIRTIME:$keyword")
            }
        }

        DATA_KEYWORDS.forEach { keyword ->
            if (searchText.contains(keyword)) {
                clues.add("DATA:$keyword")
            }
        }

        EDUCATION_KEYWORDS.forEach { keyword ->
            if (searchText.contains(keyword)) {
                clues.add("EDUCATION:$keyword")
            }
        }
        
        return clues.toList()
    }
    
    /**
     * Suggest a category based on detected clues.
     * This can be used for auto-categorization or suggestions.
     */
    fun suggestCategory(clues: List<String>): String? {
        if (clues.isEmpty()) return null
        
        val categoryCount = mutableMapOf<String, Int>()
        
        clues.forEach { clue ->
            val category = clue.split(":").firstOrNull()
            if (category != null) {
                categoryCount[category] = (categoryCount[category] ?: 0) + 1
            }
        }
        
        return categoryCount.maxByOrNull { it.value }?.key
    }
}
