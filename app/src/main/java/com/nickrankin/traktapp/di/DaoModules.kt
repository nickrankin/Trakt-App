package com.nickrankin.traktapp.di

import android.content.Context
import com.nickrankin.traktapp.dao.auth.AuthDatabase
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.images.ImagesDatabase
import com.nickrankin.traktapp.dao.watched.WatchedHistoryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DaoModules {
    @Provides
    fun provideAuthDatabase(@ApplicationContext context: Context): AuthDatabase {
        return AuthDatabase.getDatabase(context)
    }

    @Provides
    fun provideShowsDatabase(@ApplicationContext context: Context): ShowsDatabase {
        return ShowsDatabase.getDatabase(context)
    }

    @Provides
    fun provideWatchedHistoryDatabase(@ApplicationContext context: Context): WatchedHistoryDatabase {
        return WatchedHistoryDatabase.getDatabase(context)
    }

    @Singleton
    @Provides
    fun provideImagesDatabase(@ApplicationContext context: Context): ImagesDatabase {
        return ImagesDatabase.getDatabase(context)
    }
}