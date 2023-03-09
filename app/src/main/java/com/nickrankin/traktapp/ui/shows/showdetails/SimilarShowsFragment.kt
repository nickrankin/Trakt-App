package com.nickrankin.traktapp.ui.shows.showdetails

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
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.similar.SimilarMoviesAdapter
import com.nickrankin.traktapp.adapter.similar.SimilarShowsAdapter
import com.nickrankin.traktapp.databinding.FragmentSimilarMoviesBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.movies.SimilarMoviesViewModel
import com.nickrankin.traktapp.model.shows.SimilarShowsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "SimilarShowsFragment"
@AndroidEntryPoint
class SimilarShowsFragment : BaseFragment() {

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader
    
    private var _bindings: FragmentSimilarMoviesBinding? = null
    private val bindings get() = _bindings!!

    private var tmdbId: Int = 0
    private var language: String? = null
    private val viewModel: SimilarShowsViewModel by activityViewModels()

    private lateinit var adapter: SimilarShowsAdapter
    private lateinit var recyclerView: RecyclerView
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _bindings = FragmentSimilarMoviesBinding.inflate(inflater)
        
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindings.moviedetailsactivitySimilarTitle.text="Similar Shows"

        initAdapter()
        
        getRecommendations()
    }
    
    private fun getRecommendations() {
        lifecycleScope.launchWhenStarted { 
            val resource = viewModel.getSimilarShows(tmdbId, language)
            
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

        adapter = SimilarShowsAdapter(tmdbImageLoader) { baseShow ->
            navigateToShow(baseShow.id, baseShow.name)
        }

        recyclerView.adapter = adapter

    }

    private fun navigateToShow(tmdbId: Int?, title: String?) {
        if(tmdbId == null) {
            Log.e(TAG, "navigateToMovie: Cannot navigate with null TmdbId", )
            return
        }

        lifecycleScope.launchWhenStarted {
            val traktId = viewModel.getTraktIdFromTmdbId(tmdbId)

            if(traktId != null) {
                val intent = Intent(context, ShowDetailsActivity::class.java)
                intent.putExtra(ShowDetailsActivity.SHOW_DATA_KEY, ShowDataModel(traktId, tmdbId, title))

                startActivity(intent)
            } else {
                Log.e(TAG, "navigateToMovie: Trakt ID is null for movie $title // TMDB ID: $tmdbId, Aborting..", )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _bindings = null
    }

    companion object {
        @JvmStatic
        fun newInstance(tmdbId: Int, movieLanguage: String?) = SimilarShowsFragment()
            .apply { 
                this.tmdbId = tmdbId
                this.language = language
            }
    }
}