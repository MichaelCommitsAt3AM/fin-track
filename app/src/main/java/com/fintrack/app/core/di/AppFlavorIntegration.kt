package com.fintrack.app.core.di

interface AppFlavorIntegration {
    val supportsMpesa: Boolean
    val isCurrencySelectionEnabled: Boolean
}
