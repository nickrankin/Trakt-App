package com.nickrankin.traktapp.ui.shows

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
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.adapter.shows.RecommendedShowsAdapter
import com.nickrankin.traktapp.databinding.FragmentShowsRecommendedBinding
import com.nickrankin.traktapp.model.shows.RecommendedShowsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.AdaptorActionControls
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.enums.Type

private const val TAG = "ShowsRecommendedFragmen"

@AndroidEntryPoint
class ShowsRecommendedFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener, OnNavigateToShow {
    private lateinit var bindings: FragmentShowsRecommendedBinding

    private val viewModel: RecommendedShowsViewModel by activityViewModels()

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: RecommendedShowsAdapter
    private lateinit var recyclerView: RecyclerView

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        swipeRefreshLayout = bindings.fragmentreccomendedshowsSwipeLayout
        swipeRefreshLayout.setOnRefreshListener(this)

        progressBar = bindings.fragmentreccomendedshowsProgressbar

        val isLoggedIn = sharedPreferences.getBoolean(AuthActivity.IS_LOGGED_IN, false)

        updateTitle("Suggested Shows")

        if(!isLoggedIn) {
            // TODO display relevant message to UI
            Log.e(TAG, "onViewCreated: Need login for this action", )
            return
        }

        initRecycler()
        getViewType()

        getSuggestedShows()
        getEvents()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.recommended_filter_menu, menu)

        inflater.inflate(R.menu.layout_switcher_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)

    }

    private fun initRecycler() {
        recyclerView = bindings.fragmentreccomendedshowsRecyclerview

        switchRecyclerViewLayoutManager(requireContext(), recyclerView, MediaEntryBaseAdapter.VIEW_TYPE_POSTER)

        adapter = RecommendedShowsAdapter(tmdbImageLoader,
            AdaptorActionControls(
            context?.getDrawable(R.drawable.ic_baseline_do_disturb_24), "Not Interested", false, R.menu.suggested_popup_menu,
            entrySelectedCallback = {selectedItem ->
                navigateToShow(
                    selectedItem.ids?.trakt ?: 0,
                    selectedItem.ids?.tmdb ?: 0,
                    selectedItem.title)
            },
            buttonClickedCallback = { selectedShow -> 
                deleteRecommendation(selectedShow)
            },
            menuItemSelectedCallback = { selectedShow, menuItem ->
                when(menuItem) {
                    R.id.suggestedpopupmenu_dismiss_suggestion -> {
                        deleteRecommendation(selectedShow)
                    }
                    else -> {
                        Log.e(TAG, "initRecycler: Invalid menu item $menuItem", )
                    }
                }
            }
        ))

        recyclerView.adapter = adapter
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

    private fun getSuggestedShows() {
        lifecycleScope.launchWhenStarted {
            viewModel.suggestedShows.collectLatest { data ->
                when (data) {
                    is Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        if(swipeRefreshLayout.isRefreshing) {
                            swipeRefreshLayout.isRefreshing = false
                        }

                        progressBar.visibility = View.GONE

                        // Create a new list to allow ListAdapters AsyncListDiffer checks to run https://stackoverflow.com/questions/69715381/diffutil-not-refreshing-view-in-observer-call-android-kotlin
                        adapter.submitList(data.data?.toMutableList()) {
                            recyclerView.scrollToPosition(0)
                        }
                    }
                    is Resource.Error -> {
                        if(swipeRefreshLayout.isRefreshing) {
                            swipeRefreshLayout.isRefreshing = false
                        }

                        progressBar.visibility = View.GONE
                        Toast.makeText(
                            requireContext(),
                            "Error loading recommended shows from Trakt. ${data.error?.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()

                        (activity as IHandleError).showErrorSnackbarRetryButton(
                            data.error, bindings.fragmentreccomendedshowsSwipeLayout
                        ) {
                            viewModel.onRefresh()
                        }
                    }
                }
            }
        }
    }

    private fun deleteRecommendation(show: Show?) {
        if(show == null) {
            return
        }

        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Remove recommendation ${show.title}?")
            .setMessage("Are you sure you want to remove recommendation ${show.title}?")
            .setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->
                viewModel.removeFromSuggestions(show)


                dialogInterface.dismiss()
            })
            .setNegativeButton("No", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            .create()

        alertDialog.show()
    }


    private fun getEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->
                when(event) {
                    is RecommendedShowsViewModel.Event.AddToCollectionEvent -> {
                        val syncResponseResource = event.syncResponse

                        if(syncResponseResource is Resource.Success) {

                            when(getSyncResponse(syncResponseResource.data, Type.SHOW)) {
                                Response.ADDED_OK -> {

                                }
                                Response.NOT_FOUND -> {
                                    displayMessageToast("Failed to add show to your collection.", Toast.LENGTH_LONG)
                                }
                                Response.ERROR -> {
                                    displayMessageToast("Failed to add show to your collection.", Toast.LENGTH_LONG)
                                }
                                else -> {}
                            }
                        } else if (syncResponseResource is Resource.Error) {

                            (activity as IHandleError).showErrorMessageToast(syncResponseResource.error, "Error adding show to favourites")
                        }

                    }
                    is RecommendedShowsViewModel.Event.RemoveSuggestionEvent -> {
                        when(event.removedSuccessfully) {
                            is Resource.Loading -> {
                                // Unused
                            }
                            is Resource.Success -> {
                                // Unused
                            }
                            is Resource.Error -> {
                                event.removedSuccessfully.error?.printStackTrace()
                                displayMessageToast("Failed to remove suggestion. Error: ${event.removedSuccessfully.error?.localizedMessage }", Toast.LENGTH_LONG)
                            }

                            }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {
        viewModel.onRefresh()
    }

    override fun navigateToShow(traktId: Int, tmdbId: Int?, title: String?) {

        val intent = Intent(context, ShowDetailsActivity::class.java)
        intent.putExtra(ShowDetailsActivity.SHOW_DATA_KEY,
            ShowDataModel(
                traktId, tmdbId, title
            )
        )

        startActivity(intent)
    }


    private fun displayMessageToast(message: String, length: Int) {
        Toast.makeText(requireContext(), message, length).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        bindings = FragmentShowsRecommendedBinding.inflate(inflater)
        return bindings.root
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
                viewModel.applySorting(RecommendedShowsViewModel.RECOMMENDED_AT_SORT_BY)
            }
            R.id.menu_switch_layout -> {
                lifecycleScope.launchWhenStarted {
                    viewModel.switchViewType()
                }
            }
            else -> {
                Log.e(TAG, "onOptionsItemSelected: Invalid menu option ${item.itemId}", )
            }
        }
        return false
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            ShowsRecommendedFragment()
    }
}