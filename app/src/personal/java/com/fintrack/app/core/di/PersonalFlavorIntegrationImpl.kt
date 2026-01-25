package com.fintrack.app.core.di

import javax.inject.Inject

class PersonalFlavorIntegrationImpl @Inject constructor() : AppFlavorIntegration {
    override val supportsMpesa: Boolean = true
    override val isCurrencySelectionEnabled: Boolean = false
}
