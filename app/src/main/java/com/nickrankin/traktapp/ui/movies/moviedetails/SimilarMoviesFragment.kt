package com.nickrankin.traktapp.ui.movies.moviedetails

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.OnNavigateToEntity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.similar.SimilarMoviesAdapter
import com.nickrankin.traktapp.databinding.FragmentSimilarMoviesBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.movies.SimilarMoviesViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "SimilarMoviesFragment"
@AndroidEntryPoint
class SimilarMoviesFragment : BaseFragment() {

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader
    
    private lateinit var bindings: FragmentSimilarMoviesBinding
    private var tmdbId: Int = 0
    private var language: String? = null
    private val viewModel: SimilarMoviesViewModel by activityViewModels()

    private lateinit var adapter: SimilarMoviesAdapter
    private lateinit var recyclerView: RecyclerView
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindings = FragmentSimilarMoviesBinding.inflate(inflater)
        
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindings.moviedetailsactivitySimilarTitle.text="Similar Movies"

        initAdapter()
        
        getRecommendations()
    }
    
    private fun getRecommendations() {
        lifecycleScope.launchWhenStarted { 
            val resource = viewModel.getSimilarMovies(tmdbId, language)
            
            if(resource is Resource.Success) {
                adapter.submitList(resource.data?.results)
            } else {
                handleError(resource.error, null)
            }
        }
    }

    private fun initAdapter() {
        recyclerView = bindings.moviedetailsactivitySimilarRecyclerview

        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL


        recyclerView.layoutManager = layoutManager

        adapter = SimilarMoviesAdapter(tmdbImageLoader) { baseMovie ->
            navigateToMovie(baseMovie.id, baseMovie.title, baseMovie.release_date?.year)
        }

        recyclerView.adapter = adapter

    }

    private fun navigateToMovie(tmdbId: Int?, movieTitle: String?, movieYear: Int?) {
        if(tmdbId == null) {
            Log.e(TAG, "navigateToMovie: Cannot navigate with null TmdbId")
            return
        }

        lifecycleScope.launchWhenStarted {
            val traktId = viewModel.getTraktIdFromTmdbId(tmdbId)

            if(traktId != null) {
                (activity as OnNavigateToEntity).navigateToMovie(
                    MovieDataModel(traktId, tmdbId, movieTitle, movieYear)
                )
            } else {
                Log.e(TAG, "navigateToMovie: Trakt ID is null for movie $movieTitle // TMDB ID: $tmdbId, Aborting..")
            }


        }
    }

    companion object {
        @JvmStatic
        fun newInstance(tmdbId: Int, movieLanguage: String?) = SimilarMoviesFragment()
            .apply { 
                this.tmdbId = tmdbId
                this.language = language
            }
    }
}