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
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.lists.ListEntryViewModel
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

    private lateinit var bindings: FragmentListItemsBinding

    private val viewModel: ListEntryViewModel by viewModels()

    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var recyclerView: RecyclerView
    private lateinit var listEntryAdapter: ListEntryAdapter

    private var listTraktId: Int = 0
    private lateinit var listName: String

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        bindings = FragmentListItemsBinding.inflate(inflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        swipeRefreshLayout = bindings.traktlistsfragmentSwipeRefresh
        swipeRefreshLayout.setOnRefreshListener(this)
        progressBar = bindings.traktlistsfragmentProgressbar

        listTraktId = arguments?.getInt(ListEntryViewModel.LIST_ID_KEY, 0) ?: 0
        listName = arguments?.getString(ListEntryViewModel.LIST_NAME_KEY, "Unknown List") ?: "Unknown List"

        if(!isLoggedIn) {
            handleLoggedOutState(this.id)
        }

        updateTitle("$listName Entries")

        initRecycler()
        getListEntries()
        getEvents()
    }

    private fun getListEntries() {
        val messageContainerTextView = bindings.traktlistsfragmentMessageContainer

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

                            messageContainerTextView.text = "You do not have any entries in list $listName! Why not add some?"
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

                            messageContainerTextView.text = "You do not have any entries in list $listName! Why not add some?"
                        }

                        Log.e(TAG, "getListEntries: Error getting list entrties ${listEntriesData.error?.message}", )

                        listEntriesData.error?.printStackTrace()
                    }
                }
            }
        }
    }

    private fun getEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.event.collectLatest { event ->
                when(event) {
                    is ListEntryViewModel.Event.RemoveEntryEvent -> {
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
                                displayToastMessage("getEvents: Error removing entry item ${eventSyncResponseResource.error?.localizedMessage}", Toast.LENGTH_SHORT)
                                eventSyncResponseResource.error?.printStackTrace()
                            }
                            else -> {}

                        }
                    }
                }
            }
        }
    }

    private fun initRecycler() {
        recyclerView = bindings.traktlistsfragmentRecyclerview
        val lm = LinearLayoutManager(requireContext())

        listEntryAdapter = ListEntryAdapter(glide, tmdbImageLoader, sharedPreferences) { traktId, action, type,  selectedItem ->
            when(type) {
                Type.MOVIE -> {
                    when(action) {
                        ListEntryAdapter.ACTION_VIEW -> {
                            val movieIntent = Intent(requireContext(), MovieDetailsActivity::class.java)
                            movieIntent.putExtra(
                                MovieDetailsActivity.MOVIE_DATA_KEY, MovieDataModel(
                                traktId,
                                selectedItem.movie?.tmdb_id,
                                selectedItem.movie?.title,
                                selectedItem.movie?.release_date?.year ?: 0
                            )
                            )

                            startActivity(movieIntent)
                        }
                        ListEntryAdapter.ACTION_REMOVE -> {
                            removeListItemEntry(traktId, Type.MOVIE)
                        }
                    }

                }
                Type.SHOW -> {
                    when(action) {
                        ListEntryAdapter.ACTION_VIEW -> {
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
                        ListEntryAdapter.ACTION_REMOVE -> {
                            removeListItemEntry(traktId, Type.SHOW)
                        }
                    }
                }
                Type.EPISODE -> {
                    when(action) {
                        ListEntryAdapter.ACTION_VIEW -> {
                            val episodeIntent = Intent(requireContext(), EpisodeDetailsActivity::class.java)
                            episodeIntent.putExtra(
                                EpisodeDetailsActivity.EPISODE_DATA_KEY,
                                EpisodeDataModel(
                                    selectedItem.episodeShow?.trakt_id ?: 0,
                                    selectedItem.episodeShow?.tmdb_id,
                                    selectedItem.episode?.season ?: 0,
                                    selectedItem.episode?.episode ?: 0,
                                    selectedItem.show?.title
                                )
                            )

                            startActivity(episodeIntent)
                        }
                        ListEntryAdapter.ACTION_REMOVE -> {
                            removeListItemEntry(selectedItem.episode?.trakt_id ?: 0, Type.EPISODE)
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
        viewModel.onRefresh()
    }

    private fun displayToastMessage(messageText: String, length: Int) {
        Toast.makeText(requireContext(), messageText, length).show()
    }

    companion object {
        @JvmStatic
        fun newInstance() = ListItemsFragment()
    }
}