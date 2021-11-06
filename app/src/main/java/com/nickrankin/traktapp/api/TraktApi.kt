package com.nickrankin.traktapp.api

import android.util.Log
import com.nickrankin.traktapp.ApiKeys
import com.nickrankin.traktapp.api.services.TmAuth
import com.nickrankin.traktapp.api.services.TmCalendars
import com.nickrankin.traktapp.api.services.TmShows
import com.nickrankin.traktapp.api.services.TmUsers
import com.uwetrottmann.trakt5.TraktV2
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.UnsupportedEncodingException
import java.lang.StringBuilder
import java.lang.UnsupportedOperationException
import java.net.URLEncoder

private const val TAG = "TraktApi"
class TraktApi(private val loggingOn: Boolean, private val isStaging: Boolean): TraktV2(ApiKeys.TRAKT_API_KEY, ApiKeys.TRAKT_API_SECRET, CALLBACK_URL, isStaging) {
    private var okHttpClient: OkHttpClient? = null
    private var redirectUri: String? = null
    val STAGING_OAUTH2_AUTHORIZATION_URL = "$API_STAGING_URL/oauth/authorize"
    init {
        Log.e(TAG, ": New instance" )
    }

    override fun okHttpClient(): OkHttpClient {
        synchronized(this) {
            if (okHttpClient == null) {
                val builder = OkHttpClient.Builder()
                if (loggingOn) {
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
        logger.setLevel(HttpLoggingInterceptor.Level.BODY)

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

    fun tmUsers(): TmUsers {
        return retrofit().create(TmUsers::class.java)
    }

    fun tmAuth(): TmAuth {
        return retrofit().create(TmAuth::class.java)
    }

    fun tmShows(): TmShows {
        return retrofit().create(TmShows::class.java)
    }

    fun tmCalendars(): TmCalendars {
        return retrofit().create(TmCalendars::class.java)
    }


    companion object {
        const val CALLBACK_URL = "trakt-oauth-callback://"

    }
}