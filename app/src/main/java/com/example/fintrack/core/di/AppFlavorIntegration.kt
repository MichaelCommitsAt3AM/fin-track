package com.example.fintrack.core.di

interface AppFlavorIntegration {
    val supportsMpesa: Boolean
    val isCurrencySelectionEnabled: Boolean
}
