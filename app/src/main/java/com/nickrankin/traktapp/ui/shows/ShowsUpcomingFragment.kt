package com.nickrankin.traktapp.ui.shows

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.adapter.shows.ShowCalendarEntriesAdapter
import com.nickrankin.traktapp.databinding.FragmentShowsOverviewBinding
import com.nickrankin.traktapp.helper.IHandleError
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.switchRecyclerViewLayoutManager
import com.nickrankin.traktapp.model.auth.shows.ShowsOverviewViewModel
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.repo.shows.episodedetails.EpisodeDetailsRepository
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsActivity
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "OverviewFragment"
@AndroidEntryPoint
class ShowsUpcomingFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener, OnNavigateToShow, OnNavigateToEpisode {

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader
    
    private lateinit var bindings: FragmentShowsOverviewBinding
    private lateinit var progressBar: ProgressBar
    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageContainer: TextView

    private lateinit var adapter: ShowCalendarEntriesAdapter

    private val viewModel by activityViewModels<ShowsOverviewViewModel>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e(TAG, "onCreate: $this", )
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = FragmentShowsOverviewBinding.inflate(inflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        refreshLayout = bindings.showsoverviewfragmentSwipeRefreshLayout
        messageContainer = bindings.showsoverviewfragmentMessageContainer

        refreshLayout.setOnRefreshListener(this)

        updateTitle("Upcoming Shows")

        progressBar = bindings.showsoverviewfragmentProgressbar

        if(!isLoggedIn) {
            handleLoggedOutState(this.id)
        }

        setupRecyclerView()
        getViewType()
        getMyShows()

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.calendar_shows_filter_menu, menu)
    }

    private fun getMyShows() {
        lifecycleScope.launchWhenStarted {
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
                        }

                        (activity as IHandleError).showErrorSnackbarRetryButton(
                            myShowsResource.error,
                            bindings.showsoverviewfragmentSwipeRefreshLayout
                        ) {
                            viewModel.onRefresh()
                        }
                    }
                }
            }
        }

    }

    private fun setupRecyclerView() {
        recyclerView = bindings.showsoverviewfragmentRecyclerview

        switchRecyclerViewLayoutManager(requireContext(), recyclerView, MediaEntryBaseAdapter.VIEW_TYPE_POSTER)

        adapter = ShowCalendarEntriesAdapter(sharedPreferences, tmdbImageLoader, callback = { calendarEntry, action ->
            when(action) {
                ShowCalendarEntriesAdapter.ACTION_NAVIGATE_EPISODE -> {
                    navigateToEpisode(calendarEntry.show_trakt_id, calendarEntry.show_tmdb_id, calendarEntry.episode_season, calendarEntry.episode_number, calendarEntry.language ?: "en")
                }
                ShowCalendarEntriesAdapter.ACTION_NAVIGATE_SHOW -> {
                    navigateToShow(calendarEntry.show_trakt_id, calendarEntry.show_tmdb_id, calendarEntry.show_title)
                }
                ShowCalendarEntriesAdapter.ACTION_REMOVE_COLLECTION-> {
                    viewModel.setShowHiddenState(calendarEntry.show_tmdb_id, !calendarEntry.hidden)

                    // Force refresh of list
                    viewModel.onRefresh()

                }

                else -> {
                    navigateToEpisode(calendarEntry.show_trakt_id, calendarEntry.show_tmdb_id, calendarEntry.episode_season, calendarEntry.episode_number, calendarEntry.language ?: "en")
                }
            }
        })

        recyclerView.adapter = adapter

    }

    override fun navigateToShow(traktId: Int, tmdbId: Int?, title: String?) {
        val intent = Intent(requireActivity(), ShowDetailsActivity::class.java)
        intent.putExtra(ShowDetailsActivity.SHOW_DATA_KEY,
            ShowDataModel(
                traktId, tmdbId, title
            )
        )
        startActivity(intent)
    }
    override fun navigateToEpisode(showTraktId: Int, showTmdbId: Int?, seasonNumber: Int, episodeNumber: Int, language: String?) {
        val intent = Intent(context, EpisodeDetailsActivity::class.java)

        intent.putExtra(EpisodeDetailsActivity.EPISODE_DATA_KEY,
            EpisodeDataModel(
                showTraktId,
                showTmdbId,
                seasonNumber,
                episodeNumber,
                language
            )
        )


        // We cannot guarantee Watched Episode data is up to date at this point so force refresh (user could have watched more of this show in meantime)
        intent.putExtra(EpisodeDetailsRepository.SHOULD_REFRESH_WATCHED_KEY, true)

        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.calendarshowsmenu_show_hidden -> {
                viewModel.showHiddenEntries(true)
                viewModel.onRefresh()
            }
            R.id.calendarshowsmenu_switch_layout -> {
                lifecycleScope.launchWhenStarted {
                    viewModel.switchViewType()
                }
            }
        }

        return false
    }

    private fun  getViewType() {
        lifecycleScope.launchWhenStarted {
            viewModel.viewType.collectLatest { viewType ->
                adapter.switchView(viewType)

                switchRecyclerViewLayoutManager(requireContext(), recyclerView, viewType)


                recyclerView.scrollToPosition(0)
            }
        }
    }


    override fun onResume() {
        Log.e(TAG, "onResume: $this", )
        super.onResume()

        if(isLoggedIn) {
            viewModel.showHiddenEntries(false)
            viewModel.onRefresh()
        }
    }

    override fun onRefresh() {
        super.onRefresh()

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