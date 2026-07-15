package com.gigapingu.neon.core.database.di

import android.content.Context
import androidx.room.Room
import com.gigapingu.neon.core.database.CacheDao
import com.gigapingu.neon.core.database.NeonDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NeonDatabase =
        Room.databaseBuilder(context, NeonDatabase::class.java, "neon_cache.db").build()

    @Provides
    fun provideCacheDao(db: NeonDatabase): CacheDao = db.cacheDao()
}
