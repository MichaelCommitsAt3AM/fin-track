package com.fintrack.app.core.worker

import android.content.Context

interface WorkerScheduler {
    fun schedule(context: Context)
}
