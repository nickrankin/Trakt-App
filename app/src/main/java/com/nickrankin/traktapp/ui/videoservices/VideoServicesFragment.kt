package com.nickrankin.traktapp.ui.videoservices

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.google.android.material.chip.Chip
import com.nickrankin.traktapp.databinding.LayoutWatchVideoServicesBinding
import com.nickrankin.traktapp.model.VideoService
import org.apache.commons.lang3.StringUtils

private const val JUST_WATCH_URL = "https://www.justwatch.com/"
private const val APPLE_URL = "https://www.apple.com"
private const val AMAZON_URL = "https://www.amazon.com"
private const val DISNEY_URL = "https://www.disney.com"
private const val GOOGLE_URL = "https://www.google.com"
private const val NETFLIX_URL = "https://www.netflix.com"
private const val YOUTUBE_URL = "https://www.youtube.com"
private const val TAG = "VideoServicesFragment"
abstract class VideoServicesFragment() : Fragment(), OnSetVideoServiceCallback {

    private var _bindings: LayoutWatchVideoServicesBinding? = null
    private val bindings get() = _bindings!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _bindings = LayoutWatchVideoServicesBinding.inflate(inflater)

        // Inflate the layout for this fragment
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Need to add this to satisfy TMDB API Key rules https://developers.themoviedb.org/3/movies/get-movie-watch-providers
        setupJustWatchedCredits()
    }

    private fun setupJustWatchedCredits() {
        val justWatchedButton = bindings.layoutwatchvideoJwBanner
        val justWatchedUrl = Uri.parse(JUST_WATCH_URL)

        justWatchedButton.setOnClickListener {
            val i = Intent(Intent.ACTION_VIEW, justWatchedUrl)
            startActivity(i)
        }
    }

    override fun setVideoServices(videoServices: List<VideoService>) {

        getSupportedVideoServices(videoServices).groupBy { it.providerType }.map { services ->
            when (services.key) {
                VideoService.TYPE_STREAM -> {
                    // Show the header as we have some streaming services
                    bindings.layoutwatchvideoWatchTitle.visibility = View.VISIBLE

                    services.value.map { service ->
                        handleStreamingChipVisiblity(service.tmdbPoviderId, service.providerTitle)
                    }
                }
                VideoService.TYPE_BUY -> {
                    // Show the header as we have some streaming services
                    bindings.layoutwatchvideoRentTitle.visibility = View.VISIBLE

                    services.value.map { service ->
                        handleRentChipVisibility(service.tmdbPoviderId, service.providerTitle)
                    }
                }
                else -> {

                }
            }
        }
    }

    // Filter out only service that we support in the app
    private fun getSupportedVideoServices(videoService: List<VideoService>): List<VideoService> {
        return videoService.filter { service ->
            service.tmdbPoviderId == VideoService.PROVIDER_APPLE || service.tmdbPoviderId == VideoService.PROVIDER_AMAZON || service.tmdbPoviderId == VideoService.PROVIDER_DISNEY || service.tmdbPoviderId == VideoService.PROVIDER_GOOGLE || service.tmdbPoviderId == VideoService.PROVIDER_NETFLIX || service.tmdbPoviderId == VideoService.PROVIDER_YOUTUBE
        }
    }

    private fun handleStreamingChipVisiblity(providerId: Int, serviceTitle: String) {
        when (providerId) {
            VideoService.PROVIDER_APPLE -> {
                bindings.layoutwatchvideoWatchApple.visibility = View.VISIBLE

                setGenericButtonAction(bindings.layoutwatchvideoWatchApple, providerId, serviceTitle, VideoService.TYPE_STREAM)
            }
            VideoService.PROVIDER_AMAZON -> {
                bindings.layoutwatchvideoWatchAmazon.visibility = View.VISIBLE

                setGenericButtonAction(bindings.layoutwatchvideoWatchAmazon, providerId, serviceTitle, VideoService.TYPE_STREAM)

            }
            VideoService.PROVIDER_DISNEY -> {
                bindings.layoutwatchvideoWatchDisney.visibility = View.VISIBLE

                setGenericButtonAction(bindings.layoutwatchvideoWatchDisney, providerId, serviceTitle, VideoService.TYPE_STREAM)

            }
            VideoService.PROVIDER_GOOGLE -> {
                bindings.layoutwatchvideoWatchGoogle.visibility = View.VISIBLE

                setGenericButtonAction(bindings.layoutwatchvideoWatchGoogle, providerId, serviceTitle, VideoService.TYPE_STREAM)

            }
            VideoService.PROVIDER_NETFLIX -> {
                bindings.layoutwatchvideoWatchNetflix.visibility = View.VISIBLE

                setGenericButtonAction(bindings.layoutwatchvideoWatchNetflix, providerId, serviceTitle, VideoService.TYPE_STREAM)

            }
            VideoService.PROVIDER_YOUTUBE -> {
                bindings.layoutwatchvideoWatchYoutube.visibility = View.VISIBLE

                setGenericButtonAction(bindings.layoutwatchvideoWatchYoutube, providerId, serviceTitle, VideoService.TYPE_STREAM)

            }
            else -> {
                Log.d(TAG, "handleChipVisiblity: Prodiver ID $providerId not supported!")
            }
        }
    }

    private fun handleRentChipVisibility(providerId: Int, serviceTitle: String) {
        when (providerId) {
            VideoService.PROVIDER_APPLE -> {
                bindings.layoutwatchvideoRentApple.visibility = View.VISIBLE

                setGenericButtonAction(bindings.layoutwatchvideoRentApple, providerId, serviceTitle, VideoService.TYPE_BUY)

            }
            VideoService.PROVIDER_AMAZON -> {
                bindings.layoutwatchvideoRentAmazon.visibility = View.VISIBLE

                setGenericButtonAction(bindings.layoutwatchvideoRentAmazon, providerId, serviceTitle, VideoService.TYPE_BUY)

            }
            VideoService.PROVIDER_DISNEY -> {
                bindings.layoutwatchvideoRentDisney.visibility = View.VISIBLE

                setGenericButtonAction(bindings.layoutwatchvideoRentDisney, providerId, serviceTitle, VideoService.TYPE_BUY)

            }
            VideoService.PROVIDER_GOOGLE -> {
                bindings.layoutwatchvideoRentGoogle.visibility = View.VISIBLE

                setGenericButtonAction(bindings.layoutwatchvideoRentGoogle, providerId, serviceTitle, VideoService.TYPE_BUY)

            }
            VideoService.PROVIDER_NETFLIX -> {
                bindings.layoutwatchvideoRentNetflix.visibility = View.VISIBLE

                setGenericButtonAction(bindings.layoutwatchvideoRentNetflix, providerId, serviceTitle, VideoService.TYPE_BUY)

            }
            VideoService.PROVIDER_YOUTUBE -> {
                bindings.layoutwatchvideoRentYoutube.visibility = View.VISIBLE

                setGenericButtonAction(bindings.layoutwatchvideoRentYoutube, providerId, serviceTitle, VideoService.TYPE_BUY)

            }
            else -> {
                Log.d(TAG, "handleChipVisiblity: Prodiver ID $providerId not supported!")
            }
        }
    }

    private fun setGenericButtonAction(chip: Chip, providerId: Int, serviceTitle: String, type: Int) {
        val serviceUrl = when(providerId) {
            VideoService.PROVIDER_APPLE -> {
                Uri.parse(APPLE_URL)
            }
            VideoService.PROVIDER_AMAZON -> {
                Uri.parse(AMAZON_URL)
            }
            VideoService.PROVIDER_DISNEY -> {
                Uri.parse(DISNEY_URL)
            }
            VideoService.PROVIDER_GOOGLE -> {
                Uri.parse(GOOGLE_URL)
            }
            VideoService.PROVIDER_NETFLIX -> {
                Uri.parse(NETFLIX_URL)
            }
            VideoService.PROVIDER_YOUTUBE -> {
                Uri.parse(YOUTUBE_URL)
            }
            else -> {
                null
            }
        }

        chip.setOnClickListener {
           getActionDialog(serviceTitle, serviceUrl, type).show()
        }
    }

    private fun getActionDialog(serviceTitle: String, actionUri: Uri?, type: Int): AlertDialog {
        val actionType = when(type) {
            VideoService.TYPE_BUY -> {
                "purchase"
            }
            VideoService.TYPE_STREAM -> {
                "stream"
            }
            else -> {
                null
            }
        }
        return  AlertDialog.Builder(requireContext())
            .setTitle("${ StringUtils.capitalize(actionType) } on $serviceTitle")
            .setMessage("You can ${actionType?.lowercase()} this content on $serviceTitle!")
            .setPositiveButton("${ StringUtils.capitalize(actionType) } on $serviceTitle", DialogInterface.OnClickListener { dialog, which ->

                val i = Intent(Intent.ACTION_VIEW, actionUri)
                startActivity(i)

                dialog.dismiss()

                true
            })
            .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
              true
            }).create()
    }

    override fun setTrailer(trailerUrl: String?) {
        val trailerButton = bindings.layoutwatchvideoTrailer
        trailerButton.visibility = View.GONE

        if (trailerUrl == null) {
            Log.e(TAG, "setTrailer: No trailer available")
            return
        }

        if (trailerUrl.lowercase().indexOf("youtube.com") == -1) {
            Log.e(TAG, "setTrailer: Youtube Trailers only")
            return
        }

        trailerButton.visibility = View.VISIBLE

        trailerButton.setOnClickListener {
            val trailerIntent = Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl))
            startActivity(trailerIntent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _bindings = null
    }
}


interface OnSetVideoServiceCallback {
    fun setVideoServices(videoServices: List<VideoService>)
    fun setTrailer(trailerUrl: String?)
}