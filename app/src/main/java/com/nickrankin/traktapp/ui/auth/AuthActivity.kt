package com.nickrankin.traktapp.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.databinding.ActivityAuthBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject
import android.view.View
import android.webkit.*
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.nickrankin.traktapp.BaseActivity
import com.nickrankin.traktapp.MainActivity
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.TitleHelper
import com.nickrankin.traktapp.model.auth.AuthViewModel
import com.uwetrottmann.trakt5.entities.AccessToken
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.RuntimeException


private const val TAG = "AuthActivity"
@AndroidEntryPoint
class AuthActivity : BaseActivity(), TitleHelper {
    private lateinit var bindings: ActivityAuthBinding
    private lateinit var webView: WebView
    private lateinit var csrfToken: String
    private val stagingOn = false

    private val viewModel by viewModels<AuthViewModel>()

    @Inject
    lateinit var traktApi: TraktApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindings = ActivityAuthBinding.inflate(layoutInflater)

        setSupportActionBar(bindings.toolbarLayout.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setContentView(bindings.root)
        webView = bindings.authactivityWebview
        csrfToken = UUID.randomUUID().toString()

        if(!sharedPreferences.getBoolean(IS_LOGGED_IN, false)) {
            showWebView()
        } else {
            showProfile()
        }

    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showWebView() {
        webView.visibility = View.VISIBLE

        // To allow logout to be success, don't allow caching our responses
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE

        webView.settings.javaScriptEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        webView.webViewClient = WebViewClientDemo()

        if(stagingOn) {
            webView.loadUrl(traktApi.buildStagingAuthorizationUrl(csrfToken))

        } else {
            webView.loadUrl(traktApi.buildAuthorizationUrl(csrfToken))
        }
    }
    
    private fun processAccessToken(code: String?) {
        lifecycleScope.launch {
            viewModel.exchangeCodeForAccessToken(code ?: "", stagingOn).collectLatest { tokenResource ->
                when (tokenResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "processAccessToken: Loading ..." )
                    }
                    is Resource.Success -> {
                        val accessToken = tokenResource.data
                        Log.d(TAG, "processAccessToken: Got ${tokenResource.data?.access_token}", )

                        // Authorize user to the Trakt API
                        traktApi.accessToken(accessToken!!.access_token)
                        Log.e(TAG, "processAccessToken: Access Token is ${traktApi.accessToken()} ", )

                        // Need the Slug to get User data from certain Trakt endpoints
                        getUserSlug(accessToken)
                    }
                    is Resource.Error -> {
                        showErrorSnackbarRetryButton(tokenResource.error, bindings.authactivityFragmentContainer) {
                            viewModel.onRefresh()
                        }
                    }
                }
            }
        }
    }

    private suspend fun getUserSlug(accessToken: AccessToken?) {
        viewModel.getUserSlug().collectLatest { settingsResource ->
            when(settingsResource) {
                is Resource.Loading -> {
                    Log.d(TAG, "getUserProfile: Loading profile")
                }
                is Resource.Success -> {
                    val userSlug = settingsResource.data

                    // Clear cookies used for login
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.removeAllCookies(null)
                    cookieManager.flush()

                    if(userSlug != null) {
                        sharedPreferences.edit()
                            .putString(ACCESS_TOKEN_KEY, accessToken?.access_token)
                            .putString(REFRESH_TOKEN_KEY, accessToken?.refresh_token)
                            .putInt(REFRESH_TOKEN_AT_KEY, accessToken?.expires_in ?: 0)
                            .putString(USER_SLUG_KEY, userSlug)
                            .apply()
                    }

                    finalizeLogin()
                }
                is Resource.Error -> {
                    settingsResource.error?.printStackTrace()
                }
            }
        }
    }

    private fun finalizeLogin() {
        webView.visibility = View.GONE

        sharedPreferences.edit()
            .putBoolean(IS_LOGGED_IN, true)
            .apply()

        restartApp()
    }

    private fun showProfile() {
        webView.visibility = View.GONE
        bindings.authactivityFragmentContainer.visibility = View.VISIBLE

        supportFragmentManager.beginTransaction()
            .replace(bindings.authactivityFragmentContainer.id, AccountFragment.newInstance())
            .commit()
    }

    private fun restartApp() {
        val i = Intent(this, MainActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
    }

    private inner class WebViewClientDemo : WebViewClient() {

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
        }

        @Deprecated("TODO Fix")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if(url.contains("?")) {
                // Fix faulty URI Trakt returns
                val fixedUrl = TraktApi.CALLBACK_URL + url.substring(url.indexOf("?"))
                Log.d(TAG, "shouldOverrideUrlLoading: Stripped Url $fixedUrl", )

                val uri = Uri.parse(fixedUrl)
                val code = uri.getQueryParameter("code") ?: ""

                // Check to see if XSRF token matches and code is not empty
                if(uri.getQueryParameter("state") == csrfToken && code.isNotEmpty()) {
                    processAccessToken(uri.getQueryParameter("code"))

                    return true
                } else if (uri.getQueryParameter("state") != csrfToken) {
                    throw RuntimeException("XSRF Validation failure")
                }
            }
            return false
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        const val IS_LOGGED_IN = "logged_in"
        const val ACCESS_TOKEN_KEY = "access_token"
        const val REFRESH_TOKEN_KEY = "refresh_token"
        const val REFRESH_TOKEN_AT_KEY = "access_token_refresh"
        const val USER_SLUG_KEY = "user_slug"
    }

    override fun setTitle(newTitle: String) {
        supportActionBar?.title = newTitle
    }


}