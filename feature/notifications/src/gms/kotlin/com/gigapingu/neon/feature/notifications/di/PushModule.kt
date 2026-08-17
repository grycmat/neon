package com.gigapingu.neon.feature.notifications.di

import com.gigapingu.neon.core.data.push.PushEndpointProvider
import com.gigapingu.neon.feature.notifications.GmsPushEndpointProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PushModule {
    @Provides
    @Singleton
    fun providePushEndpointProvider(impl: GmsPushEndpointProvider): PushEndpointProvider = impl
}
