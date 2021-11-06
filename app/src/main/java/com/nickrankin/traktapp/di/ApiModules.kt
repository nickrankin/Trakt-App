package com.nickrankin.traktapp.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.ApiKeys
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.images.ImagesDatabase
import com.nickrankin.traktapp.helper.PosterImageLoader
import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.entities.UserSlug
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModules {
    @Singleton
    @Provides
    fun providesTraktApi(): TraktApi {
        return TraktApi(true, false)
    }

    @Singleton
    @Provides
    fun provideTmdbApi(): TmdbApi {
        return TmdbApi(true)
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
    fun providePosterImageLoader(tmdbApi: TmdbApi, imagesDatabase: ImagesDatabase): PosterImageLoader {
        return PosterImageLoader(tmdbApi, imagesDatabase)
    }
}