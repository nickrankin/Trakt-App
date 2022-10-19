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
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.AdaptorActionControls
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.adapter.movies.ReccomendedMoviesAdaptor
import com.nickrankin.traktapp.databinding.FragmentRecommendedMoviesBinding
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.movies.RecommendedMoviesViewModel
import com.nickrankin.traktapp.ui.movies.moviedetails.MovieDetailsActivity
import com.uwetrottmann.trakt5.entities.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "RecommendedMoviesFragme"

@AndroidEntryPoint
class RecommendedMoviesFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var bindings: FragmentRecommendedMoviesBinding

    private lateinit var swipeLayout: SwipeRefreshLayout
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
        bindings = FragmentRecommendedMoviesBinding.inflate(inflater)
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        swipeLayout = bindings.recommendedmoviesfragmentSwipeLayout
        swipeLayout.setOnRefreshListener(this)

        progressBar = bindings.recommendedmoviesfragmentProgressbar

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
        inflater.inflate(R.menu.collected_filter_menu, menu)
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

                        if (swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }
                        Log.d(TAG, "getRecommendedMovies: Got recommendations successfully")

                        if(recommendedMoviesResource.data != null && recommendedMoviesResource.data!!.isNotEmpty()) {
                            bindings.recommendedmoviesfragmentRecyclerview.visibility = View.VISIBLE
                            bindings.recommendedmoviesfragmentMessageContainer.visibility = View.GONE

                            if(recommendedMoviesResource.data != null) {
                                adapter.submitList(recommendedMoviesResource.data!!.toMutableList())
                            }

                        } else {
                            bindings.recommendedmoviesfragmentRecyclerview.visibility = View.GONE
                            bindings.recommendedmoviesfragmentMessageContainer.visibility = View.VISIBLE

                            bindings.recommendedmoviesfragmentMessageContainer.text = "There are no recommended movies at this time!"
                        }
                    }

                    is Resource.Error -> {
                        progressBar.visibility = View.GONE

                        if (swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }

                        if(recommendedMoviesResource.data != null && recommendedMoviesResource.data!!.isNotEmpty()) {
                            bindings.recommendedmoviesfragmentRecyclerview.visibility = View.VISIBLE
                            bindings.recommendedmoviesfragmentMessageContainer.visibility = View.GONE

                            adapter.submitList(recommendedMoviesResource.data)
                        } else {
                            bindings.recommendedmoviesfragmentRecyclerview.visibility = View.GONE
                            bindings.recommendedmoviesfragmentMessageContainer.visibility = View.VISIBLE

                            bindings.recommendedmoviesfragmentMessageContainer.text = "There are no recommended movies at this time!"
                        }

                        (activity as IHandleError).showErrorSnackbarRetryButton(
                            recommendedMoviesResource.error,
                            bindings.recommendedmoviesfragmentSwipeLayout
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
                                (activity as IHandleError).showErrorMessageToast(response.error, "Error removing recommendation")
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun initRecyclerView() {
        recyclerView = bindings.recommendedmoviesfragmentRecyclerview


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
            R.id.collectedfiltermenu_switch_layout -> {
                lifecycleScope.launchWhenStarted {
                    viewModel.switchViewType()
                }
            }
            else -> {
                Log.e(TAG, "onOptionsItemSelected: Invalid menu item ${item.itemId}", )
            }
        }
        return false
    }

    private fun navigateToMovie(movie: Movie?) {
        if (movie == null) {
            return
        }

        val intent = Intent(requireContext(), MovieDetailsActivity::class.java)
        intent.putExtra(
            MovieDetailsActivity.MOVIE_DATA_KEY,
            MovieDataModel(
                movie.ids?.trakt ?: 0,
                movie.ids?.tmdb,
                movie.title,
                movie.year,
            )
        )
        startActivity(intent)
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