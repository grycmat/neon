package com.gigapingu.neon

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import com.gigapingu.neon.core.data.SearchRepository
import com.gigapingu.neon.core.data.StatusRepository
import com.gigapingu.neon.core.ui.StatusActionService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NeonApplication : Application(), SingletonImageLoader.Factory {
    @Inject lateinit var statusRepository: StatusRepository
    @Inject lateinit var searchRepository: SearchRepository

    override fun onCreate() {
        super.onCreate()
        StatusActionService.init(this, statusRepository, searchRepository)
    }

    // Global crossfade so avatars and media fade in instead of popping.
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .crossfade(true)
            .build()
}
