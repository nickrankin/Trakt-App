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
import com.nickrankin.traktapp.adapter.credits.ShowCastCreditsAdapter
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.databinding.FragmentEpisodeDetailsOverviewBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.shows.EpisodeDetailsFragmentsViewModel
import com.nickrankin.traktapp.model.shows.EpisodeDetailsViewModel
import com.nickrankin.traktapp.ui.person.PersonActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "MovieDetailsOverviewFr"

@AndroidEntryPoint
class EpisodeDetailsOverviewFragment : Fragment() {

    private lateinit var bindings: FragmentEpisodeDetailsOverviewBinding
    private val viewModel: EpisodeDetailsFragmentsViewModel by activityViewModels()

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

        initFragment()
        getCredits()

    }

    fun initFragment() {

        lifecycleScope.launchWhenStarted {
            viewModel.episode.collectLatest { episode ->

                if (episode != null) {
                    bindEpisodeData(episode)
                }
            }
        }

    }

    private fun getCredits() {
        viewModel.filterCast(false)

        lifecycleScope.launchWhenStarted {
            viewModel.cast.collectLatest { credits ->
                bindings.showdetailsactivityCrewToggle.visibility = View.VISIBLE
                castAdapter.submitList(credits)
            }
        }
    }

    private fun initRecyclerView() {
        setupCastSwitcher()

        castRecyclerView = bindings.episodedetailsoverviewCastRecycler

        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL

        castAdapter = ShowCastCreditsAdapter(glide) { selectedCastPerson ->
            val castPersonIntent = Intent(requireContext(), PersonActivity::class.java)
            castPersonIntent.putExtra(
                PersonActivity.PERSON_ID_KEY,
                selectedCastPerson.person.trakt_id
            )

            startActivity(castPersonIntent)
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

    companion object {

        @JvmStatic
        fun newInstance() =
            EpisodeDetailsOverviewFragment()
    }


}