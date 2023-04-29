package com.nickrankin.traktapp.ui.movies.collected

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.OnNavigateToEntity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.AdaptorActionControls
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.adapter.movies.CollectedMoviesAdapter
import com.nickrankin.traktapp.dao.movies.model.CollectedMovie
import com.nickrankin.traktapp.databinding.FragmentSplitviewLayoutBinding
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.movies.CollectedMoviesViewModel
import com.nickrankin.traktapp.ui.movies.MoviesMainActivity
import com.nickrankin.traktapp.ui.movies.moviedetails.MovieDetailsFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "CollectedMoviesFragment"

@AndroidEntryPoint
class CollectedMoviesFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var bindings: FragmentSplitviewLayoutBinding


    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CollectedMoviesAdapter

    private var currentViewType = MediaEntryBaseAdapter.VIEW_TYPE_POSTER

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader

    private val viewModel: CollectedMoviesViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = FragmentSplitviewLayoutBinding.inflate(inflater)
        // Inflate the layout for this fragment
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        updateTitle("Collected Movies")

        if(!isLoggedIn) {
            handleLoggedOutState(this.id)
        }

        // Enable the Drawer Layout and Navigation Tabs
        (activity as OnNavigateToEntity).enableOverviewLayout(false)

        initRecycler()

        getCollectedMovies()
        getViewTypeState()
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.collected_filter_menu, menu)
        inflater.inflate(R.menu.layout_switcher_menu, menu)

    }

    private fun getCollectedMovies() {
        lifecycleScope.launchWhenStarted {
            viewModel.collectedMovies.collectLatest { collectedMoviesResource ->
                when (collectedMoviesResource) {
                    is Resource.Loading -> {
                        bindings.splitviewlayoutProgressbar.visibility = View.VISIBLE
                        Log.d(TAG, "getCollectedMovies: Loading collected")
                    }
                    is Resource.Success -> {
                        bindings.splitviewlayoutProgressbar.visibility = View.GONE

                        val collectedMovies = collectedMoviesResource.data

                        if(collectedMovies != null && collectedMovies.isNotEmpty()) {
                            bindings.splitviewlayoutMainGroup.visibility = View.VISIBLE
                            bindings.splitviewlayoutMessageContainer.visibility = View.GONE

                            adapter.submitList(collectedMoviesResource.data) {
                                recyclerView.scrollToPosition(0)
                            }

                        } else {
                            handleNoResults()
                            adapter.submitList(collectedMoviesResource.data)
                        }
                    }
                    is Resource.Error -> {
                        bindings.splitviewlayoutProgressbar.visibility = View.GONE

                        val collectedMovies = collectedMoviesResource.data

                        if(collectedMovies != null && collectedMovies.isNotEmpty()) {
                            bindings.splitviewlayoutMainGroup.visibility = View.VISIBLE
                            bindings.splitviewlayoutMessageContainer.visibility = View.GONE

                            adapter.submitList(collectedMoviesResource.data)
                        } else {
                            handleNoResults()
                            adapter.submitList(collectedMoviesResource.data)
                        }

                        (activity as IHandleError).showErrorSnackbarRetryButton(
                            collectedMoviesResource.error,
                            bindings.root
                        ) {
                            viewModel.onRefresh()
                        }
                    }
                }
            }
        }
    }

    private fun handleNoResults() {
        bindings.splitviewlayoutMainGroup.visibility = View.GONE
        bindings.splitviewlayoutMessageContainer.visibility = View.VISIBLE
        bindings.splitviewlayoutMessageContainer.text = "You have nothing in your Trakt Collection :( "
    }

    private fun initRecycler() {
        recyclerView = bindings.splitviewlayoutRecyclerview

        switchRecyclerViewLayoutManager(requireContext(), recyclerView, MediaEntryBaseAdapter.VIEW_TYPE_POSTER)

        adapter = CollectedMoviesAdapter(tmdbImageLoader, sharedPreferences,
        AdaptorActionControls(context?.getDrawable(R.drawable.ic_baseline_delete_forever_24), "REmove from Collection", false, R.menu.collected_popup_menu,
            entrySelectedCallback = {selectedItem ->
                navigateToMovie(selectedItem)
            },
            buttonClickedCallback = { selectedMovie -> Toast.makeText(requireContext(), "Deleted ${selectedMovie.title}", Toast.LENGTH_SHORT).show() },
            menuItemSelectedCallback = { selectedMovie, menuItem ->
                when(menuItem) {
                    R.id.collectedpopupmenu_delete -> {
AlertDialog.Builder(requireContext())
                            .setTitle("Do you want to delete ${selectedMovie.title}")
                            .setMessage("Really remove ${selectedMovie.title} from your collection?")
                            .setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->
                                lifecycleScope.launchWhenStarted {
                                    viewModel.deleteCollectedMovie(selectedMovie.trakt_id)

                                }
                            })
                            .setNegativeButton("No", DialogInterface.OnClickListener { dialogInterface, i -> dialogInterface.dismiss() })
                            .create()
                            .show()
                    }
                    else -> {
                        Log.e(TAG, "initRecycler: Invalid menu $menuItem", )
                    }
                }
            })
        )

        recyclerView.adapter = adapter
    }

    private fun navigateToMovie(collectedMovie: CollectedMovie) {
        (activity as OnNavigateToEntity).navigateToMovie(
            MovieDataModel(
                collectedMovie.trakt_id,
                collectedMovie.tmdb_id,
                collectedMovie.title,
                collectedMovie.release_date?.year
            )
        )


    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {
        super.onRefresh()

        viewModel.onRefresh()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.collectedfiltermenu_title -> {
                viewModel.applySorting(ISortable.SORT_BY_TITLE)
            }
            R.id.collectedfiltermenu_collected_at -> {
                viewModel.applySorting(CollectedMoviesViewModel.SORT_COLLECTED_AT)

            }
            R.id.collectedfiltermenu_year -> {
                viewModel.applySorting(ISortable.SORT_BY_YEAR)
            }
            R.id.menu_switch_layout -> {
                lifecycleScope.launchWhenStarted {
                    viewModel.switchViewType()
                }
            }
        }

        return false
    }

    private fun getViewTypeState() {
        lifecycleScope.launchWhenStarted {
            viewModel.viewType.collectLatest { viewType ->
                adapter.switchView(viewType)

                switchRecyclerViewLayoutManager(requireContext(), recyclerView, viewType)


                recyclerView.scrollToPosition(0)

                adapter.notifyDataSetChanged()
            }
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() =
            CollectedMoviesFragment()
    }
}