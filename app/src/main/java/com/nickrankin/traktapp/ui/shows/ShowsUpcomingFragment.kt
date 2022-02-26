package com.nickrankin.traktapp.ui.shows

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.*
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.shows.ShowCalendarEntriesAdapter
import com.nickrankin.traktapp.databinding.FragmentShowsOverviewBinding
import com.nickrankin.traktapp.helper.PosterImageLoader
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.auth.shows.ShowsOverviewViewModel
import com.nickrankin.traktapp.repo.shows.episodedetails.EpisodeDetailsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsActivity
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "OverviewFragment"
@AndroidEntryPoint
class ShowsUpcomingFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, OnNavigateToShow, OnNavigateToEpisode {
    
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

        setHasOptionsMenu(true)

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.calendar_shows_filter_menu, menu)
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

                    Log.d(TAG, "getMyShows: Got ${data?.size} SHOWS")

//                    data?.map {
//                        Log.d(TAG, "getMyShows: Got show ${it.show_title} airing episode ${it.episode_title} S${it.episode_season} // E${it.episode_number} on ${it.first_aired.toString()}")
//                    }

                    if(data?.isEmpty() == true) {
                        messageContainer.visibility = View.VISIBLE
                        messageContainer.text = "No upcoming shows!"
                    } else {
                        messageContainer.visibility = View.GONE
                    }
                    adapter.submitList(data?.sortedBy {
                        it.first_aired
                    })
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

        adapter = ShowCalendarEntriesAdapter(sharedPreferences, posterImageLoader, glide, callback = { calendarEntry, action ->
            when(action) {
                ShowCalendarEntriesAdapter.ACTION_NAVIGATE_EPISODE -> {
                    navigateToEpisode(calendarEntry.show_trakt_id, calendarEntry.show_tmdb_id, calendarEntry.episode_season, calendarEntry.episode_number, calendarEntry.language ?: "en")
                }
                ShowCalendarEntriesAdapter.ACTION_NAVIGATE_SHOW -> {
                    navigateToShow(calendarEntry.show_trakt_id, calendarEntry.show_tmdb_id, calendarEntry.show_title, calendarEntry.language)
                }
                ShowCalendarEntriesAdapter.ACTION_REMOVE_COLLECTION-> {
                    viewModel.setShowHiddenState(calendarEntry.show_tmdb_id, !calendarEntry.hidden)

                    // Force refresh of list
                    viewModel.onReload()

                }

                else -> {
                    navigateToEpisode(calendarEntry.show_trakt_id, calendarEntry.show_tmdb_id, calendarEntry.episode_season, calendarEntry.episode_number, calendarEntry.language ?: "en")
                }
            }
        })

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

    }

    override fun navigateToShow(traktId: Int, tmdbId: Int, showTitle: String?, language: String?) {

        val intent = Intent(context, ShowDetailsActivity::class.java)
        intent.putExtra(ShowDetailsRepository.SHOW_TRAKT_ID_KEY, traktId)
        intent.putExtra(ShowDetailsRepository.SHOW_TMDB_ID_KEY, tmdbId)
        intent.putExtra(ShowDetailsRepository.SHOW_TITLE_KEY, showTitle)
        intent.putExtra(ShowDetailsRepository.SHOW_LANGUAGE_KEY, language)

        startActivity(intent)
    }

    override fun navigateToEpisode(showTraktId: Int, showTmdbId: Int?, seasonNumber: Int, episodeNumber: Int, language: String?) {
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.calendarshowsmenu_show_hidden -> {
                viewModel.showHiddenEntries(true)
                viewModel.onReload()
            }
        }

        return false
    }


    override fun onResume() {
        super.onResume()

        if(isLoggedIn) {
            viewModel.showHiddenEntries(false)
            viewModel.onReload()
        }
    }

    override fun onRefresh() {
        if(isLoggedIn) {
            viewModel.showHiddenEntries(false)
            viewModel.onRefresh()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            ShowsUpcomingFragment()
    }
}