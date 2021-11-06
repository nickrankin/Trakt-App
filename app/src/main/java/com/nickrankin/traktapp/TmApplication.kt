package com.nickrankin.traktapp

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.auth.AuthDatabase
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.UserSlug
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

private const val TAG = "TmApplication"
@HiltAndroidApp
class TmApplication: Application() {
    @Inject
    lateinit var traktApi: TraktApi

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var userSlug: UserSlug

    override fun onCreate() {
        super.onCreate()
        authenticate()
    }

    fun authenticate() {
        val accessToken = sharedPreferences.getString(AuthActivity.ACCESS_TOKEN_KEY, "")
        val refreshToken = sharedPreferences.getString(AuthActivity.REFRESH_TOKEN_KEY, "")
        val slug = sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "NULL")

        if(accessToken?.isNotEmpty() == true) {
            Log.d(TAG, "authenticate: User is authenticated (access token ${accessToken})")

            updateLoginState(true)

            traktApi.accessToken(accessToken)
            traktApi.refreshToken(refreshToken)
        } else {

            updateLoginState(false)

            sharedPreferences.edit()
                .putBoolean(AuthActivity.IS_LOGGED_IN, false)
                .apply()
        }

        userSlug = UserSlug(slug)
    }

    fun logout(): Boolean {

        sharedPreferences.edit()
            .remove(AuthActivity.ACCESS_TOKEN_KEY)
            .remove(AuthActivity.REFRESH_TOKEN_KEY)
            .remove(AuthActivity.REFRESH_TOKEN_AT_KEY)
            .remove(AuthActivity.USER_SLUG_KEY)
            .apply()

        traktApi.accessToken("")
        traktApi.refreshToken("")

        updateLoginState(false)

        Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_LONG).show()

        return true
    }

    private fun updateLoginState(isLoggedIn: Boolean) {
        Log.d(TAG, "updateLoginState: Logon state updated. New state: ${isLoggedIn}")
        sharedPreferences.edit()
            .putBoolean(AuthActivity.IS_LOGGED_IN, isLoggedIn)
            .apply()
    }
}