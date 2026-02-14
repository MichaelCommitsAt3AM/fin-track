package com.fintrack.app.core.worker

import android.content.Context
import javax.inject.Inject

class NoOpWorkerScheduler @Inject constructor() : WorkerScheduler {
    override fun schedule(context: Context) {
        // No-op for store variant
    }
}
