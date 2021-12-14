package com.nickrankin.traktapp.ui.shows

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.google.android.material.snackbar.Snackbar
import com.nickrankin.traktapp.MainActivity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.shows.ShowCalendarEntriesAdapter
import com.nickrankin.traktapp.dao.calendars.model.ShowCalendarEntry
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.databinding.FragmentShowsOverviewBinding
import com.nickrankin.traktapp.helper.PosterImageLoader
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.auth.shows.ShowsOverviewViewModel
import com.nickrankin.traktapp.repo.shows.EpisodeDetailsRepository
import com.nickrankin.traktapp.repo.shows.ShowDetailsRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "OverviewFragment"
@AndroidEntryPoint
class OverviewFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var posterImageLoader: PosterImageLoader
    
    private lateinit var bindings: FragmentShowsOverviewBinding
    private lateinit var progressBar: ProgressBar
    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageContainer: TextView

    private lateinit var adapter: ShowCalendarEntriesAdapter

    private var isLoggedIn = false

    private lateinit var layoutManager: LinearLayoutManager

    private val viewModel by activityViewModels<ShowsOverviewViewModel>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings= FragmentShowsOverviewBinding.inflate(inflater)

        isLoggedIn = sharedPreferences.getBoolean(AuthActivity.IS_LOGGED_IN, false)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        refreshLayout = bindings.showsoverviewfragmentSwipeRefreshLayout
        messageContainer = bindings.showsoverviewfragmentMessageContainer

        refreshLayout.setOnRefreshListener(this)

        progressBar = bindings.showsoverviewfragmentProgressbar
        if(isLoggedIn) {
            setupRecyclerView()

            lifecycleScope.launch {
                getMyShows()
            }
        } else {
            handleLoggedOutState()
        }

    }

    private fun handleLoggedOutState() {
        progressBar.visibility = View.GONE
        messageContainer.visibility = View.VISIBLE
        refreshLayout.isEnabled = false

        val connectButton = bindings.showsoverviewfragmentTraktConnectButton
            connectButton.visibility = View.VISIBLE

        messageContainer.text = "You are not logged in. Please login to see your shows."

        connectButton.setOnClickListener {
            startActivity(Intent(activity, AuthActivity::class.java))
        }
    }
    
    private suspend fun getMyShows() {
        viewModel.myShows.collectLatest { myShowsResource ->
            when(myShowsResource) {
                is Resource.Loading -> {
                    Log.d(TAG, "getMyShows: Loading shows")
                    progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    progressBar.visibility = View.GONE
                    messageContainer.visibility = View.GONE

                    if(refreshLayout.isRefreshing) {
                        refreshLayout.isRefreshing = false
                    }

                    val data = myShowsResource.data

                    if(data?.isNotEmpty() == true) {
                        adapter.submitList(data)
                    } else {
                        messageContainer.visibility = View.VISIBLE
                        messageContainer.text = "You have no shows :-("
                    }


                    data?.map {
                        Log.d(TAG, "getMyShows: Got show ${it.show_title} airing episode ${it.episode_title} S${it.episode_season} // E${it.episode_number} on ${it.first_aired.toString()}")
                    }
                }
                is Resource.Error -> {
                    messageContainer.visibility = View.GONE
                    progressBar.visibility = View.GONE

                    if(refreshLayout.isRefreshing) {
                        refreshLayout.isRefreshing = false
                    }

                    if(myShowsResource.data != null) {
                        adapter.submitList(myShowsResource.data ?: emptyList())
                    }else {
                        messageContainer.visibility = View.VISIBLE
                        messageContainer.text = "An error occurred loading your shows. ${myShowsResource.error?.localizedMessage}"
                    }


                    Log.e(TAG, "getMyShows: Error getting my shows.. ${myShowsResource.error?.localizedMessage}", )
                }
            }            
        }
    }

    private fun setupRecyclerView() {
        recyclerView = bindings.showsoverviewfragmentRecyclerview
        layoutManager = LinearLayoutManager(context)

        adapter = ShowCalendarEntriesAdapter(sharedPreferences, posterImageLoader, glide, callback = {calendarEntry ->
            navigateToEpisode(calendarEntry.show_trakt_id, calendarEntry.show_tmdb_id, calendarEntry.episode_season, calendarEntry.episode_number, calendarEntry.language ?: "en")
        })

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

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
            viewModel.onRefresh()
        }
    }

    companion object {
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() =
            OverviewFragment()
    }
}