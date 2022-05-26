package com.nickrankin.traktapp.ui.shows.episodedetails

import android.graphics.Typeface
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
import com.nickrankin.traktapp.adapter.credits.ShowCastCreditsAdapter
import com.nickrankin.traktapp.api.services.trakt.TmEpisodes
import com.nickrankin.traktapp.dao.movies.model.TmMovie
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.databinding.FragmentEpisodeDetailsOverviewBinding
import com.nickrankin.traktapp.databinding.FragmentMovieDetailsOverviewBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.movies.MovieDetailsOverviewViewModel
import com.nickrankin.traktapp.model.shows.episodedetails.EpisodeDetailsOverviewViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "MovieDetailsOverviewFr"
@AndroidEntryPoint
class EpisodeDetailsOverviewFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var bindings: FragmentEpisodeDetailsOverviewBinding
    private val viewModel: EpisodeDetailsOverviewViewModel by activityViewModels()

    private lateinit var castRecyclerView: RecyclerView
    private lateinit var castAdapter: ShowCastCreditsAdapter

    @Inject
    lateinit var glide: RequestManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = FragmentEpisodeDetailsOverviewBinding.inflate(inflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()

    }

    fun initFragment(tmEpisode: TmEpisode?) {
        if(tmEpisode == null) {
            return
        }

        viewModel.onStart()

        getCredits()

        bindMovieData(tmEpisode)
    }

    private fun getCredits() {
        viewModel.filterCast(false)

        lifecycleScope.launchWhenStarted {
            viewModel.cast.collectLatest { creditsResource ->
                when(creditsResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getCredits: Loading Credits")
                    }
                    is Resource.Success -> {
                        if(creditsResource.data?.isNotEmpty() == true) {
                            val castPeople = creditsResource.data

                            if(castPeople != null) {
                                bindings.showdetailsactivityCrewToggle.visibility = View.VISIBLE
                                } else {
                                    bindings.showdetailsactivityCrewToggle.visibility = View.GONE
                                }

                        }

                        Log.d(TAG, "getCredits: Got ${creditsResource.data?.size} Credits")
                        castAdapter.submitList(creditsResource.data)
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
        setupCastSwitcher()

        castRecyclerView = bindings.episodedetailsoverviewCastRecycler
        val layoutManager = FlexboxLayoutManager(requireContext())
        layoutManager.flexDirection = FlexDirection.ROW
        layoutManager.flexWrap = FlexWrap.NOWRAP
        castAdapter = ShowCastCreditsAdapter(glide)

        castRecyclerView.layoutManager = layoutManager
        castRecyclerView.adapter = castAdapter
    }

    private fun bindMovieData(episode: TmEpisode) {
        bindings.apply {
            episodedetailsoverviewMainGroup.visibility = View.VISIBLE
            episodedetailsoverviewOverview.text = episode.overview
        }
    }

    private fun setupCastSwitcher() {

        bindings.apply {
            val regularCastButton = showdetailsactivityCastRegularButton
            val guestStarsButton = showdetailsactivityCastGuestButton


            regularCastButton.text = "Season Regulars"
            guestStarsButton.text = "Guest Stars"

            regularCastButton.setOnClickListener {
                regularCastButton.setTypeface(null, Typeface.BOLD)
                guestStarsButton.setTypeface(null, Typeface.NORMAL)

                viewModel.filterCast(false)
            }

            guestStarsButton.setOnClickListener {
                regularCastButton.setTypeface(null, Typeface.NORMAL)
                guestStarsButton.setTypeface(null, Typeface.BOLD)

                viewModel.filterCast(true)
            }
        }
    }

    override fun onRefresh() {
        viewModel.onRefresh()
    }

    companion object {

        @JvmStatic
        fun newInstance() =
            EpisodeDetailsOverviewFragment()
    }


}