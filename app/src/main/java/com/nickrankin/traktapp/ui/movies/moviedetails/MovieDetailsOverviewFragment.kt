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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.nickrankin.traktapp.adapter.credits.MovieCastCreditsAdapter
import com.nickrankin.traktapp.dao.movies.model.TmMovie
import com.nickrankin.traktapp.databinding.FragmentMovieDetailsOverviewBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.movies.MovieDetailsOverviewViewModel
import com.nickrankin.traktapp.ui.person.PersonActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "MovieDetailsOverviewFra"
@AndroidEntryPoint
class MovieDetailsOverviewFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var bindings: FragmentMovieDetailsOverviewBinding
    private val viewModel: MovieDetailsOverviewViewModel by activityViewModels()

    private lateinit var castRecyclerView: RecyclerView
    private lateinit var castAdapter: MovieCastCreditsAdapter

    @Inject
    lateinit var glide: RequestManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = FragmentMovieDetailsOverviewBinding.inflate(inflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()

    }

    fun initFragment(tmMovie: TmMovie?) {
        if(tmMovie == null) {
            return
        }

        viewModel.onStart()

        getCredits()

        bindMovieData(tmMovie)
    }

    private fun getCredits() {
        lifecycleScope.launchWhenStarted {
            viewModel.credits.collectLatest { creditsResource ->
                when(creditsResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getCredits: Loading Credits")
                    }
                    is Resource.Success -> {

                        castAdapter.submitList(creditsResource.data?.sortedBy { it.movieCastPersonData.ordering })
                        creditsResource.data?.map {
                            Log.d(TAG, "getCredits: ${it.person.name} is order ${it.movieCastPersonData.ordering}")
                        }
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "getCredits: Error getting credits ${creditsResource.error?.message}", )
                        creditsResource.error?.printStackTrace()
                    }
                }
            }
        }
    }

    private fun initRecyclerView() {
        Log.d(TAG, "initRecyclerView: HERE")
        castRecyclerView = bindings.moviedetailsoverviewCastRecycler
        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL

        castAdapter = MovieCastCreditsAdapter(glide) { selectedCredit ->
            val creditIntent = Intent(requireContext(), PersonActivity::class.java)
            creditIntent.putExtra(PersonActivity.PERSON_ID_KEY, selectedCredit.person.trakt_id)

            startActivity(creditIntent)
        }

        castRecyclerView.layoutManager = layoutManager
        castRecyclerView.adapter = castAdapter
    }

    private fun bindMovieData(movie: TmMovie) {
        bindings.apply {
            moviedetailsoverviewMainGroup.visibility = View.VISIBLE
            moviedetailsoverviewOverview.text = movie.overview
        }
    }

    override fun onRefresh() {
        viewModel.onRefresh()
    }

    companion object {

        @JvmStatic
        fun newInstance() =
            MovieDetailsOverviewFragment()

        const val TMDB_ID_KEY = "tmdb_key"
        const val  OVERVIEW_KEY = "overview_key"
    }


}