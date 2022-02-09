package com.nickrankin.traktapp.api

import com.nickrankin.traktapp.ApiKeys
import com.nickrankin.traktapp.api.services.tmdb.*
import com.uwetrottmann.tmdb2.Tmdb
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class TmdbApi(private val enableLogging: Boolean): Tmdb(ApiKeys.TMDB_API_KEY) {
    private var okHttpClient: OkHttpClient? = null

    @Synchronized
    override fun okHttpClient(): OkHttpClient? {
        if (okHttpClient == null) {
            val builder = OkHttpClient.Builder()
            setOkHttpClientDefaults(builder)

            if(enableLogging) {
                builder.addInterceptor(getLogger())
            }

            okHttpClient = builder.build()
        }
        return okHttpClient
    }

    private fun getLogger(): HttpLoggingInterceptor {
        val logger = HttpLoggingInterceptor()
        logger.setLevel(HttpLoggingInterceptor.Level.HEADERS)

        return logger
    }

    fun tmTvService(): TmTvService {
        return retrofit.create(TmTvService::class.java)
    }

    fun tmTvSeasonService(): TmSeasons {
        return retrofit.create(TmSeasons::class.java)
    }

    fun tmPersonService(): TmPersonService {
        return retrofit.create(TmPersonService::class.java)
    }

    fun tmTvEpisodesService(): TmEpisodes {
        return retrofit.create(TmEpisodes::class.java)
    }

    fun tmSearchService(): SearchService {
        return retrofit.create(SearchService::class.java)
    }
}