package com.nickrankin.traktapp.helper

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

private const val TAG = "ImdbNavigationHelper"
object ImdbNavigationHelper {
    private const val IMDB_APP_ID = "com.imdb.mobile"
    private const val IMDB_APP_URI = "imdb:///title/"
    private const val IMDB_URL_PATH = "https://www.imdb.com/title/"

    fun navigateToImdb(context: Context, imdbId: String) {
        Log.d(TAG, "navigateToImdb: Navigating to IMDB for ID: $imdbId")
        try {
            context.packageManager.getApplicationInfo(IMDB_APP_ID, 0)

            // If we get this far without any Exception thrown, assume user has IMDB App installed
            navigateToUri(context, IMDB_APP_URI + imdbId)
        } catch(e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "navigateToImdb: IMDB App Not Installed, Fallback to using browser")
            navigateToUri(context, IMDB_URL_PATH + imdbId)

        } catch(e: Exception) {
            Log.e(TAG, "navigateToImdb: An error occurred ${e.message}. Fallback to using browser default https:// protocol handler", )
            navigateToUri(context, IMDB_URL_PATH + imdbId)
        }

    }

    private fun navigateToUri(context: Context, uri: String) {
        try {
            Log.d(TAG, "navigateToUrl: Trying to open IMDB URI: $uri")
            val imdbIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))

            context.startActivity(imdbIntent)

        } catch (e: Exception) {
            Log.e(TAG, "navigateToUrl: Error navigating to IMDB. URL Given $uri", )
            e.printStackTrace()
        }
    }
}