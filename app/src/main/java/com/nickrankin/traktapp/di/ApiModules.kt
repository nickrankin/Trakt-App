package com.nickrankin.traktapp.di

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.google.gson.Gson
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.images.ImagesDatabase
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.services.helper.TrackedEpisodeAlarmScheduler
import com.nickrankin.traktapp.services.helper.TrackedEpisodeNotificationsBuilder
import com.uwetrottmann.trakt5.entities.UserSlug
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModules {
    @Singleton
    @Provides
    fun providesTraktApi(@ApplicationContext context: Context): TraktApi {
        return TraktApi(context, true, HttpLoggingInterceptor.Level.BASIC, false)
    }

    @Singleton
    @Provides
    fun provideTmdbApi(): TmdbApi {
        return TmdbApi(false)
    }

    @Singleton
    @Provides
    fun provideGson(): Gson {
        return Gson()
    }

    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Singleton
    @Provides
    fun provideGlide(@ApplicationContext context: Context): RequestManager {
        return Glide.with(context)
    }

    @Singleton
    @Provides
    fun provideUserSlug(): UserSlug {
        return UserSlug("NULL")
    }

    @Singleton
    @Provides
    fun provideImagesLoader(tmdbApi: TmdbApi, imagesDatabase: ImagesDatabase, glide: RequestManager): TmdbImageLoader {
        return TmdbImageLoader(tmdbApi, imagesDatabase, glide)
    }

    @Singleton
    @Provides
    fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager {
        return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    @Singleton
    @Provides
    fun provideTrackedEpisodesAlarmScheduler(@ApplicationContext context: Context, alarmManager: AlarmManager, showsDatabase: ShowsDatabase): TrackedEpisodeAlarmScheduler {
        return TrackedEpisodeAlarmScheduler(context, alarmManager, showsDatabase)
    }

    @Singleton
    @Provides
    fun provideTrackedEpisodeNotificationsBuilder(@ApplicationContext context: Context): TrackedEpisodeNotificationsBuilder {
        return TrackedEpisodeNotificationsBuilder(context)
    }
}