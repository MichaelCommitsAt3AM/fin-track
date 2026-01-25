package com.fintrack.app.core.di

import javax.inject.Inject

class StoreFlavorIntegrationImpl @Inject constructor() : AppFlavorIntegration {
    override val supportsMpesa: Boolean = false
    override val isCurrencySelectionEnabled: Boolean = true
}
