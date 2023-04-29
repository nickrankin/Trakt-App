package com.nickrankin.traktapp.ui.shows

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.OnNavigateToEntity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.shows.ShowCalendarEntriesAdapter
import com.nickrankin.traktapp.dao.calendars.model.BaseCalendarEntry
import com.nickrankin.traktapp.dao.calendars.model.ShowBaseCalendarEntry
import com.nickrankin.traktapp.databinding.FragmentSplitviewLayoutBinding
import com.nickrankin.traktapp.helper.IHandleError
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.switchRecyclerViewLayoutManager
import com.nickrankin.traktapp.model.auth.shows.ShowsOverviewViewModel
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import org.threeten.bp.temporal.ChronoUnit
import javax.inject.Inject

private const val TAG = "OverviewFragment"
@AndroidEntryPoint
class ShowsUpcomingFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener, OnNavigateToShow, OnNavigateToEpisode {

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader

    private var _bindings: FragmentSplitviewLayoutBinding? = null
    private val bindings get() = _bindings!!


    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageContainer: TextView

    private lateinit var adapter: ShowCalendarEntriesAdapter

    private val viewModel: ShowsOverviewViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _bindings = FragmentSplitviewLayoutBinding.inflate(inflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        messageContainer = bindings.splitviewlayoutMessageContainer


        updateTitle("Upcoming Shows")

        progressBar = bindings.splitviewlayoutProgressbar

        (activity as OnNavigateToEntity).enableOverviewLayout(false)

        if(!isLoggedIn) {
            handleLoggedOutState(this.id)
        }

        setupRecyclerView()
//        getViewType()
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

                        val data = myShowsResource.data

                        if(data?.isEmpty() == true) {
                            messageContainer.visibility = View.VISIBLE
                            messageContainer.text = "No upcoming shows!"
                        } else {
                            messageContainer.visibility = View.GONE
                        }


                        adapter.submitList(sortShows(data))

                    }
                    is Resource.Error -> {
                        messageContainer.visibility = View.GONE
                        progressBar.visibility = View.GONE


//                        if(myShowsResource.data != null) {
//                            adapter.submitList(myShowsResource.data ?: emptyList())
//                        }

                        (activity as IHandleError).showErrorSnackbarRetryButton(
                            myShowsResource.error,
                            bindings.root
                        ) {
                            viewModel.onRefresh()
                        }
                    }
                }
            }
        }

    }

    private fun sortShows(showCalendarEntries: List<ShowBaseCalendarEntry>?): List<BaseCalendarEntry> {

        // Group the shows by Date of their airing.
        val showsGroupedByDate = showCalendarEntries?.groupBy {
            it.first_aired?.truncatedTo(ChronoUnit.DAYS)
        }

        val sortedShows = showsGroupedByDate?.flatMap { value1 ->

            val mutableList: MutableList<BaseCalendarEntry> = mutableListOf()

            // We use the key of the sortedShows Map to provide heading inserts into the List. The ListAdapter will display first Heading (Date airing) followed by all Show elements upcoming on that date.
            // We should only have one Heading for particular date based on groupBy value to be certain, ensure we only have one heading per date
            if(mutableList.find { it.first_aired?.truncatedTo(ChronoUnit.DAYS) == value1.key?.truncatedTo(ChronoUnit.DAYS) } == null) {
                Log.d(TAG, "getMyShows: Header set: - ${value1.key?.truncatedTo(ChronoUnit.DAYS)}", )

                // Add the heading element
                mutableList.add(BaseCalendarEntry(value1.hashCode(), value1.key))
            }

            // Add the upcoming shows, this time sorted by Date & Time
            mutableList.addAll(value1.value.sortedBy { it.first_aired })

            mutableList
        }

        return sortedShows ?: emptyList()
    }

    private fun setupRecyclerView() {
        recyclerView = bindings.splitviewlayoutRecyclerview

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = ShowCalendarEntriesAdapter(sharedPreferences, tmdbImageLoader, callback = { calendarEntry, action ->
            if(calendarEntry is ShowBaseCalendarEntry) {
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
            }

        })

        recyclerView.adapter = adapter

    }

    override fun navigateToShow(traktId: Int, tmdbId: Int?, title: String?) {
        (activity as OnNavigateToEntity).navigateToShow(
            ShowDataModel(
                traktId, tmdbId, title
            )
        )
    }
    override fun navigateToEpisode(showTraktId: Int, showTmdbId: Int?, seasonNumber: Int, episodeNumber: Int, language: String?) {

        (activity as OnNavigateToEntity).navigateToEpisode(EpisodeDataModel(
            showTraktId,
            showTmdbId,
            seasonNumber,
            episodeNumber,
            language
        ))

//        // We cannot guarantee Watched Episode data is up to date at this point so force refresh (user could have watched more of this show in meantime)
//        intent.putExtra(EpisodeDetailsRepository.SHOULD_REFRESH_WATCHED_KEY, true)

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
//                adapter.switchView(viewType)

                switchRecyclerViewLayoutManager(requireContext(), recyclerView, viewType)


                recyclerView.scrollToPosition(0)
            }
        }
    }


    override fun onResume() {
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

    override fun onDestroyView() {
        super.onDestroyView()

        _bindings = null
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            ShowsUpcomingFragment()
    }
}