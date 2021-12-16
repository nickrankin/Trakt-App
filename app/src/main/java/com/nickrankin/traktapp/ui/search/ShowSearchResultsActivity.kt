package com.nickrankin.traktapp.ui.search

import android.app.SearchManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.adapter.search.ShowSearchLoadStateAdapter
import com.nickrankin.traktapp.adapter.search.ShowSearchResultsAdapter
import com.nickrankin.traktapp.databinding.ActivityShowSearchResultsBinding
import com.nickrankin.traktapp.helper.PosterImageLoader
import com.nickrankin.traktapp.model.search.ShowSearchViewModel
import com.nickrankin.traktapp.repo.shows.ShowDetailsRepository
import com.nickrankin.traktapp.ui.shows.ShowDetailsActivity
import com.uwetrottmann.trakt5.entities.Show
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "ShowSearchResultsActivi"
@AndroidEntryPoint
class ShowSearchResultsActivity : AppCompatActivity() {
    private lateinit var bindings: ActivityShowSearchResultsBinding

    private lateinit var adapter: ShowSearchResultsAdapter
    private lateinit var recyclerView: RecyclerView


    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var posterImageLoader: PosterImageLoader

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
                adapter.submitData(pagingData)
            }
        }
    }

    private fun initRecycler() {
        recyclerView = bindings.showsearchresultsactivityRecyclerview
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ShowSearchResultsAdapter(glide, posterImageLoader, callback = { result ->
            if(result != null) {
                navigateToShow(result.show)
            } else {
                displayMessageToast("Could not load item details", Toast.LENGTH_LONG)
            }
        })

        adapter.withLoadStateHeaderAndFooter(
            header = ShowSearchLoadStateAdapter(adapter),
            footer = ShowSearchLoadStateAdapter(adapter)
        )

        recyclerView.adapter = adapter
    }

    private fun navigateToShow(show: Show?) {
        val intent = Intent(this, ShowDetailsActivity::class.java)
        intent.putExtra(ShowDetailsRepository.SHOW_TRAKT_ID_KEY, show?.ids?.trakt ?: 0)
        intent.putExtra(ShowDetailsRepository.SHOW_TMDB_ID_KEY, show?.ids?.tmdb ?: 0)
        intent.putExtra(ShowDetailsRepository.SHOW_LANGUAGE_KEY, show?.language)

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