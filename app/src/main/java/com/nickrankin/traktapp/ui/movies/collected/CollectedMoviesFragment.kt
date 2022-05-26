package com.nickrankin.traktapp.ui.movies.collected

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.movies.CollectedMoviesAdapter
import com.nickrankin.traktapp.databinding.FragmentCollectedMoviesBinding
import com.nickrankin.traktapp.helper.OnTitleChangeListener
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.movies.CollectedMoviesViewModel
import com.nickrankin.traktapp.repo.movies.MovieDetailsRepository
import com.nickrankin.traktapp.ui.movies.moviedetails.MovieDetailsActivity
import com.uwetrottmann.trakt5.enums.SortBy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "CollectedMoviesFragment"
@AndroidEntryPoint
class CollectedMoviesFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var bindings: FragmentCollectedMoviesBinding

    private lateinit var swipeLayout: SwipeRefreshLayout

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CollectedMoviesAdapter

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader

    private val viewModel: CollectedMoviesViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = FragmentCollectedMoviesBinding.inflate(inflater)
        // Inflate the layout for this fragment
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeLayout = bindings.collectedmoviesfragmentSwipeLayout
        swipeLayout.setOnRefreshListener(this)

        setHasOptionsMenu(true)

        updateTitle("Collected Movies")

        initRecycler()
        getCollectedMovies()

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.collected_filter_menu, menu)

    }

    private fun getCollectedMovies() {
        lifecycleScope.launchWhenStarted {
            viewModel.collectedMovies.collectLatest { collectedMoviesResource ->
                when(collectedMoviesResource) {
                    is Resource.Loading -> {
                        bindings.collectedmoviesfragmentProgressbar.visibility = View.VISIBLE
                        Log.d(TAG, "getCollectedMovies: Loading collected")
                    }
                    is Resource.Success -> {
                        bindings.collectedmoviesfragmentProgressbar.visibility = View.GONE

                        if(swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }

                        adapter.submitList(collectedMoviesResource.data)
//                        collectedShowsResource.data?.map { show ->
//                            Log.d(TAG, "getCollectedMovies: Got show $show")
//                        }
                    }
                    is Resource.Error -> {
                        bindings.collectedmoviesfragmentProgressbar.visibility = View.GONE
                        if(swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }
                        collectedMoviesResource.error?.printStackTrace()
                    }
                }
            }
        }
    }

    private fun initRecycler() {
        recyclerView = bindings.collectedmoviesfragmentRecyclerview

        val lm = FlexboxLayoutManager(requireContext())

        lm.flexDirection = FlexDirection.ROW
        lm.justifyContent = JustifyContent.SPACE_EVENLY


        adapter = CollectedMoviesAdapter(glide, tmdbImageLoader, callback = { movie, type ->
            val intent = Intent(requireContext(), MovieDetailsActivity::class.java)
            intent.putExtra(MovieDetailsActivity.MOVIE_DATA_KEY,
                MovieDataModel(
                    movie.trakt_id,
                    movie.tmdb_id,
                    movie.title
                )
            )

            startActivity(intent)
        })

        recyclerView.layoutManager = lm

        recyclerView.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {
        viewModel.onRefresh()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.collectedfiltermenu_title -> {
                viewModel.sortMovies(CollectedMoviesViewModel.SORT_TITLE)
            }
            R.id.collectedfiltermenu_collected_at -> {
                viewModel.sortMovies(CollectedMoviesViewModel.SORT_COLLECTED_AT)

            }
            R.id.collectedfiltermenu_year -> {
                viewModel.sortMovies(CollectedMoviesViewModel.SORT_YEAR)
            }
        }

        return false
    }

    companion object {

        @JvmStatic
        fun newInstance() =
            CollectedMoviesFragment()
    }
}