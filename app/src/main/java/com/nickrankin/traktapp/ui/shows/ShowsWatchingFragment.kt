package com.nickrankin.traktapp.ui.shows

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.map
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.google.android.material.snackbar.Snackbar
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.shows.WatchedEpisodesLoadStateAdapter
import com.nickrankin.traktapp.adapter.shows.WatchedEpisodesPagingAdapter
import com.nickrankin.traktapp.databinding.FragmentWatchingBinding
import com.nickrankin.traktapp.helper.PosterImageLoader
import com.nickrankin.traktapp.model.auth.shows.WatchedEpisodesViewModel
import com.nickrankin.traktapp.repo.shows.ShowDetailsRepository
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
        Log.e(TAG, "onCreate: HERE", )
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
        adapter = WatchedEpisodesPagingAdapter(sharedPreferences, imageLoader, glide, callback = {selectedShow ->
            navigateToShow(selectedShow?.show_trakt_id ?: 0,selectedShow?.show_tmdb_id ?: 0, selectedShow?.language ?: "en")
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

    private fun navigateToShow(traktId: Int, tmdbId: Int, langauge: String) {
        val intent = Intent(context, ShowDetailsActivity::class.java)
        intent.putExtra(ShowDetailsRepository.SHOW_TRAKT_ID_KEY, traktId)
        intent.putExtra(ShowDetailsRepository.SHOW_TMDB_ID_KEY, tmdbId)
        intent.putExtra(ShowDetailsRepository.SHOW_LANGUAGE_KEY, langauge)

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
            viewModel.onRefresh()
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() =
            WatchingFragment()
    }
}