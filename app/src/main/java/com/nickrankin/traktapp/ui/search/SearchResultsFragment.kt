package com.nickrankin.traktapp.ui.search

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.google.android.material.chip.Chip
import com.nickrankin.traktapp.BaseActivity
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.OnNavigateToEntity
import com.nickrankin.traktapp.adapter.search.ShowSearchLoadStateAdapter
import com.nickrankin.traktapp.adapter.search.SearchResultsAdapter
import com.nickrankin.traktapp.databinding.ActivityShowSearchResultsBinding
import com.nickrankin.traktapp.databinding.FragmentSplitviewLayoutBinding
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.search.SearchViewModel
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import org.apache.commons.lang3.StringUtils
import javax.inject.Inject

private const val TAG = "SearchResultsFrag"
@AndroidEntryPoint
class SearchResultsFragment : BaseFragment() {

    private var _bindings: ActivityShowSearchResultsBinding? = null
    private val bindings get() = _bindings!!

    private lateinit var adapter: SearchResultsAdapter
    private lateinit var recyclerView: RecyclerView

    private lateinit var progressBar: ProgressBar
    private lateinit var noResultsBanner: TextView

    private lateinit var chipMovies: Chip
    private lateinit var chipShows: Chip

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader

    private val viewModel: SearchViewModel by activityViewModels()



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _bindings = ActivityShowSearchResultsBinding.inflate(layoutInflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        chipMovies = bindings.searchresultsactivityChipMovies
        chipShows = bindings.searchresultsactivityChipShows

        val showChips = arguments?.getBoolean(SearchResultsActivity.SHOW_CHIPS) ?: false

        initRecycler()

        val query = arguments?.getString(SearchManager.QUERY)
        val type = arguments?.getString(SearchResultsActivity.SEARCH_TYPE_KEY)
        val genre = arguments?.getString(SearchResultsActivity.GENRE_KEY)

        if(showChips) {
            bindSearchChips(query ?: "")
        } else {
            bindings.searchresultsactivityChipGroup.visibility = View.GONE
        }

        // Handle Genre use case
        if(!genre.isNullOrBlank()) {

            // need this to ensure back button is displayed
            (activity as OnNavigateToEntity).enableOverviewLayout(true)

            updateTitle("Tagged: ${StringUtils.capitalize(genre) }")

            doSearchWithGenre(query ?: "", genre)
        } else {

            if(query == null || type == null) {
                throw RuntimeException("A search Query and Type must be provided! (Query: $query Type: $type)")
            }

            val searchType = when(type) {
                SearchResultsActivity.TYPE_MOVIE_KEY -> {
                    chipMovies.isChecked = true

                    Type.MOVIE
                } SearchResultsActivity.TYPE_SHOW_KEY -> {
                    chipShows.isChecked = true

                    Type.SHOW
                }
                else -> {
                    chipMovies.isChecked = true

                    Type.MOVIE
                }

            }



            Log.d(TAG, "onCreate: Got Search Query $query", )

            updateTitle("Search results: $query")

            doSearch(query, searchType)
        }


    }

    private fun bindSearchChips(query: String) {
        bindings.searchresultsactivityChipGroup.visibility = View.VISIBLE

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

    private fun doSearchWithGenre(query: String, genre: String) {
        lifecycleScope.launchWhenStarted {
            viewModel.doSearchWithGenre(query, null, genre).collectLatest { pagingData ->
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
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = SearchResultsAdapter(tmdbImageLoader, callback = { resultClicked ->
            if(resultClicked != null) {
                when(resultClicked.type) {
                    "movie" -> {
                        val movie = resultClicked.movie


                        (activity as OnNavigateToEntity).navigateToMovie(
                            MovieDataModel(
                                movie?.ids?.trakt ?: 0,
                                movie?.ids?.tmdb,
                                movie?.title,
                                movie?.year
                            )
                        )

                    }
                    "show" -> {
                        val show = resultClicked.show

                        (activity as OnNavigateToEntity).navigateToShow(
                            ShowDataModel(
                                show?.ids?.trakt ?: 0,
                                show?.ids?.tmdb ?: 0,
                                show?.title
                            )
                        )
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
        Toast.makeText(requireContext(), message, length).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        recyclerView.adapter = null
        _bindings = null

    }

    companion object {

        @JvmStatic
        fun newInstance() = SearchResultsFragment()

    }
}