package com.nickrankin.traktapp.helper

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.uwetrottmann.tmdb2.entities.Videos
import com.uwetrottmann.tmdb2.enumerations.VideoType

private const val TAG = "VideoTrailerHelper"
object VideoTrailerHelper {

    private const val YT_APP_PKG_NAME = "com.google.android.youtube"

    private const val YOUTUBE_SITE_VIDEO_KEY = "YouTube"
    private const val YOUTUBE_URL_PATH = "https://www.youtube.com/watch?v="
    private const val YOUTUBE_APP_INTENT_URI = "vnd.youtube:"

    private const val VIMEO_SITE_KEY = "Vimeo"
    private const val VIMEO_URL_PATH = "https://vimeo.com/"

    fun watchVideoTrailer(context: Context, videos: Videos) {
        val youtubeTrailer = videos.results?.find { video ->
            video.site == YOUTUBE_SITE_VIDEO_KEY && (video.type == VideoType.TEASER || video.type == VideoType.TRAILER)
        }

        val vimeoTrailer = videos.results?.find { video ->
            video.site == VIMEO_SITE_KEY && (video.type == VideoType.TEASER || video.type == VideoType.TRAILER)
        }

        // YouTube is first preference
        if(youtubeTrailer != null) {
            Log.d(TAG, "watchVideoTrailer: Watching trailer on YouTube source (ID: ${youtubeTrailer.key})")
            // Try to open YouTube Trailer in Youtube app (with browser fallback)
            navigateToYoutubeVideo(context, youtubeTrailer.key)
        } else if(vimeoTrailer != null) {
            Log.d(TAG, "watchVideoTrailer: Watching trailer on Video source (ID: ${vimeoTrailer.key})")
            navigateToVimeoVideo(context, vimeoTrailer.key)
        }
    }

    private fun navigateToYoutubeVideo(context: Context, youtubeVideoId: String) {
            try {
               context.packageManager.getApplicationInfo(YT_APP_PKG_NAME, 0)

                // Get this far, its assumed YT app is installed and operational..
                Log.d(TAG, "navigateToYoutubeVideo: Calling YT App with Intent ${YOUTUBE_APP_INTENT_URI + youtubeVideoId}")

                navigateToUri(context, YOUTUBE_APP_INTENT_URI + youtubeVideoId)
            } catch(e: PackageManager.NameNotFoundException) {
                Log.d(TAG, "navigateToVideo: Youtube App Not installed, fall back to browser. ${e.message}")
                navigateToUri(context, YOUTUBE_URL_PATH + youtubeVideoId)
            } catch(e: Exception) {
                Log.e(TAG, "navigateToYoutubeVideo: ${e.message} occurred. Fallback to browser")
                e.printStackTrace()
                navigateToUri(context, YOUTUBE_URL_PATH + youtubeVideoId)
            }
    }

    fun navigateToVideoUrl(context: Context, videoUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
        context.startActivity(intent)
    }

    private fun navigateToVimeoVideo(context: Context, vimeoVideoId: String) {
        navigateToUri(context, VIMEO_URL_PATH + vimeoVideoId)
    }

    private fun navigateToUri(context: Context, uri: String) {
        try {
            Log.d(TAG, "navigateToVideoUrl: Trying to go to web address $uri")
            val videoUrlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))

            context.startActivity(videoUrlIntent)
        } catch(e: Exception) {
            Log.e(TAG, "navigateToVideoUrl: Error navigating to video. $uri. ")
            Log.e(TAG, "navigateToVideoUrl: Error ${e.message}")
            e.printStackTrace()
        }
    }
}