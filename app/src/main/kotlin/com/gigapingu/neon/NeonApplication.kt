package com.gigapingu.neon

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.gigapingu.neon.core.data.AccountRepository
import com.gigapingu.neon.core.data.AuthRepository
import com.gigapingu.neon.core.data.SearchRepository
import com.gigapingu.neon.core.data.StatusRepository
import com.gigapingu.neon.core.ui.StatusActionService
import com.gigapingu.neon.feature.notifications.NEON_NOTIFICATION_CHANNEL_ID
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import okio.Path.Companion.toOkioPath

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

    /** Channel for FCM-delivered Mastodon notifications (minSdk 26, always available). */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NEON_NOTIFICATION_CHANNEL_ID,
            "Notifications",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Mentions, favourites, boosts, follows and more" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    // Global crossfade so avatars and media fade in instead of popping.
    // Cache sizes are set explicitly (rather than left to Coil3 defaults)
    // since this app is image-heavy on every scrolling list (avatars + media
    // grids per row).
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizePercent(0.02)
                    .build()
            }
            .build()
}
