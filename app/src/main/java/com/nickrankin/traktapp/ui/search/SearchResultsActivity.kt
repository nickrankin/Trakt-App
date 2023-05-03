package com.nickrankin.traktapp.ui.search

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.google.android.material.chip.Chip
import com.nickrankin.traktapp.BaseActivity
import com.nickrankin.traktapp.OnNavigateToEntity
import com.nickrankin.traktapp.SplitViewActivity
import com.nickrankin.traktapp.adapter.search.ShowSearchLoadStateAdapter
import com.nickrankin.traktapp.adapter.search.SearchResultsAdapter
import com.nickrankin.traktapp.databinding.ActivityShowSearchResultsBinding
import com.nickrankin.traktapp.databinding.ActivitySplitviewBinding
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.search.SearchViewModel
import com.nickrankin.traktapp.ui.movies.moviedetails.MovieDetailsFragment
import com.nickrankin.traktapp.ui.person.PersonOverviewFragment
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsFragment
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsFragment
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import javax.inject.Inject

private const val TAG = "ShowSearchResultsActivi"

@AndroidEntryPoint
class SearchResultsActivity : SplitViewActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Search defaults to movies
        setSearchType(TYPE_MOVIE_KEY)

        val query = intent.getStringExtra(SearchManager.QUERY)
        val type = intent.getStringExtra(SEARCH_TYPE_KEY)
        val genre = intent.getStringExtra(GENRE_KEY)

        val showChips = intent.getBooleanExtra(SHOW_CHIPS, true)

        if (query == null || type == null) {
            // Query and type can be null if Genre is specified
            throw RuntimeException("A search Query and Type must be provided!")
        }

        Log.d(TAG, "onCreate: Got Search Query $query")

        supportActionBar?.title = "Search results: $query"

        doSearch(query, type, showChips)
    }

    private fun doSearch(query: String, type: String, showChips: Boolean) {
        val searchResultsFragment = SearchResultsFragment.newInstance()
        val bundle = Bundle()
        bundle.putString(SEARCH_TYPE_KEY, type)
        bundle.putString(SearchManager.QUERY, query)
        bundle.putBoolean(SHOW_CHIPS, showChips)

        searchResultsFragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .replace(bindings.splitviewactivityFirstContainer.id, searchResultsFragment)
            .commit()
    }

    override fun enableOverviewLayout(isEnabled: Boolean) {
        // always use overview layout for searches, no drawer
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val SEARCH_TYPE_KEY = "search_type"
        const val TYPE_MOVIE_KEY = "movie_type"
        const val TYPE_SHOW_KEY = "show_type"
        const val TYPE_GENRE = "type_genre"
        const val GENRE_KEY = "genre_key"
        const val SHOW_CHIPS = "show_chips"

    }
}