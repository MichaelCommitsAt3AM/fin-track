package com.example.fintrack

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FinTrackApplication : Application() {
    // This class is intentionally empty for now.
    // Its only job is to hold the @HiltAndroidApp annotation
    // which initializes Hilt for the entire app.
}