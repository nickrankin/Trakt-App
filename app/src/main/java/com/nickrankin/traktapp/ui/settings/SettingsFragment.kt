package com.nickrankin.traktapp.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.ui.auth.AuthActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        handleTraktConnectPreferenceDisplay()
    }

    private fun handleTraktConnectPreferenceDisplay() {
        val traktConnectPreference = findPreference<Preference>("trakt_connect")
        val isLoggedIn = sharedPreferences.getBoolean(AuthActivity.IS_LOGGED_IN, false)
        val userSlug = sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "Unknown")
        if(traktConnectPreference != null) {
            if(isLoggedIn) {
                traktConnectPreference.title = "Trakt Account Connected (${userSlug})"
                traktConnectPreference.summary = "Click to view Profile or logout of your Trakt account."
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when(preference?.key) {
            "trakt_connect" -> {
                val intent = Intent(context, AuthActivity::class.java)
                startActivity(intent)
            }
            else -> {

            }
        }
        return true
    }
}