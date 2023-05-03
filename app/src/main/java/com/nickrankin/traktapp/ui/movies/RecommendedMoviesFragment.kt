package com.nickrankin.traktapp.ui.movies

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.OnNavigateToEntity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.AdaptorActionControls
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.adapter.movies.ReccomendedMoviesAdaptor
import com.nickrankin.traktapp.databinding.FragmentRecommendedMoviesBinding
import com.nickrankin.traktapp.databinding.FragmentSplitviewLayoutBinding
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.movies.RecommendedMoviesViewModel
import com.uwetrottmann.trakt5.entities.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "RecommendedMoviesFragme"

@AndroidEntryPoint
class RecommendedMoviesFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var bindings: FragmentSplitviewLayoutBinding

    private lateinit var progressBar: ProgressBar

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReccomendedMoviesAdaptor

    private val viewModel: RecommendedMoviesViewModel by activityViewModels()

    @Inject
    lateinit var tmdbPosterImageLoader: TmdbImageLoader

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = FragmentSplitviewLayoutBinding.inflate(inflater)
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        progressBar = bindings.splitviewlayoutProgressbar

        (activity as OnNavigateToEntity).enableOverviewLayout(false)

        updateTitle("Suggested Movies")

        initRecyclerView()
        getViewType()

        if(!isLoggedIn) {
            handleLoggedOutState(this.id)
        }

        getRecommendedMovies()
        getEvents()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.recommended_filter_menu, menu)
        inflater.inflate(R.menu.layout_switcher_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun getRecommendedMovies() {
        lifecycleScope.launchWhenStarted {
            viewModel.recommendedMovies.collectLatest { recommendedMoviesResource ->
                when (recommendedMoviesResource) {
                    is Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        recyclerView.visibility = View.VISIBLE


                        Log.d(TAG, "getRecommendedMovies: Loading recommendations")
                    }

                    is Resource.Success -> {
                        progressBar.visibility = View.GONE

                        Log.d(TAG, "getRecommendedMovies: Got recommendations successfully")

                        if(recommendedMoviesResource.data != null && recommendedMoviesResource.data!!.isNotEmpty()) {
                            bindings.splitviewlayoutRecyclerview.visibility = View.VISIBLE
                            bindings.splitviewlayoutMessageContainer.visibility = View.GONE

                            if(recommendedMoviesResource.data != null) {

                                adapter.submitList(recommendedMoviesResource.data!!.toMutableList()) {
                                    recyclerView.scrollToPosition(0)
                                }
                            }



                        } else {
                            bindings.splitviewlayoutRecyclerview.visibility = View.GONE
                            bindings.splitviewlayoutMessageContainer.visibility = View.VISIBLE

                            bindings.splitviewlayoutMessageContainer.text = "There are no recommended movies at this time!"
                        }
                    }

                    is Resource.Error -> {
                        progressBar.visibility = View.GONE

                        if(recommendedMoviesResource.data != null && recommendedMoviesResource.data!!.isNotEmpty()) {
                            bindings.splitviewlayoutRecyclerview.visibility = View.VISIBLE
                            bindings.splitviewlayoutMessageContainer.visibility = View.GONE

                            adapter.submitList(recommendedMoviesResource.data)
                        } else {
                            bindings.splitviewlayoutRecyclerview.visibility = View.GONE
                            bindings.splitviewlayoutMessageContainer.visibility = View.VISIBLE

                            bindings.splitviewlayoutMessageContainer.text = "There are no recommended movies at this time!"
                        }

                        (activity as IHandleError).showErrorSnackbarRetryButton(
                            recommendedMoviesResource.error,
                            bindings.root
                        ) {
                            viewModel.onRefresh()
                        }
                    }
                }
            }
        }
    }

    private fun getEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.event.collectLatest { event ->
                when (event) {
                    is RecommendedMoviesViewModel.Event.RemoveRecommendationEvent -> {
                        val response = event.response

                        when (response) {
                            is Resource.Success -> {
                                displayToast(
                                    "Successfully removed recommended movie",
                                    Toast.LENGTH_SHORT
                                )
                            }
                            is Resource.Error -> {
                                (activity as IHandleError).handleError(response.error, "Error removing recommendation")
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun initRecyclerView() {
        recyclerView = bindings.splitviewlayoutRecyclerview


        switchRecyclerViewLayoutManager(requireContext(), recyclerView, MediaEntryBaseAdapter.VIEW_TYPE_POSTER)

        adapter = ReccomendedMoviesAdaptor(tmdbPosterImageLoader,
            AdaptorActionControls(
                context?.getDrawable(R.drawable.ic_baseline_do_disturb_24),
                "Not Interested",
                false,
                R.menu.suggested_popup_menu,
                entrySelectedCallback = { selectedItem ->
                    navigateToMovie(selectedItem)
                },
                buttonClickedCallback = { selectedItem ->
                    getWarningAlertDialog(selectedItem)
                        .show()
                },
                menuItemSelectedCallback = {selectedItem, menuId ->
                    when(menuId) {
                        R.id.suggestedpopupmenu_dismiss_suggestion -> {
                            getWarningAlertDialog(selectedItem)
                                .show()
                        }
                    }
                }
            ))

        recyclerView.adapter = adapter

    }

    private fun getWarningAlertDialog(movie: Movie): AlertDialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Remove ${movie.title} from Suggestions?")
            .setMessage("Are you sure you want to remove ${movie.title} from your suggestions?")
            .setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->
                viewModel.removeRecommendedMovie(movie)
                dialogInterface.dismiss()
            })
            .setNegativeButton("No", DialogInterface.OnClickListener { dialogInterface, i -> dialogInterface.dismiss() })
            .create()
    }

    private fun getViewType() {
        lifecycleScope.launchWhenStarted {
            viewModel.viewType.collectLatest { viewType ->
                adapter.switchView(viewType)

                switchRecyclerViewLayoutManager(requireContext(), recyclerView, viewType)


                recyclerView.scrollToPosition(0)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.reccomendedfiltermenu_title -> {
                viewModel.applySorting(ISortable.SORT_BY_TITLE)
            }
            R.id.reccomendedfiltermenu_year -> {
                viewModel.applySorting(ISortable.SORT_BY_YEAR)
            }
            R.id.reccomendedfiltermenu_recommended_at -> {
                viewModel.applySorting(RecommendedMoviesViewModel.SORT_BY_LAST_SUGGESTED)
            }
            R.id.menu_switch_layout -> {
                lifecycleScope.launchWhenStarted {
                    viewModel.switchViewType()
                }
            }
            else -> {
                Log.e(TAG, "onOptionsItemSelected: Invalid menu item ${item.itemId}")
            }
        }
        return false
    }

    private fun navigateToMovie(movie: Movie?) {
        if (movie == null) {
            return
        }

        (activity as MoviesMainActivity).navigateToMovie(
            MovieDataModel(
                movie.ids?.trakt ?: 0,
                movie.ids?.tmdb,
                movie.title,
                movie.year,
            )
        )
    }

    private fun deleteSuggestion(position: Int, movie: Movie?) {
        if (movie == null) {
            return
        }

        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Delete suggestion ${movie.title}")
            .setMessage("Would you like to remove suggestion of ${movie.title}?")
            .setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->
                viewModel.removeRecommendedMovie(movie)

                dialogInterface.dismiss()
            })
            .setNegativeButton("No", DialogInterface.OnClickListener { dialogInterface, i ->
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

    private fun displayToast(message: String, length: Int) {
        Toast.makeText(requireContext(), message, length).show()
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            RecommendedMoviesFragment()
    }
}