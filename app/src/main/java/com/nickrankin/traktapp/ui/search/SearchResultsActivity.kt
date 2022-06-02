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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.google.android.material.chip.Chip
import com.nickrankin.traktapp.BaseActivity
import com.nickrankin.traktapp.adapter.search.ShowSearchLoadStateAdapter
import com.nickrankin.traktapp.adapter.search.ShowSearchResultsAdapter
import com.nickrankin.traktapp.databinding.ActivityShowSearchResultsBinding
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.search.SearchViewModel
import com.nickrankin.traktapp.ui.movies.moviedetails.MovieDetailsActivity
import com.nickrankin.traktapp.ui.shows.OnNavigateToShow
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import javax.inject.Inject

private const val TAG = "ShowSearchResultsActivi"
@AndroidEntryPoint
class SearchResultsActivity : BaseActivity() {
    private lateinit var bindings: ActivityShowSearchResultsBinding

    private lateinit var adapter: ShowSearchResultsAdapter
    private lateinit var recyclerView: RecyclerView

    private lateinit var progressBar: ProgressBar
    private lateinit var noResultsBanner: TextView

    private lateinit var chipMovies: Chip
    private lateinit var chipShows: Chip

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader

    private val viewModel: SearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = ActivityShowSearchResultsBinding.inflate(layoutInflater)

        chipMovies = bindings.searchresultsactivityChipMovies
        chipShows = bindings.searchresultsactivityChipShows

        setContentView(bindings.root)

        setSupportActionBar(bindings.toolbarLayout.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Search defaults to movies
        setSearchType(TYPE_MOVIE_KEY)

        initRecycler()

        val query = intent.getStringExtra(SearchManager.QUERY)
        val type = intent.getStringExtra(SEARCH_TYPE_KEY)

        if(query == null || type == null) {
            throw RuntimeException("A search Query and Type must be provided!")
        }

        val searchType = when(type) {
            TYPE_MOVIE_KEY -> {
                chipMovies.isChecked = true

                Type.MOVIE
            } TYPE_SHOW_KEY -> {
                chipShows.isChecked = true

                Type.SHOW
            }
            else -> {
                chipMovies.isChecked = true

                Type.MOVIE
            }

        }

        bindSearchChips(query)

            Log.d(TAG, "onCreate: Got Search Query $query", )

            supportActionBar?.title = "Search results: $query"

            doSearch(query, searchType)
    }

    private fun bindSearchChips(query: String) {

        chipShows.setOnClickListener {
            chipShows.isChecked = true
            chipMovies.isChecked = false

            doSearch(query, Type.SHOW)
        }

        chipMovies.setOnClickListener {
            chipShows.isChecked = false
            chipMovies.isChecked = true

            doSearch(query, Type.MOVIE)
        }
    }

    private fun doSearch(query: String, type: Type) {
        lifecycleScope.launchWhenStarted {
            viewModel.doSearch(query, type).collectLatest { pagingData ->
                progressBar.visibility = View.GONE
                noResultsBanner.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

                adapter.submitData(pagingData)
            }
        }
    }

    private fun initRecycler() {
        progressBar = bindings.showsearchresultsactivityProgressbar
        noResultsBanner = bindings.showsearchresultsactivityNoResults

        recyclerView = bindings.showsearchresultsactivityRecyclerview
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ShowSearchResultsAdapter(tmdbImageLoader, callback = { resultClicked ->
            if(resultClicked != null) {
                when(resultClicked.type) {
                    "movie" -> {

                        val movie = resultClicked.movie
                        val movieIntent = Intent(this, MovieDetailsActivity::class.java)
                        movieIntent.putExtra(MovieDetailsActivity.MOVIE_DATA_KEY,
                        MovieDataModel(
                            movie?.ids?.trakt ?: 0,
                            movie?.ids?.tmdb,
                            movie?.title,
                            movie?.year
                        )
                        )

                        startActivity(movieIntent)

                    }
                    "show" -> {
                        val show = resultClicked.show

                        val showIntent = Intent(this, ShowDetailsActivity::class.java)

                        showIntent.putExtra(ShowDetailsActivity.SHOW_DATA_KEY,
                            ShowDataModel(
                                show?.ids?.trakt ?: 0,
                                show?.ids?.tmdb ?: 0,
                                show?.title
                            )
                        )
                        startActivity(showIntent)

                    }
                    "else" -> {
                        Log.e(TAG, "initRecycler: Unknown result type ${resultClicked.type}", )
                    }
                }


            } else {
                displayMessageToast("Could not load item details", Toast.LENGTH_LONG)
            }
        })

        adapter.addLoadStateListener { loadState ->
            if(loadState.append.endOfPaginationReached) {
                if(adapter.itemCount < 1) {
                    noResultsBanner.visibility = View.VISIBLE
                } else {
                    noResultsBanner.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
        }

        recyclerView.adapter = adapter.withLoadStateHeaderAndFooter(
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

    private fun displayMessageToast(message: String, length: Int) {
        Toast.makeText(this, message, length).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        const val SEARCH_TYPE_KEY = "search_type"
        const val TYPE_MOVIE_KEY = "movie_type"
        const val TYPE_SHOW_KEY = "show_type"

    }
}