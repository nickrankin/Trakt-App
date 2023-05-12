package com.nickrankin.traktapp.ui.lists

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.OnNavigateToEntity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.lists.ListEntryAdapter
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.dao.lists.model.TraktListEntry
import com.nickrankin.traktapp.databinding.FragmentSplitviewLayoutBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.getResponsiveGridLayoutManager
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.lists.ListEntryViewModel
import com.nickrankin.traktapp.model.lists.TraktListsViewModel
import com.uwetrottmann.trakt5.enums.SortBy
import com.uwetrottmann.trakt5.enums.SortHow
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ListItemsFragment"

@AndroidEntryPoint
class ListItemsFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {

    private var _bindings: FragmentSplitviewLayoutBinding? = null
    private val bindings get() = _bindings!!

    private val viewModel: TraktListsViewModel by activityViewModels()

    private lateinit var progressBar: ProgressBar

    private lateinit var recyclerView: RecyclerView
    private lateinit var listEntryAdapter: ListEntryAdapter

    private var selectedList: TraktList? = null

    private var listTraktId: Int = 0

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _bindings = FragmentSplitviewLayoutBinding.inflate(inflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        progressBar = bindings.splitviewlayoutProgressbar
        listTraktId = arguments?.getInt(ListEntryViewModel.LIST_ID_KEY, 0) ?: 0

        if (!isLoggedIn) {
            handleLoggedOutState(this.id)
        }

        setHasOptionsMenu(true)

        (activity as OnNavigateToEntity).enableOverviewLayout(true)

        initRecycler()
        getListsAndEntries()
        getEvents()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.list_items_menu, menu)
    }

    private fun getListsAndEntries() {

        lifecycleScope.launch {
            viewModel.listItems.collectLatest { listAndListEntriesData ->

                val listResource = listAndListEntriesData.first
                val listEntryResource = listAndListEntriesData.second

                when (listResource) {
                    is Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        selectedList = listResource.data
                        displayListEntries(listAndListEntriesData.second)
                        updateTitle(listResource.data?.name ?: getString(R.string.unknown_list))

                    }
                    is Resource.Error -> {
                        progressBar.visibility = View.VISIBLE

                        selectedList = listResource.data
                        updateTitle(listResource.data?.name ?: getString(R.string.unknown_list))

                        showErrorSnackbarRetryButton(listResource.error, bindings.root) {
                            onRefresh()
                        }
                    }

                }
            }
        }
    }

    private fun displayListEntries(listEntriesResource: Resource<List<TraktListEntry>>?) {
        if(listEntriesResource == null) {
            toggleMessageBanner(bindings, getString(R.string.error_loading_list_items), true)
            return
        }

        when (listEntriesResource) {
            is Resource.Loading -> {
                progressBar.visibility = View.VISIBLE
            }
            is Resource.Success -> {
                progressBar.visibility = View.GONE

                val listItems = listEntriesResource.data

                if(listItems.isNullOrEmpty()) {
                    toggleMessageBanner(bindings, getString(R.string.no_list_entries), true)
                } else {
                    toggleMessageBanner(bindings, null, false)

                    if(selectedList != null) {
                        val sortedLists = sortAndDisplayListEntries(selectedList!!.sortBy!!, selectedList!!.sortHow!!, listEntriesResource.data ?: emptyList())
                        listEntryAdapter.submitList(sortedLists)

                    } else {
                        Log.e(TAG, "displayListEntries: Selected List cannot be null", )
                    }
                }

            }
            is Resource.Error -> {
                progressBar.visibility = View.GONE

                val listItems = listEntriesResource.data

                if(listItems.isNullOrEmpty()) {
                    toggleMessageBanner(bindings, getString(R.string.no_list_entries), true)
                } else {
                    toggleMessageBanner(bindings, null, false)

                    if(selectedList != null) {
                        val sortedLists = sortAndDisplayListEntries(selectedList!!.sortBy!!, selectedList!!.sortHow!!, listEntriesResource.data ?: emptyList())
                        listEntryAdapter.submitList(sortedLists)

                    } else {
                        Log.e(TAG, "displayListEntries: Selected List cannot be null", )
                    }
                }

                showErrorSnackbarRetryButton(listEntriesResource.error, bindings.root) {
                    onRefresh()
                }
            }
        }
    }

    private fun sortAndDisplayListEntries(sortBy: SortBy, sortHow: SortHow, listEntries: List<TraktListEntry>): List<TraktListEntry> {
                return getSortedList(
                    listEntries,
                    sortBy,
                    sortHow
            )

    }

    private fun getSortedList(
        listItems: List<TraktListEntry>,
        sortBy: SortBy?,
        sortHow: SortHow?
    ): List<TraktListEntry> {
        // If sorting not specified, display list as is
        if (sortBy == null || sortHow == null) {
            return listItems
        }

        return when (sortBy) {
            SortBy.RANK -> {
                var sortedList = listItems.sortedBy {
                    it.entryData.rank
                }

                if (sortHow == SortHow.DESC) {
                    sortedList = sortedList.reversed()
                }

                sortedList
            }
            SortBy.ADDED -> {
                var sortedList = listItems.sortedBy {
                    it.entryData.listed_at
                }

                if (sortHow == SortHow.DESC) {
                    sortedList = sortedList.reversed()
                }

                sortedList
            }
            SortBy.TITLE -> {
                var sortedList = listItems.sortedBy {
                    when (it.entryData.type) {
                        Type.MOVIE -> {
                            it.movie?.title?.lowercase()
                        }
                        Type.SHOW -> {
                            it.show?.title?.lowercase()
                        }
                        Type.EPISODE -> {
                            it.episodeShow?.title!!.lowercase()

                        }
                        Type.PERSON -> {
                            it.person?.title?.lowercase()

                        }
                        Type.LIST -> {
                            it.entryData.type.name.lowercase()
                        }
                    }
                }

                if (sortHow == SortHow.DESC) {
                    sortedList = sortedList.reversed()
                }

                sortedList
            }
            SortBy.RELEASED -> {
                var sortedList = listItems.sortedBy {
                    when (it.entryData.type) {
                        Type.MOVIE -> {
                            it.movie?.first_aired.toString()
                        }
                        Type.SHOW -> {
                            it.show?.first_aired.toString()
                        }
                        Type.EPISODE -> {
                            it.episode?.first_aired.toString()

                        }
                        Type.PERSON -> {
                            it.person?.dob.toString()

                        }
                        Type.LIST -> {
                            it.entryData.listed_at.toString()
                        }
                    }
                }

                if (sortHow == SortHow.DESC) {
                    sortedList = sortedList.reversed()
                }

                sortedList
            }
            SortBy.RUNTIME -> {
                listItems
            }
            SortBy.POPULARITY -> {
                listItems
            }
            SortBy.PERCENTAGE -> {
                listItems
            }
            SortBy.VOTES -> {
                listItems
            }
            SortBy.MY_RATING -> {
                listItems
            }
            SortBy.RANDOM -> {
                listItems.shuffled()
            }
        }
    }

    private fun getEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->
                when (event) {

                    is TraktListsViewModel.Event.RemoveEntryEvent -> {
                        val eventSyncResponseResource = event.syncResponseResource

                        when (eventSyncResponseResource) {
                            is Resource.Success -> {
                                val syncResponseDeletedStats =
                                    eventSyncResponseResource.data?.deleted

                                if (syncResponseDeletedStats != null) {
                                    if (syncResponseDeletedStats.movies > 0 || syncResponseDeletedStats.shows > 0 || syncResponseDeletedStats.episodes > 0 || syncResponseDeletedStats.people > 0) {
                                        displayToastMessage(
                                            "Successfully removed entry from the list!",
                                            Toast.LENGTH_SHORT
                                        )
                                    }

                                }

                            }
                            is Resource.Error -> {
                                handleError(
                                    eventSyncResponseResource.error,
                                    "Error removing entry item "
                                )
                            }
                            else -> {}

                        }
                    }
                    is TraktListsViewModel.Event.AddListEvent -> {}
                    is TraktListsViewModel.Event.DeleteListEvent -> {}
                    is TraktListsViewModel.Event.EditListEvent -> {}
                    is TraktListsViewModel.Event.ErrorEvent -> {
                        // Null means no error so list update successful, only show error on Throwable not being NULL
                        if (event.error != null) {
                            Toast.makeText(
                                requireContext(),
                                "Could not update the list ordering. ${event.error.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    }
                }
            }
        }
    }

    private fun initRecycler() {
        recyclerView = bindings.splitviewlayoutRecyclerview
        val lm = getResponsiveGridLayoutManager(requireContext(), null)

        listEntryAdapter = ListEntryAdapter(
            glide,
            tmdbImageLoader,
            sharedPreferences
        ) { traktId, action, type, selectedItem ->
            when (type) {
                Type.MOVIE -> {
                    when (action) {
                        ListEntryAdapter.ACTION_VIEW -> {
                            if (traktId != null) {
                                (activity as OnNavigateToEntity).navigateToMovie(
                                    MovieDataModel(
                                        traktId,
                                        selectedItem.movie?.tmdb_id,
                                        selectedItem.movie?.title,
                                        selectedItem.movie?.first_aired?.year ?: 0
                                    )
                                )
                            }


                        }
                        ListEntryAdapter.ACTION_REMOVE -> {
                            if (traktId != null) {
                                removeListItemEntry(traktId, Type.MOVIE)

                            }
                        }
                    }

                }
                Type.SHOW -> {
                    when (action) {
                        ListEntryAdapter.ACTION_VIEW -> {

                            if (traktId != null) {

                                (activity as OnNavigateToEntity).navigateToShow(
                                    ShowDataModel(
                                        traktId,
                                        selectedItem.show?.tmdb_id,
                                        selectedItem.show?.title
                                    )
                                )
                            }


                        }
                        ListEntryAdapter.ACTION_REMOVE -> {
                            if (traktId != null) {
                                removeListItemEntry(traktId, Type.SHOW)

                            }

                        }
                    }
                }
                Type.EPISODE -> {
                    when (action) {
                        ListEntryAdapter.ACTION_VIEW -> {

                            if (selectedItem.episodeShow?.trakt_id != null) {

                                (activity as OnNavigateToEntity).navigateToEpisode(
                                    EpisodeDataModel(
                                        selectedItem.episodeShow.trakt_id,
                                        selectedItem.episodeShow.tmdb_id,
                                        selectedItem.episode?.season_number ?: 0,
                                        selectedItem.episode?.episode_number ?: 0,
                                        selectedItem.episodeShow.title
                                    )
                                )

                            }


                        }
                        ListEntryAdapter.ACTION_REMOVE -> {
                            if (selectedItem.episode?.trakt_id != null) {
                                removeListItemEntry(selectedItem.episode.trakt_id, Type.EPISODE)
                            }

                        }
                    }

                }
                else -> {}
            }

        }

        recyclerView.layoutManager = lm
        recyclerView.adapter = listEntryAdapter

    }

    private fun removeListItemEntry(listEntryItemTraktId: Int, type: Type) {
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Confirm removal")
            .setMessage("Are you sure you want to remove this list entry?")
            .setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->
                viewModel.removeEntry(listTraktId, listEntryItemTraktId, type)
                dialogInterface.dismiss()
            })
            .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            .create()

        alertDialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.listitemsmenu_added -> {
                viewModel.changeListOrdering(selectedList, SortBy.ADDED)
            }
            R.id.listitemsmenu_rank -> {
                viewModel.changeListOrdering(selectedList, SortBy.RANK)

            }
            R.id.listitemsmenu_title -> {
                viewModel.changeListOrdering(selectedList, SortBy.TITLE)

            }
            R.id.listitemsmenu_released -> {
                viewModel.changeListOrdering(selectedList, SortBy.RELEASED)
            }
            R.id.listitemsmenu_random -> {
                viewModel.changeListOrdering(selectedList, SortBy.RANDOM)
            }
        }

        return true
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {
        viewModel.onRefresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _bindings = null
    }

    private fun displayToastMessage(messageText: String, length: Int) {
        Toast.makeText(requireContext(), messageText, length).show()
    }

    companion object {
        @JvmStatic
        fun newInstance() = ListItemsFragment()
    }
}