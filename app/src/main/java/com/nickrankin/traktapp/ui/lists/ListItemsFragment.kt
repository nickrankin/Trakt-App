package com.nickrankin.traktapp.ui.lists

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.lists.ListEntryAdapter
import com.nickrankin.traktapp.databinding.FragmentListItemsBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.getResponsiveGridLayoutManager
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.lists.ListEntryViewModel
import com.nickrankin.traktapp.model.lists.TraktListsViewModel
import com.nickrankin.traktapp.ui.movies.moviedetails.MovieDetailsActivity
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsActivity
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ListItemsFragment"
@AndroidEntryPoint
class ListItemsFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {

    private var bindings: FragmentListItemsBinding? = null

    private val viewModel: TraktListsViewModel by activityViewModels()

    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var recyclerView: RecyclerView
    private lateinit var listEntryAdapter: ListEntryAdapter

    private var listTraktId: Int = 0

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        bindings = FragmentListItemsBinding.inflate(inflater)

        return bindings!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        swipeRefreshLayout = bindings!!.traktlistsfragmentSwipeRefresh
        swipeRefreshLayout.setOnRefreshListener(this)
        progressBar = bindings!!.traktlistsfragmentProgressbar

        listTraktId = arguments?.getInt(ListEntryViewModel.LIST_ID_KEY, 0) ?: 0
//        listName = arguments?.getString(ListEntryViewModel.LIST_NAME_KEY, "Unknown List") ?: "Unknown List"

        if(!isLoggedIn) {
            handleLoggedOutState(this.id)
        }

//        updateTitle("$listName Entries")

        getList()
        initRecycler()
        getListEntries()
        getEvents()
    }
    
    private fun getList() {
        lifecycleScope.launchWhenStarted { 
            viewModel.list.collectLatest { listResource ->
                when(listResource) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        if(listResource.data != null) {
                            updateTitle(listResource.data?.name ?: "Unknown List")
                        }
                    }
                    is Resource.Error -> {
                        handleError(listResource.error, null)
                    }

                }
            }
        }
    }

    private fun getListEntries() {
        val messageContainerTextView = bindings!!.traktlistsfragmentMessageContainer

        lifecycleScope.launch {
            viewModel.listItems.collectLatest { listEntriesData ->
                when(listEntriesData) {
                    is Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        Log.d(TAG, "getListEntries: Loading entires")
                    }
                    is Resource.Success -> {
                        progressBar.visibility = View.GONE
                        if(swipeRefreshLayout.isRefreshing) {
                            swipeRefreshLayout.isRefreshing = false
                        }

                        val listItems = listEntriesData.data ?: emptyList()

                        if(listItems.isNotEmpty()) {
                            messageContainerTextView.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE

                            listEntryAdapter.submitList(listEntriesData.data)
                        } else {
                            messageContainerTextView.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE

//                            messageContainerTextView.text = "You do not have any entries in list $listName! Why not add some?"
                        }
                        Log.d(TAG, "getListEntries: Got ${listEntriesData.data?.size} entries")
                    }
                    is Resource.Error -> {
                        progressBar.visibility = View.GONE
                        if(swipeRefreshLayout.isRefreshing) {
                            swipeRefreshLayout.isRefreshing = false
                        }

                        val listItems = listEntriesData.data ?: emptyList()

                        if(listItems.isNotEmpty()) {
                            messageContainerTextView.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE

                            listEntryAdapter.submitList(listEntriesData.data)
                        } else {
                            messageContainerTextView.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE

//                            messageContainerTextView.text = "You do not have any entries in list $listName! Why not add some?"
                        }

                        handleError(listEntriesData.error, null)
                    }
                }
            }
        }
    }

    private fun getEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->
                when(event) {

                    is TraktListsViewModel.Event.RemoveEntryEvent -> {
                        val eventSyncResponseResource = event.syncResponseResource

                        when(eventSyncResponseResource) {
                            is Resource.Success -> {
                                val syncResponseDeletedStats = eventSyncResponseResource.data?.deleted

                                if(syncResponseDeletedStats != null) {
                                    if(syncResponseDeletedStats.movies > 0 || syncResponseDeletedStats.shows > 0 || syncResponseDeletedStats.episodes > 0 || syncResponseDeletedStats.people > 0) {
                                        displayToastMessage("Successfully removed entry from the list!", Toast.LENGTH_SHORT)
                                    }

                                }

                            }
                            is Resource.Error -> {
                                handleError(eventSyncResponseResource.error, "Error removing entry item ")
                            }
                            else -> {}

                        }
                    }
                    is TraktListsViewModel.Event.AddListEvent -> {}
                    is TraktListsViewModel.Event.DeleteListEvent -> {}
                    is TraktListsViewModel.Event.EditListEvent -> {}
                }
            }
        }
    }

    private fun initRecycler() {
        recyclerView = bindings!!.traktlistsfragmentRecyclerview
        val lm = getResponsiveGridLayoutManager(requireContext(), null)

        listEntryAdapter = ListEntryAdapter(glide, tmdbImageLoader, sharedPreferences) { traktId, action, type,  selectedItem ->
            when(type) {
                Type.MOVIE -> {
                    when(action) {
                        ListEntryAdapter.ACTION_VIEW -> {
                            if(traktId != null) {
                                val movieIntent = Intent(requireContext(), MovieDetailsActivity::class.java)
                                movieIntent.putExtra(
                                    MovieDetailsActivity.MOVIE_DATA_KEY, MovieDataModel(
                                        traktId,
                                        selectedItem.movie?.tmdb_id,
                                        selectedItem.movie?.title,
                                        selectedItem.movie?.first_aired?.year ?: 0
                                    )
                                )

                                startActivity(movieIntent)
                            }


                        }
                        ListEntryAdapter.ACTION_REMOVE -> {
                            if(traktId != null) {
                                removeListItemEntry(traktId, Type.MOVIE)

                            }
                        }
                    }

                }
                Type.SHOW -> {
                    when(action) {
                        ListEntryAdapter.ACTION_VIEW -> {

                            if(traktId != null) {
                                val showIntent = Intent(requireContext(), ShowDetailsActivity::class.java)
                                showIntent.putExtra(
                                    ShowDetailsActivity.SHOW_DATA_KEY,
                                    ShowDataModel(
                                        traktId,
                                        selectedItem.show?.tmdb_id,
                                        selectedItem.show?.title
                                    )
                                )

                                startActivity(showIntent)
                            }


                        }
                        ListEntryAdapter.ACTION_REMOVE -> {
                            if(traktId != null) {
                                removeListItemEntry(traktId, Type.SHOW)

                            }

                        }
                    }
                }
                Type.EPISODE -> {
                    when(action) {
                        ListEntryAdapter.ACTION_VIEW -> {

                            if(selectedItem.episodeShow?.trakt_id != null) {
                                val episodeIntent = Intent(requireContext(), EpisodeDetailsActivity::class.java)
                                episodeIntent.putExtra(
                                    EpisodeDetailsActivity.EPISODE_DATA_KEY,
                                    EpisodeDataModel(
                                        selectedItem.episodeShow.trakt_id,
                                        selectedItem.episode?.tmdb_id,
                                        selectedItem.episode?.season_number ?: 0,
                                        selectedItem.episode?.episode_number ?: 0,
                                        selectedItem.episodeShow.title
                                    )
                                )

                                startActivity(episodeIntent)
                            }


                        }
                        ListEntryAdapter.ACTION_REMOVE -> {
                            if(selectedItem.episode?.trakt_id != null) {
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

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {
        super.onRefresh()

        viewModel.onRefresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        bindings = null
    }

    private fun displayToastMessage(messageText: String, length: Int) {
        Toast.makeText(requireContext(), messageText, length).show()
    }

    companion object {
        @JvmStatic
        fun newInstance() = ListItemsFragment()
    }
}