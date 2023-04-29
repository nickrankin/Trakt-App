package com.nickrankin.traktapp.ui.movies.moviedetails

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.movies.MovieDetailsViewModel
import com.nickrankin.traktapp.ui.videoservices.VideoServicesFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest

private const val TAG = "MovieVideoServicesFragm"
class MovieVideoServicesFragment: VideoServicesFragment() {
    private val viewModel: MovieDetailsViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getMovie()
        
        getStreamingServices()
    }

    private fun getMovie() {
        lifecycleScope.launchWhenStarted {
            viewModel.movie.collectLatest { movieResource ->
                when(movieResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getMovie: Loading")
                    }
                    is Resource.Success -> {
                        setTrailer(movieResource.data?.trailer)
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "getMovie: Error ${movieResource.error}", )
                    }
                }

            }
        }
    }
    
    private fun getStreamingServices() {
        lifecycleScope.launchWhenStarted {
            viewModel.videoStreamingServices.collectLatest { videoStreamingServices ->
                setVideoServices(videoStreamingServices)

            }
        }
    }
}