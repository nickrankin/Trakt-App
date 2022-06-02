package com.nickrankin.traktapp.ui.lists

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.adapter.lists.ListEntryAdapter
import com.nickrankin.traktapp.databinding.ActivityListItemsBinding
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.lists.ListEntryViewModel
import com.nickrankin.traktapp.repo.movies.MovieDetailsRepository
import com.nickrankin.traktapp.repo.shows.episodedetails.EpisodeDetailsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.ui.movies.moviedetails.MovieDetailsActivity
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsActivity
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ListItemsActivity"
@AndroidEntryPoint
class ListItemsActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {

    private val viewModel: ListEntryViewModel by viewModels()
    private lateinit var bindings: ActivityListItemsBinding

    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var recyclerView: RecyclerView
    private lateinit var listEntryAdapter: ListEntryAdapter

    private var listTraktId: Int = 0
    private lateinit var listName: String

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = ActivityListItemsBinding.inflate(layoutInflater)

        setContentView(bindings.root)

        swipeRefreshLayout = bindings.traktlistsactivitySwipeRefresh
        swipeRefreshLayout.setOnRefreshListener(this)
        progressBar = bindings.traktlistsactivityProgressbar

        setSupportActionBar(bindings.toolbarLayout.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        listTraktId = intent.extras?.getInt(ListEntryViewModel.LIST_ID_KEY, 0) ?: 0
        listName = intent.extras?.getString(ListEntryViewModel.LIST_NAME_KEY, "Unknown List") ?: "Unknown List"

        supportActionBar?.title = "$listName Entries"

        initRecycler()
        getListEntries()
        getEvents()
    }

    private fun getListEntries() {
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

                        listEntryAdapter.submitList(listEntriesData.data)
                        Log.d(TAG, "getListEntries: Got ${listEntriesData.data?.size} entries")
//
//                        listEntriesData.data?.map {
//                            Log.d(TAG, "getListEntries: Got $it")
//                        }
                    }
                    is Resource.Error -> {
                        progressBar.visibility = View.GONE
                        if(swipeRefreshLayout.isRefreshing) {
                            swipeRefreshLayout.isRefreshing = false
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
                        }
                    }
                }
            }
        }
    }

    private fun initRecycler() {
        recyclerView = bindings.traktlistsactivityRecyclerview
        val lm = LinearLayoutManager(this)

        listEntryAdapter = ListEntryAdapter(glide, tmdbImageLoader, sharedPreferences) { traktId, action, type,  selectedItem ->
            when(type) {
                Type.MOVIE -> {
                    when(action) {
                        ListEntryAdapter.ACTION_VIEW -> {
                            val movieIntent = Intent(this, MovieDetailsActivity::class.java)
                            movieIntent.putExtra(MovieDetailsActivity.MOVIE_DATA_KEY, MovieDataModel(
                                traktId,
                                selectedItem.movie?.tmdb_id,
                                selectedItem.movie?.title,
                                        selectedItem.movie?.release_date?.year ?: 0
                            ))

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
                            val showIntent = Intent(this, ShowDetailsActivity::class.java)
                            showIntent.putExtra(ShowDetailsActivity.SHOW_DATA_KEY,
                            ShowDataModel(
                                traktId,
                                selectedItem.show?.tmdb_id,
                                selectedItem.show?.title
                            ))

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
                            val episodeIntent = Intent(this, EpisodeDetailsActivity::class.java)
                            episodeIntent.putExtra(EpisodeDetailsActivity.EPISODE_DATA_KEY,
                            EpisodeDataModel(
                                selectedItem.episodeShow?.trakt_id ?: 0,
                                selectedItem.episodeShow?.tmdb_id,
                                selectedItem.episode?.season ?: 0,
                                selectedItem.episode?.episode ?: 0,
                                selectedItem.show?.title
                            ))

                            startActivity(episodeIntent)
                        }
                        ListEntryAdapter.ACTION_REMOVE -> {
                            removeListItemEntry(selectedItem.episode?.trakt_id ?: 0, Type.EPISODE)
                        }
                    }

                }
            }
        }

        recyclerView.layoutManager = lm
        recyclerView.adapter = listEntryAdapter

    }

    private fun removeListItemEntry(listEntryItemTraktId: Int, type: Type) {
        val alertDialog = AlertDialog.Builder(this)
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()

        return true
    }

    private fun displayToastMessage(messageText: String, length: Int) {
        Toast.makeText(this, messageText, length).show()
    }
}