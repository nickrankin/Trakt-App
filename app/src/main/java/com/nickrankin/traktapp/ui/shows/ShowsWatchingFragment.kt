package com.nickrankin.traktapp.ui.shows

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.adapter.shows.WatchedEpisodesLoadStateAdapter
import com.nickrankin.traktapp.adapter.shows.WatchedEpisodesPagingAdapter
import com.nickrankin.traktapp.databinding.FragmentWatchingBinding
import com.nickrankin.traktapp.helper.PosterImageLoader
import com.nickrankin.traktapp.model.auth.shows.WatchedEpisodesViewModel
import com.nickrankin.traktapp.repo.shows.EpisodeDetailsRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "WatchingFragment"
@AndroidEntryPoint
class WatchingFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    
    private val viewModel by activityViewModels<WatchedEpisodesViewModel>()

    private lateinit var bindings: FragmentWatchingBinding
    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: WatchedEpisodesPagingAdapter

    @Inject
    lateinit var sharedPreferences: SharedPreferences
    
    @Inject
    lateinit var imageLoader: PosterImageLoader

    @Inject
    lateinit var glide: RequestManager

    private var isLoggedIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = FragmentWatchingBinding.inflate(inflater)
        // Inflate the layout for this fragment

        isLoggedIn = sharedPreferences.getBoolean(AuthActivity.IS_LOGGED_IN, false)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        swipeLayout = bindings.showwatchingfragmentSwipeRefreshLayout
        swipeLayout.setOnRefreshListener(this)

        progressBar = bindings.showwatchingfragmentProgressbar

        initRecycler()
        
        if(isLoggedIn) {
            lifecycleScope.launch {
                collectEpisodes()
            }
        } else {
            handleLoggedOutState()
        }
        

    }

    private fun handleLoggedOutState() {
        progressBar.visibility = View.GONE
        val messageContainer = bindings.showwatchingfragmentMessageContainer
            messageContainer.visibility = View.VISIBLE
        swipeLayout.isEnabled = false

        val connectButton = bindings.showwatchingfragmentTraktConnectButton
        connectButton.visibility = View.VISIBLE

        messageContainer.text = "You are not logged in. Please login to see your  Watched shows."

        connectButton.setOnClickListener {
            startActivity(Intent(activity, AuthActivity::class.java))
        }
    }
    
    private suspend fun collectEpisodes() {
        viewModel.watchedEpisodes.collectLatest { latestData ->
            progressBar.visibility = View.GONE

            if(swipeLayout.isRefreshing) {
                swipeLayout.isRefreshing = false
            }

            adapter.submitData(latestData)

        }
    }

    private fun initRecycler() {
        recyclerView = bindings.showwatchingfragmentRecyclerview
        layoutManager = LinearLayoutManager(context)
        adapter = WatchedEpisodesPagingAdapter(sharedPreferences, imageLoader, glide, callback = {selectedEpisode ->
            navigateToEpisode(selectedEpisode?.show_trakt_id ?: 0,selectedEpisode?.show_tmdb_id ?: 0, selectedEpisode?.episode_season ?: 0, selectedEpisode?.episode_number ?: 0, selectedEpisode?.language ?: "en")
        })
        recyclerView.layoutManager = layoutManager

        recyclerView.adapter = adapter.withLoadStateHeaderAndFooter(
            header = WatchedEpisodesLoadStateAdapter(adapter),
            footer = WatchedEpisodesLoadStateAdapter(adapter)
        )

        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow.collectLatest { loadStates ->
                bindings.showwatchingfragmentSwipeRefreshLayout.isRefreshing = loadStates.refresh is LoadState.Loading
            }
        }

        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow
                // Only emit when REFRESH LoadState for RemoteMediator changes.
                .distinctUntilChangedBy { it.refresh }
                // Only react to cases where Remote REFRESH completes i.e., NotLoading.
                .filter { it.refresh is LoadState.NotLoading }
                .collect { bindings.showwatchingfragmentRecyclerview.scrollToPosition(0) }
        }

    }

    private fun navigateToEpisode(showTraktId: Int, showTmdbId: Int, seasonNumber: Int, episodeNumber: Int, language: String) {
        val intent = Intent(context, EpisodeDetailsActivity::class.java)
        intent.putExtra(EpisodeDetailsRepository.SHOW_TRAKT_ID_KEY, showTraktId)
        intent.putExtra(EpisodeDetailsRepository.SHOW_TMDB_ID_KEY, showTmdbId)
        intent.putExtra(EpisodeDetailsRepository.SEASON_NUMBER_KEY, seasonNumber)
        intent.putExtra(EpisodeDetailsRepository.EPISODE_NUMBER_KEY, episodeNumber)
        intent.putExtra(EpisodeDetailsRepository.LANGUAGE_KEY, language)

        // We cannot guarantee Watched Episode data is up to date at this point so force refresh (user could have watched more of this show in meantime)
        intent.putExtra(EpisodeDetailsRepository.SHOULD_REFRESH_WATCHED_KEY, true)

        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()
        if(isLoggedIn) {
            viewModel.onStart()
        }
    }

    override fun onRefresh() {
        if(isLoggedIn) {
            //https://developer.android.com/reference/kotlin/androidx/paging/PagingDataAdapter#refresh()
            adapter.refresh()
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() =
            WatchingFragment()
    }
}