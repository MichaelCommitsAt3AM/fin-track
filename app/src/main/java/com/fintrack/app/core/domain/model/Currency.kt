package com.fintrack.app.core.domain.model

enum class Currency(val symbol: String, val label: String) {
    KSH("Ksh", "Kenyan Shilling (Ksh)"),
    USD("$", "US Dollar ($)"),
    EUR("€", "Euro (€)");

    companion object {
        fun fromName(name: String?): Currency {
            return entries.find { it.name == name } ?: KSH
        }
    }
}
