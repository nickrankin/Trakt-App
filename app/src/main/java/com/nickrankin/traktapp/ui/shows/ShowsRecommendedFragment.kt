package com.nickrankin.traktapp.ui.shows

import android.content.DialogInterface
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
import com.nickrankin.traktapp.model.shows.RecommendedShowsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.OnNavigateToEntity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.AdaptorActionControls
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.databinding.FragmentSplitviewLayoutBinding
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.enums.Type

private const val TAG = "ShowsRecommendedFragmen"

@AndroidEntryPoint
class ShowsRecommendedFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener, OnNavigateToShow {

    private var _bindings: FragmentSplitviewLayoutBinding? = null
    private val bindings get() = _bindings!!

    private val viewModel: RecommendedShowsViewModel by activityViewModels()

    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: RecommendedShowsAdapter
    private lateinit var recyclerView: RecyclerView

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _bindings = FragmentSplitviewLayoutBinding.inflate(inflater)
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        (activity as OnNavigateToEntity).enableOverviewLayout(false)


        progressBar = bindings.splitviewlayoutProgressbar

        val isLoggedIn = sharedPreferences.getBoolean(AuthActivity.IS_LOGGED_IN, false)

        updateTitle("Suggested Shows")

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
        recyclerView = bindings.splitviewlayoutRecyclerview

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
                        Log.e(TAG, "initRecycler: Invalid menu item $menuItem")
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
            viewModel.suggestedShows.collectLatest { suggestedShowsResource ->
                when (suggestedShowsResource) {
                    is Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        toggleMessageBanner(bindings, null, false)
                    }
                    is Resource.Success -> {

                        progressBar.visibility = View.GONE

                        val shows = suggestedShowsResource.data

                        if(shows.isNullOrEmpty()) {
                            toggleMessageBanner(bindings, getString(R.string.no_recommended_shows), true)
                        } else {
                            toggleMessageBanner(bindings, null, false)

                            // Create a new list to allow ListAdapters AsyncListDiffer checks to run https://stackoverflow.com/questions/69715381/diffutil-not-refreshing-view-in-observer-call-android-kotlin
                            adapter.submitList(shows.toMutableList()) {
                                recyclerView.scrollToPosition(0)
                            }
                        }
                    }
                    is Resource.Error -> {

                        progressBar.visibility = View.GONE

                        val shows = suggestedShowsResource.data

                        if(shows.isNullOrEmpty()) {
                            toggleMessageBanner(bindings, getString(R.string.no_recommended_shows), true)
                        } else {
                            toggleMessageBanner(bindings, null, false)

                            // Create a new list to allow ListAdapters AsyncListDiffer checks to run https://stackoverflow.com/questions/69715381/diffutil-not-refreshing-view-in-observer-call-android-kotlin
                            adapter.submitList(shows.toMutableList()) {
                                recyclerView.scrollToPosition(0)
                            }
                        }

                        showErrorSnackbarRetryButton(
                            suggestedShowsResource.error, bindings.root
                        ) {
                            onRefresh()
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

                            (activity as IHandleError).handleError(syncResponseResource.error, "Error adding show to favourites")
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
                                handleError(event.removedSuccessfully.error, "Failed to remove suggestion. Error: ")
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

        (activity as OnNavigateToEntity).navigateToShow(
            ShowDataModel(
                traktId, tmdbId, title
            )
        )
    }


    private fun displayMessageToast(message: String, length: Int) {
        Toast.makeText(requireContext(), message, length).show()
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
                Log.e(TAG, "onOptionsItemSelected: Invalid menu option ${item.itemId}")
            }
        }
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _bindings = null
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            ShowsRecommendedFragment()
    }
}