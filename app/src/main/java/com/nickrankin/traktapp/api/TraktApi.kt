package com.nickrankin.traktapp.api

import android.content.Context
import android.util.Log
import com.nickrankin.traktapp.ApiKeys
import com.nickrankin.traktapp.api.services.trakt.*
import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.TraktV2Authenticator
import com.uwetrottmann.trakt5.TraktV2Interceptor
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.TimeUnit

private const val TAG = "TraktApi"

class TraktApi(
    private val context: Context,
    private val loggingOn: Boolean,
    private val loggerLevel: HttpLoggingInterceptor.Level,
    private val isStaging: Boolean
) : TraktV2(ApiKeys.TRAKT_API_KEY, ApiKeys.TRAKT_API_SECRET, CALLBACK_URL, isStaging) {
    private var okHttpClient: OkHttpClient? = null
    private var redirectUri: String? = null
    val STAGING_OAUTH2_AUTHORIZATION_URL = "$API_STAGING_URL/oauth/authorize"

    init {
        Log.e(TAG, ": New instance")
    }

    override fun okHttpClient(): OkHttpClient {
        synchronized(this) {
            if (okHttpClient == null) {
                val builder = OkHttpClient.Builder()
                    .retryOnConnectionFailure(true)
                    .pingInterval(8, TimeUnit.SECONDS)
                    .connectTimeout(9, TimeUnit.SECONDS)
                    .readTimeout(7, TimeUnit.SECONDS)
                    .writeTimeout(7, TimeUnit.SECONDS)
                    .connectionPool(ConnectionPool(1, 25, TimeUnit.SECONDS))
                if (loggingOn) {
                    builder.eventListener(OkHttpPerformanceEventListener())
                    builder.addInterceptor(getLogger())
                }
                setOkHttpClientDefaults(builder)
                okHttpClient = builder.build()
            }
            return okHttpClient!!
        }
    }

    private fun getLogger(): HttpLoggingInterceptor {
        val logger = HttpLoggingInterceptor()
        logger.setLevel(loggerLevel)

        return logger
    }

    fun buildStagingAuthorizationUrl(state: String): String {
        this.redirectUri = CALLBACK_URL

        checkNotNull(redirectUri) { "redirectUri not provided" }
        val authUrl = StringBuilder(STAGING_OAUTH2_AUTHORIZATION_URL)
        authUrl.append("?").append("response_type=code")
        authUrl.append("&").append("redirect_uri=").append(urlEncode(redirectUri!!))
        authUrl.append("&").append("state=").append(urlEncode(state))
        authUrl.append("&").append("client_id=").append(urlEncode(apiKey()))
        return authUrl.toString()
    }

    private fun urlEncode(content: String): String? {
        return try {
            // can not use java.nio.charset.StandardCharsets as on Android only available since API 19
            URLEncoder.encode(content, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            throw UnsupportedOperationException(e)
        }
    }

    /**
     * Adds [TraktV2Interceptor] as an application interceptor and [TraktV2Authenticator] as an authenticator.
     */
    override fun setOkHttpClientDefaults(builder: OkHttpClient.Builder) {
        builder.addInterceptor(TraktV2Interceptor(this))
        builder.authenticator(TraktAuthenticator(context, this))
//        builder.authenticator(com.uwetrottmann.trakt5.TraktV2Authenticator(this))

    }


    fun tmCheckin(): TmCheckin {
        return retrofit().create(TmCheckin::class.java)
    }

    fun tmUsers(): TmUsers {
        return retrofit().create(TmUsers::class.java)
    }

    fun tmRecommendations(): TmRecommendations {
        return retrofit().create(TmRecommendations::class.java)
    }

    fun tmAuth(): TmAuth {
        return retrofit().create(TmAuth::class.java)
    }

    fun tmSearch(): TmSearch {
        return retrofit().create(TmSearch::class.java)
    }

    fun tmShows(): TmShows {
        return retrofit().create(TmShows::class.java)
    }

    fun tmMovies(): TmMovies {
        return retrofit().create(TmMovies::class.java)
    }

    fun tmSeasons(): TraktSeasons {
        return retrofit().create(TraktSeasons::class.java)
    }

    fun tmPeople(): TmPerson {
        return retrofit().create(TmPerson::class.java)
    }

    fun tmEpisodes(): TmEpisodes {
        return retrofit().create(TmEpisodes::class.java)
    }

    fun tmCalendars(): TmCalendars {
        return retrofit().create(TmCalendars::class.java)
    }

    fun tmSync(): TmSync {
        return retrofit().create(TmSync::class.java)
    }


    companion object {
        const val CALLBACK_URL = "trakt-oauth-callback://"

    }
}