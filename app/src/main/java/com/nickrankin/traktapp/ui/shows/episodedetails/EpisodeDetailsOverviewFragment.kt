package com.nickrankin.traktapp.ui.shows.episodedetails

import android.content.Intent
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
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.OnNavigateToEntity
import com.nickrankin.traktapp.adapter.credits.ShowCastCreditsAdapter
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.databinding.FragmentEpisodeDetailsOverviewBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.PersonDataModel
import com.nickrankin.traktapp.model.shows.EpisodeDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "MovieDetailsOverviewFr"

@AndroidEntryPoint
class EpisodeDetailsOverviewFragment : BaseFragment() {

    private var _bindings: FragmentEpisodeDetailsOverviewBinding? = null
    private val bindings get() = _bindings!!

    private val viewModel: EpisodeDetailsViewModel by activityViewModels()

    private lateinit var castRecyclerView: RecyclerView
    private lateinit var castAdapter: ShowCastCreditsAdapter

    @Inject
    lateinit var glide: RequestManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _bindings = FragmentEpisodeDetailsOverviewBinding.inflate(inflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()

        initFragment()
        getCredits()

    }

    fun initFragment() {

        lifecycleScope.launchWhenStarted {
            viewModel.episode.collectLatest { episodeResource ->
                when(episodeResource) {
                    is Resource.Loading -> {

                    }
                    is Resource.Success -> {
                        if (episodeResource.data != null) {
                            bindEpisodeData(episodeResource.data!!)
                        }
                    }
                    is Resource.Error -> TODO()

                }


            }
        }

    }

    private fun getCredits() {
        lifecycleScope.launchWhenStarted {
            viewModel.cast.collectLatest { creditsResource ->

                when(creditsResource) {
                    is Resource.Loading -> {
                        bindings.showdetailsactivityCastProgressbar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        bindings.showdetailsactivityCastProgressbar.visibility = View.GONE
                        bindings.showdetailsactivityCrewToggle.visibility = View.VISIBLE
                        castAdapter.submitList(creditsResource.data)

                    }
                    is Resource.Error -> {

                        bindings.showdetailsactivityCastProgressbar.visibility = View.GONE
                        if(!creditsResource.data.isNullOrEmpty()) {
                            bindings.showdetailsactivityCrewToggle.visibility = View.VISIBLE
                            castAdapter.submitList(creditsResource.data)
                        }

                        handleError(creditsResource.error, null)
                    }
                }
            }
        }
    }

    private fun initRecyclerView() {
        setupCastSwitcher()

        castRecyclerView = bindings.episodedetailsoverviewCastRecycler

        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL

        castAdapter = ShowCastCreditsAdapter(glide) { selectedCastPerson ->

            (activity as OnNavigateToEntity).navigateToPerson(
                PersonDataModel(selectedCastPerson.person_trakt_id, null, null)

            )

        }

        castRecyclerView.layoutManager = layoutManager
        castRecyclerView.adapter = castAdapter
    }

    private fun bindEpisodeData(episode: TmEpisode) {
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

    override fun onDestroyView() {
        super.onDestroyView()

        _bindings = null
    }

    companion object {

        @JvmStatic
        fun newInstance() =
            EpisodeDetailsOverviewFragment()
    }


}