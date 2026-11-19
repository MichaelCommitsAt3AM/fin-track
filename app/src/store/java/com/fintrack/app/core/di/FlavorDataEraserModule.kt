package com.fintrack.app.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/** The store flavor has no data beyond the main database. */
class StoreDataEraser @Inject constructor() : FlavorDataEraser {
    override suspend fun eraseFlavorData() = Unit
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FlavorDataEraserModule {
    @Binds
    @Singleton
    abstract fun bindFlavorDataEraser(impl: StoreDataEraser): FlavorDataEraser
}
