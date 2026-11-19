package com.fintrack.app.core.di

/** Flavor-specific local data that must go with the rest when the user erases the app's data. */
interface FlavorDataEraser {
    suspend fun eraseFlavorData()
}
