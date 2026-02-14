package com.fintrack.app.core.di

import com.fintrack.app.core.worker.NoOpWorkerScheduler
import com.fintrack.app.core.worker.WorkerScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkerSchedulerModule {

    @Binds
    abstract fun bindWorkerScheduler(
        impl: NoOpWorkerScheduler
    ): WorkerScheduler
}
