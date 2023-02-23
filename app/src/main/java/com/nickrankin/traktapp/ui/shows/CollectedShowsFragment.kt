package com.nickrankin.traktapp.ui.shows

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.AdaptorActionControls
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.adapter.shows.CollectedShowsAdapter
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.databinding.FragmentCollectedShowsBinding
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.auth.shows.CollectedShowsViewModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CollectedShowsFragment"

@AndroidEntryPoint
class CollectedShowsFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {
    private lateinit var bindings: FragmentCollectedShowsBinding
    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var messageContainer: TextView

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CollectedShowsAdapter

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader

    @Inject
    lateinit var glide: RequestManager

    private val viewModel by activityViewModels<CollectedShowsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = FragmentCollectedShowsBinding.inflate(inflater)
        swipeLayout = bindings.collectedshowsfragmentSwipeLayout
        messageContainer = bindings.collectedshowsfragmentMessageContainer

        swipeLayout.setOnRefreshListener(this)
        setHasOptionsMenu(true)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        progressBar = bindings.collectedshowsfragmentProgressbar

        updateTitle("Collected Shows")

        if (isLoggedIn) {
            lifecycleScope.launch {
                launch {
                    collectCollectedShows()
                }
            }
        } else {
            progressBar.visibility = View.GONE
            messageContainer.visibility = View.VISIBLE
            messageContainer.text = "You must login to see your shows."

            handleLoggedOutState()
        }
        initRecycler()
        getViewState()

        handleEvents()

    }

    private fun handleLoggedOutState() {
        progressBar.visibility = View.GONE
        messageContainer.visibility = View.VISIBLE
        swipeLayout.isEnabled = false

        val connectButton = bindings.collectedshowsfragmentTraktConnectButton
        connectButton.visibility = View.VISIBLE

        messageContainer.text = "You are not logged in. Please login to see your  Collected shows."

        connectButton.setOnClickListener {
            startActivity(Intent(activity, AuthActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.collected_filter_menu, menu)
        inflater.inflate(R.menu.layout_switcher_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)

    }

    private suspend fun collectCollectedShows() {
        viewModel.collectedShows.collectLatest { collectedShowsResource ->
            when (collectedShowsResource) {
                is Resource.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    Log.d(TAG, "collectCollectedShows: Loading collected shows ...")
                }
                is Resource.Success -> {
                    messageContainer.visibility = View.GONE
                    if (swipeLayout.isRefreshing) {
                        swipeLayout.isRefreshing = false
                    }
                    progressBar.visibility = View.GONE
                    Log.d(TAG, "collectCollectedShows: Got Collected Shows success")

                    val data = collectedShowsResource.data

                    if (data?.isNotEmpty() == true) {

                        adapter.submitList(data) {
                            recyclerView.scrollToPosition(0)
                        }

                    } else {
                        messageContainer.visibility = View.VISIBLE
                        messageContainer.text = "You have nothing in your collection :-("
                    }

                }
                is Resource.Error -> {
                    messageContainer.visibility = View.GONE
                    if (swipeLayout.isRefreshing) {
                        swipeLayout.isRefreshing = false
                    }
                    progressBar.visibility = View.GONE

                    if (collectedShowsResource.data != null) {
                        adapter.submitList(collectedShowsResource.data ?: emptyList())
                    }

                    (activity as IHandleError).showErrorSnackbarRetryButton(
                        collectedShowsResource.error,
                        bindings.collectedshowsfragmentSwipeLayout
                    ) {
                        viewModel.onRefresh()
                    }
                }
            }
        }
    }

    private fun handleEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->
                when (event) {
                    is CollectedShowsViewModel.Event.DELETE_COLLECTION_EVENT -> {
                        val syncResponse = event.syncResponse.data

                        if (event.syncResponse is Resource.Success) {
                            if (syncResponse?.deleted?.episodes ?: 0 > 0) {
                                displayMessageToast(
                                    "Successfully removed show from collection",
                                    Toast.LENGTH_LONG
                                )
                            }
                        } else if (event.syncResponse is Resource.Error) {
                            handleError(event.syncResponse.error, "Error removing show from collection, ")

                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun initRecycler() {
        recyclerView = bindings.collectedshowsfragmentRecyclerview

        switchRecyclerViewLayoutManager(requireContext(), recyclerView, MediaEntryBaseAdapter.VIEW_TYPE_POSTER)

        adapter = CollectedShowsAdapter(
            AdaptorActionControls(
            context?.getDrawable(R.drawable.ic_baseline_delete_forever_24), "Remove from Collection", false, R.menu.collected_popup_menu,
            entrySelectedCallback = {selectedItem ->
                navigateToShow(selectedItem)

            },
            buttonClickedCallback = { selectedShow ->
                removeFromCollection(selectedShow)

            },
            menuItemSelectedCallback = { selectedShow, menuItem ->
                when(menuItem) {
                    R.id.collectedpopupmenu_delete -> {
                        removeFromCollection(selectedShow)
                    }
                    else -> {
                        Log.e(TAG, "initRecycler: Invalid menu item $menuItem", )
                    }
                }
            }),
            sharedPreferences,
            glide,
            tmdbImageLoader)

        recyclerView.adapter = adapter
    }

    private fun navigateToShow(collectedShow: CollectedShow) {
        val intent = Intent(requireActivity(), ShowDetailsActivity::class.java)
        intent.putExtra(ShowDetailsActivity.SHOW_DATA_KEY,
            ShowDataModel(
                collectedShow.show_trakt_id, collectedShow.show_tmdb_id, collectedShow.show_title
            )
        )
        startActivity(intent)
    }

    private fun removeFromCollection(collectedShow: CollectedShow) {
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Delete ${collectedShow.show_title} from Collection?")
            .setMessage("Are you sure you want to delete ${collectedShow.show_title} from your Trakt Collection?")
            .setPositiveButton("Yes") { dialogInterface, i ->
                viewModel.deleteShowFromCollection(collectedShow)

                dialogInterface.dismiss()
            }
            .setNegativeButton("No", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            .create()

        alertDialog.show()

    }

    private fun displayMessageToast(message: String, duration: Int) {
        Toast.makeText(context, message, duration).show()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.collectedfiltermenu_title -> {
                viewModel.applySorting(ISortable.SORT_BY_TITLE)
            }
            R.id.collectedfiltermenu_year -> {
                viewModel.applySorting(ISortable.SORT_BY_YEAR)

            }
            R.id.collectedfiltermenu_collected_at -> {
                viewModel.applySorting(CollectedShowsViewModel.SORT_COLLECT_AT)

            }
            R.id.menu_switch_layout -> {
                lifecycleScope.launchWhenStarted {
                    viewModel.switchViewType()

                    recyclerView.scrollToPosition(0)
                }
            }
        }

        return false
    }

    private fun getViewState() {
        lifecycleScope.launchWhenStarted {
            viewModel.viewType.collectLatest { viewState ->
                adapter.switchView(viewState)

                switchRecyclerViewLayoutManager(requireContext(), recyclerView, viewState)

                recyclerView.scrollToPosition(0)

            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (isLoggedIn) {
            viewModel.onStart()
        }
    }

    override fun onRefresh() {
        super.onRefresh()

        if (isLoggedIn) {
            viewModel.onRefresh()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            CollectedShowsFragment()
    }
}