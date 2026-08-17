package com.gigapingu.neon.update.di

import com.gigapingu.neon.update.AppUpdateController
import com.gigapingu.neon.update.NoOpAppUpdateController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UpdateModule {
    @Provides
    @Singleton
    fun provideAppUpdateController(impl: NoOpAppUpdateController): AppUpdateController = impl
}
