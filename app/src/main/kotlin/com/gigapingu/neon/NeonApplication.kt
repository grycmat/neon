package com.gigapingu.neon

import android.app.Application
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import com.gigapingu.neon.core.data.AccountRepository
import com.gigapingu.neon.core.data.AuthRepository
import com.gigapingu.neon.core.data.SearchRepository
import com.gigapingu.neon.core.data.StatusRepository
import com.gigapingu.neon.core.ui.StatusActionService
import com.gigapingu.neon.feature.notifications.NEON_NOTIFICATION_CHANNEL_ID
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NeonApplication : Application(), SingletonImageLoader.Factory {
    @Inject lateinit var statusRepository: StatusRepository
    @Inject lateinit var searchRepository: SearchRepository
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var accountRepository: AccountRepository

    override fun onCreate() {
        super.onCreate()
        StatusActionService.init(this, statusRepository, searchRepository, authRepository, accountRepository)
        createNotificationChannel()
    }

    // Required on minSdk 26+: notify() to a missing channel is silently dropped.
    // Idempotent — creating an existing channel is a no-op.
    private fun createNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(
            NEON_NOTIFICATION_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_DEFAULT
        )
            .setName(getString(R.string.notification_channel_name))
            .setDescription(getString(R.string.notification_channel_description))
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    // Global crossfade so avatars and media fade in instead of popping.
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .crossfade(true)
            .build()
}
