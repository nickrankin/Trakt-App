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
import androidx.paging.map
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.adapter.search.ShowSearchLoadStateAdapter
import com.nickrankin.traktapp.adapter.search.ShowSearchResultsAdapter
import com.nickrankin.traktapp.databinding.ActivityShowSearchResultsBinding
import com.nickrankin.traktapp.helper.PosterImageLoader
import com.nickrankin.traktapp.model.search.ShowSearchViewModel
import com.nickrankin.traktapp.repo.shows.ShowDetailsRepository
import com.nickrankin.traktapp.ui.shows.OnNavigateToShow
import com.nickrankin.traktapp.ui.shows.ShowDetailsActivity
import com.uwetrottmann.trakt5.entities.Show
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
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

        adapter = ShowSearchResultsAdapter(glide, posterImageLoader, callback = { result ->
            if(result != null) {
                val show = result.show
                navigateToShow(
                    show?.ids?.trakt ?: 0,
                    show?.ids?.tmdb ?: 0,
                    show?.language
                )
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

    override fun navigateToShow(traktId: Int, tmdbId: Int, language: String?) {
        if(tmdbId == 0) {
            Toast.makeText(this, "Trakt does not have this show's TMDB", Toast.LENGTH_LONG).show()
            return
        }


        val intent = Intent(this, ShowDetailsActivity::class.java)
        intent.putExtra(ShowDetailsRepository.SHOW_TRAKT_ID_KEY, traktId)
        intent.putExtra(ShowDetailsRepository.SHOW_TMDB_ID_KEY, tmdbId)
        intent.putExtra(ShowDetailsRepository.SHOW_LANGUAGE_KEY, language)

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