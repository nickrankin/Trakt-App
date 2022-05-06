package com.nickrankin.traktapp.ui.search

import android.app.SearchManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.map
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.adapter.search.ShowSearchLoadStateAdapter
import com.nickrankin.traktapp.adapter.search.ShowSearchResultsAdapter
import com.nickrankin.traktapp.databinding.ActivityShowSearchResultsBinding
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.model.search.ShowSearchViewModel
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.ui.shows.OnNavigateToShow
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import com.uwetrottmann.trakt5.entities.Show
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import javax.inject.Inject

private const val TAG = "ShowSearchResultsActivi"
@AndroidEntryPoint
class ShowSearchResultsActivity : AppCompatActivity(), OnNavigateToShow {
    private lateinit var bindings: ActivityShowSearchResultsBinding

    private lateinit var adapter: ShowSearchResultsAdapter
    private lateinit var recyclerView: RecyclerView

    private lateinit var progressBar: ProgressBar
    private lateinit var noResultsBanner: TextView


    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader

    private val viewModel: ShowSearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = ActivityShowSearchResultsBinding.inflate(layoutInflater)

        setContentView(bindings.root)

        setSupportActionBar(bindings.toolbarLayout.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initRecycler()

        intent.getStringExtra(SearchManager.QUERY)?.also { query ->
            Log.d(TAG, "onCreate: Got Search Query $query", )

            supportActionBar?.title = "Search results: $query"

            doSearch(query)
        }

    }

    private fun doSearch(query: String) {
        lifecycleScope.launchWhenStarted {
            viewModel.doSearch(query).collectLatest { pagingData ->
                progressBar.visibility = View.GONE
                noResultsBanner.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

//                adapter.addLoadStateListener { loadState ->
//                    if(loadState.append.endOfPaginationReached) {
//                        if(adapter.itemCount < 1) {
//                            noResultsBanner.visibility = View.VISIBLE
//                        } else {
//                            noResultsBanner.visibility = View.GONE
//                            recyclerView.visibility = View.VISIBLE
//                        }
//                    }
//                }

                adapter.submitData(pagingData)
            }
        }
    }

    private fun initRecycler() {
        progressBar = bindings.showsearchresultsactivityProgressbar
        noResultsBanner = bindings.showsearchresultsactivityNoResults

        recyclerView = bindings.showsearchresultsactivityRecyclerview
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ShowSearchResultsAdapter(glide, tmdbImageLoader, callback = { result ->
            if(result != null) {
                val show = result.show
                navigateToShow(
                    show?.ids?.trakt ?: 0,
                    show?.ids?.tmdb ?: 0,
                    show.title,
                    show?.language
                )
            } else {
                displayMessageToast("Could not load item details", Toast.LENGTH_LONG)
            }
        })

        recyclerView.adapter =adapter.withLoadStateHeaderAndFooter(
            header = ShowSearchLoadStateAdapter(adapter),
            footer = ShowSearchLoadStateAdapter(adapter)
        )

        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow.collectLatest { loadStates ->
                bindings.showsearchresultsactivityProgressbar.visibility = if(loadStates.refresh is LoadState.Loading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow
                // Only emit when REFRESH LoadState for RemoteMediator changes.
                .distinctUntilChangedBy { it.refresh }
                // Only react to cases where Remote REFRESH completes i.e., NotLoading.
                .filter { it.refresh is LoadState.NotLoading }
                .collect { recyclerView.scrollToPosition(0) }
        }
    }

    override fun navigateToShow(traktId: Int, tmdbId: Int, showTitle: String?, language: String?) {
        if(tmdbId == 0) {
            Toast.makeText(this, "Trakt does not have this show's TMDB", Toast.LENGTH_LONG).show()
            return
        }


        val intent = Intent(this, ShowDetailsActivity::class.java)
        intent.putExtra(ShowDetailsRepository.SHOW_TRAKT_ID_KEY, traktId)

        startActivity(intent)
    }



    private fun displayMessageToast(message: String, length: Int) {
        Toast.makeText(this, message, length).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}