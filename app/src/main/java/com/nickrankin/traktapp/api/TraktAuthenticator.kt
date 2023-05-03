package com.nickrankin.traktapp.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.nickrankin.traktapp.TmApplication
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.TraktV2
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import javax.inject.Inject

/***
 *
 * This class is a copy of com.uwetrottmann.trakt5.TraktV2Authenticator
 * This customized version will store new tokens into the apps SharedPreferences
 *
 * **/
private const val TAG = "TraktAuthenticator"
class TraktAuthenticator(private val context: Context, private val trakt: TraktV2): Authenticator {

   private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val tmApplication = (context.applicationContext) as TmApplication

    @Throws(IOException::class)
    override fun authenticate(route: Route?, response: Response): Request? {
        Log.e(TAG, "authenticate: Authenticating!=")
        return handleAuthenticate(response, trakt)
    }

    /**
     * If not doing a trakt [TraktV2.API_URL] request tries to refresh the access token with the refresh token.
     *
     * @param response The response passed to [.authenticate].
     * @param trakt The [TraktV2] instance to get the API key from and to set the updated JSON web token on.
     * @return A request with updated authorization header or null if no auth is possible.
     */
    @Throws(IOException::class)
    fun handleAuthenticate(response: Response, trakt: TraktV2): Request? {
        Log.d(TAG, "handleAuthenticate: Refreshing AccessToken")
        if (trakt.apiHost() != response.request.url.host) {
            return null // not a trakt API endpoint (possibly trakt OAuth or other API), give up.
        }
        if (responseCount(response) >= 2) {
            // Force logout of the app if refresh was failed
            tmApplication.logout(true)

            return null // failed 2 times, give up.
        }
        val refreshToken = trakt.refreshToken()
        if (refreshToken == null || refreshToken.length == 0) {
            return null // have no refresh token, give up.
        }

        // try to refresh the access token with the refresh token
        val refreshResponse = trakt.refreshAccessToken(refreshToken)
        val body = refreshResponse.body()
        if (!refreshResponse.isSuccessful || body == null) {
            return null // failed to retrieve a token, give up.
        }

        // store the new tokens
        val accessToken = body.access_token
        trakt.accessToken(accessToken)
        trakt.refreshToken(body.refresh_token)

        sharedPreferences.edit()
            .putString(AuthActivity.ACCESS_TOKEN_KEY, accessToken)
            .putString(AuthActivity.REFRESH_TOKEN_KEY, body.refresh_token)
            .putInt(AuthActivity.REFRESH_TOKEN_AT_KEY, body.expires_in ?: -1)
            .apply()

        Log.d(TAG, "handleAuthenticate: New AccessToken $accessToken")

        // retry request
        return response.request.newBuilder()
            .header(TraktV2.HEADER_AUTHORIZATION, "Bearer $accessToken")
            .build()
    }

    private fun responseCount(response: Response?): Int {
        var result = 1
        while (response?.priorResponse != null) {
            result++
        }
        return result
    }

}