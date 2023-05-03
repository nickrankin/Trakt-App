package com.nickrankin.traktapp.ui.shows

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.viewModels
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.BaseActivity
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.OnNavigateToEntity
import com.nickrankin.traktapp.adapter.shows.EpisodesAdapter
import com.nickrankin.traktapp.dao.show.TmSeasonAndStats
import com.nickrankin.traktapp.databinding.ActivitySeasonEpisodesBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.OnTitleChangeListener
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.datamodel.SeasonDataModel
import com.nickrankin.traktapp.model.shows.SeasonEpisodesViewModel
import com.nickrankin.traktapp.repo.shows.episodedetails.EpisodeDetailsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

private const val SELECTED_SEASON = "selected_season"
private const val TAG = "SeasonEpisodesFragment"

@AndroidEntryPoint
class SeasonEpisodesFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener,
    OnNavigateToEpisode {
    private var _bindings: ActivitySeasonEpisodesBinding? = null
    private val bindings get() = _bindings!!

    private val viewModel: SeasonEpisodesViewModel by viewModels()

    private lateinit var progressBar: ProgressBar

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EpisodesAdapter


    private var showTitle = "Show"
    private var seasonTitle = "Season Episodes"
    private val seasonIntro = "Season - "

    private lateinit var seasonSpinner: Spinner

    @Inject
    lateinit var glide: RequestManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _bindings = ActivitySeasonEpisodesBinding.inflate(layoutInflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = bindings.seasonepisodesactivityProgressbar


        if (arguments?.containsKey(SEASON_DATA_KEY) == false || arguments?.getParcelable<SeasonDataModel>(
                SEASON_DATA_KEY
            ) == null
        ) {
            throw RuntimeException("Error loading Season Data. SeasonDataModel must be included with Intent!")
        }

        initRecycler()
        initSeasonSpinner()

        getShow()
        getSeasons()
        getSelectedSeason()
        getEpisodes()

    }

    private fun getShow() {
        lifecycleScope.launchWhenStarted {
            viewModel.show.collectLatest { showResource ->
                val show = showResource.data
                when (showResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getShow: Loading show ..")
                    }
                    is Resource.Success -> {
                        if (show != null) {
                            showTitle = show.name

                        }
                    }
                    is Resource.Error -> {

                        if (show != null) {
                            showTitle = show.name

                        }

                        showErrorSnackbarRetryButton(showResource.error, bindings.root) {
                            viewModel.onRefresh()
                        }
                    }
                }
            }
        }
    }

    private fun getSeasons() {
        lifecycleScope.launchWhenStarted {
            viewModel.seasons.collectLatest { seasonResource ->
                val seasons = seasonResource.data ?: emptyList()

                when (seasonResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getSeason: Loading season ...")
                    }
                    is Resource.Success -> {
                        setupSeasonSwitcher(seasons)
                    }
                    is Resource.Error -> {

                        showErrorSnackbarRetryButton(seasonResource.error, bindings.root) {
                            viewModel.onRefresh()
                        }
                    }
                }
            }
        }
    }

    private fun getSelectedSeason() {
        lifecycleScope.launchWhenStarted {
            viewModel.currentSeason.collectLatest { selectedSeason ->
                val season = selectedSeason.data

                when (selectedSeason) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        // Season Stats can be null initially if user hasn't watched any of show before. See SeasonStatsRepository -> getWatchedSeasonStatsPerShow()
                        if (season != null) {
                            Log.d(TAG, "getSeason: Got season ${season.season.name}")
                            displaySeason(season)
                        }
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "getSeason: Error getting Seasons")
                    }
                }
            }
        }

    }

    private fun displaySeason(selectedSeason: TmSeasonAndStats?) {
        if (selectedSeason == null) {
            return
        }


        val season = selectedSeason.season

        bindings.seasonepisodeactivityMainGroup.visibility = View.VISIBLE

        seasonTitle = season.name

        (activity as OnTitleChangeListener).onTitleChanged(seasonTitle)

        bindings.seasonepisodeactivitySeasonTitle.text = season.name
        bindings.seasonepisodeactivitySeasonOverview.text = season.overview

        if (season.air_date != null) {
            bindings.seasonepisodeactivitySeasonAired.text = "First aired ${

                season.air_date.format(
                    DateTimeFormatter.ofPattern(
                        sharedPreferences.getString(
                            AppConstants.DATE_FORMAT,
                            AppConstants.DEFAULT_DATE_TIME_FORMAT
                        )
                    )
                )
            }"
        } else {
            bindings.seasonepisodeactivitySeasonAired.visibility = View.GONE
        }
        bindings.seasonepisodeactivitySeasonEpisodeCount.text =
            "${season.episode_count} Episodes"

        if (season.poster_path != null) {
            glide
                .load(AppConstants.TMDB_POSTER_URL + season.poster_path)
                .into(bindings.seasonepisodeactivitySeasonPoster)
        }
    }

    private fun getEpisodes() {
        lifecycleScope.launchWhenStarted {
            viewModel.episodes.collectLatest { episodesResource ->
                when (episodesResource) {
                    is Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        Log.d(TAG, "collectEpisodes: Episodes loading")
                    }
                    is Resource.Success -> {
                        progressBar.visibility = View.GONE

                        val data = episodesResource.data

                        if (data?.isNotEmpty() == true) {
                            adapter.submitList(data.sortedBy {
                                it.episode.episode_number
                            })
                        }
                    }
                    is Resource.Error -> {
                        progressBar.visibility = View.GONE

                        handleError(episodesResource.error, null)
                    }
                }
            }
        }
    }

    private fun setupSeasonSwitcher(seasons: List<TmSeasonAndStats>) {
        val seasonSwitcherTitle = bindings.seasonepisodeactivitySeasonSwitcherTitle
        seasonSwitcherTitle.visibility = View.VISIBLE

        val seasonsList: MutableList<Int> = mutableListOf()

        //Sort by the Season number 0 || 1..n and populate seasonsList with each Season #
        seasons.sortedBy { it.season.season_number }.map { tmSeason ->
            seasonsList.add(tmSeason.season.season_number)
        }

        val seasonArrayAdapter =
            ArrayAdapter<Int>(requireContext(), android.R.layout.simple_spinner_dropdown_item)

        // Populating ArrayAdapter with all the seasons for the show.
        seasonArrayAdapter.addAll(seasonsList)

        seasonSpinner.adapter = seasonArrayAdapter

        lifecycleScope.launchWhenStarted {
            viewModel.seasonSwitched.collectLatest { seasonNumber ->
                    Log.d(
                        TAG,
                        "setupSeasonSwitcher: Current Season number is ${seasonNumber}. All seasons array $seasonsList"
                    )
                    if (seasonsList.contains(seasonNumber)) {
                        seasonSpinner.setSelection(seasonsList.indexOf(seasonNumber))
                    }

            }

        }
    }

    private fun initSeasonSpinner() {
        seasonSpinner = bindings.seasonepisodeactivitySeasonSwitcher

        seasonSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {

                // Current selected element/Season number in Spinner
                val currentSeason = p0?.selectedItem as Int

                viewModel.switchSeason(currentSeason)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                Log.e(TAG, "onNothingSelected: Nothing selected")
            }
        }
    }

    private fun initRecycler() {
        recyclerView = bindings.seasonepisodesactivityRecyclerview
        val layoutManager = LinearLayoutManager(requireContext())

        adapter = EpisodesAdapter(sharedPreferences, glide, callback = { selectedEpisodeData ->
            val selectedEpisode = selectedEpisodeData.episode

            (activity as OnNavigateToEntity).navigateToEpisode(
                EpisodeDataModel(
                    selectedEpisode.show_trakt_id,
                    selectedEpisode.show_tmdb_id,
                    selectedEpisode.season_number ?: 0,
                    selectedEpisode.episode_number ?: 0,
                    ""
                )
            )
        })

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
    }

    override fun navigateToEpisode(
        showTraktId: Int,
        showTmdbId: Int?,
        seasonNumber: Int,
        episodeNumber: Int,
        showTitle: String?
    ) {
        (activity as OnNavigateToEntity).navigateToEpisode(
            EpisodeDataModel(
                showTraktId,
                showTmdbId,
                seasonNumber,
                episodeNumber,
                showTitle
            )
        )
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {
        viewModel.onRefresh()
    }
    

    companion object {

        const val SEASON_DATA_KEY = "season_data_key"

        @JvmStatic
        fun newInstance() = SeasonEpisodesFragment()

    }
}