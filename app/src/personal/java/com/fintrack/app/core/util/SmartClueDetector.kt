package com.fintrack.app.core.util

/**
 * Smart clue detector for M-Pesa transactions.
 * Identifies keywords and patterns in merchant names to help with future categorization.
 */
class SmartClueDetector {
    
    companion object {
        // Category-based keyword mappings
        private val TRANSPORT_KEYWORDS = setOf(
            "UBER", "BOLT", "LITTLE", "MATATU", "BUS", "TAXI", "PETROL", "FUEL",
            "SHELL", "TOTAL", "ASTROL", "LEADWAY", "ENGEN", "KOBIL", "RUBIS", "OLA", "PARKING",
            "NICCO MOVERS", "SUPER METRO"
        )
        
        private val FOOD_KEYWORDS = setOf(
            "RESTAURANT", "CAFE", "COFFEE", "PIZZA", "CHICKEN", "KFC", "SUBWAY", "JAVA",
            "ARTCAFFE", 
            "TUSKYS", "UCHUMI", "GROCERY", "BUTCHERY", "BAKERY", "HOTEL", "LOUNGE"
        )
        
        private val SUPERMARKET_KEYWORDS = setOf(
            "SUPERMARKET", "SUPERMARKETS", "NAIVAS", "CARREFOUR", "QUICK MART", "CHANDARANA", "CLEANSHELF", "MATHAI",
            "UCHUMI"
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
            "JUMIA", "AMAZON", "KILIMALL", "JIJI", "ALIBABA", "EBAY", "ALIEXPRESS", 
            "SHOP", "STORE", "MALL", "BOUTIQUE",
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
        // Split into words for exact matching of single-word keywords
        val words = searchText.split(Regex("[^A-Z]+")).filter { it.isNotEmpty() }.toSet()

        fun checkCategory(keywords: Set<String>, currentCategory: String) {
            keywords.forEach { keyword ->
                // If keyword contains non-letters (e.g. "T-KASH", "NAIROBI WATER"), treat as phrase
                val isPhrase = keyword.any { !it.isLetter() }
                
                if (isPhrase) {
                    if (searchText.contains(keyword)) {
                        clues.add("$currentCategory:$keyword")
                    }
                } else {
                    // Exact word match to avoid "BUS" matching "BUSINESS" or "OLA" matching "NICHOLAS"
                    if (words.contains(keyword)) {
                        clues.add("$currentCategory:$keyword")
                    }
                }
            }
        }
        
        checkCategory(TRANSPORT_KEYWORDS, "TRANSPORT")
        checkCategory(FOOD_KEYWORDS, "FOOD")
        checkCategory(SUPERMARKET_KEYWORDS, "SUPERMARKET")
        checkCategory(UTILITIES_KEYWORDS, "UTILITIES")
        checkCategory(WALLET_KEYWORDS, "TRANSFER")
        checkCategory(ENTERTAINMENT_KEYWORDS, "ENTERTAINMENT")
        checkCategory(HEALTH_KEYWORDS, "HEALTH")
        checkCategory(SHOPPING_KEYWORDS, "SHOPPING")
        checkCategory(AIRTIME_KEYWORDS, "AIRTIME")
        checkCategory(DATA_KEYWORDS, "DATA")
        checkCategory(EDUCATION_KEYWORDS, "EDUCATION")
        
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
