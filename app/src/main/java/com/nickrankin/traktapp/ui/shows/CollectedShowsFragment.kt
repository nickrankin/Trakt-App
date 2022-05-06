package com.nickrankin.traktapp.ui.shows

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.google.android.material.snackbar.Snackbar
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.shows.CollectedShowsAdapter
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.databinding.FragmentCollectedShowsBinding
import com.nickrankin.traktapp.helper.ItemDecorator
import com.nickrankin.traktapp.helper.OnTitleChangeListener
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.model.auth.shows.CollectedShowsViewModel
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import com.uwetrottmann.trakt5.enums.SortBy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CollectedShowsFragment"

@AndroidEntryPoint
class CollectedShowsFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener, OnNavigateToShow {
    private lateinit var bindings: FragmentCollectedShowsBinding
    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var messageContainer: TextView

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: CollectedShowsAdapter

    private var scrollToTop = true

    private var isLoggedIn = false

    @Inject
    lateinit var sharedPreferences: SharedPreferences

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

        isLoggedIn = sharedPreferences.getBoolean(AuthActivity.IS_LOGGED_IN, false)

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
                            if (scrollToTop) {
                                recyclerView.scrollToPosition(0)

                                scrollToTop = false
                            }
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
                    } else {
                        messageContainer.visibility = View.VISIBLE
                        messageContainer.text =
                            "An error occurred while loading your Collected shows. ${collectedShowsResource.error?.localizedMessage} "
                    }
                    Log.e(TAG, "collectCollectedShows: Error collecting shows")
                    collectedShowsResource.error?.printStackTrace()
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
                            displayMessageToast(
                                "Error removing show from collection, ${event.syncResponse.error?.localizedMessage}",
                                Toast.LENGTH_LONG
                            )
                        }
                    }
                }
            }
        }
    }

    private fun initRecycler() {
        recyclerView = bindings.collectedshowsfragmentRecyclerview
        setupViewSwipeBehaviour()

        layoutManager = LinearLayoutManager(context)
        adapter = CollectedShowsAdapter(
            sharedPreferences,
            glide,
            tmdbImageLoader,
            callback = { show, action ->

                when (action) {
                    CollectedShowsAdapter.ACTION_NAVIGATE_SHOW -> {
                        navigateToShow(show.show_trakt_id, show.show_tmdb_id, show.show_title, show.language)
                    }
                    CollectedShowsAdapter.ACTION_REMOVE_COLLECTION -> {
                        removeFromCollection(show)
                    }
                }

            })

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
    }


    override fun navigateToShow(traktId: Int, tmdbId: Int, showTitle: String?, language: String?) {
        val intent = Intent(context, ShowDetailsActivity::class.java)
        intent.putExtra(ShowDetailsRepository.SHOW_TRAKT_ID_KEY, traktId)

        startActivity(intent)
    }

    private fun removeFromCollection(collectedShow: CollectedShow) {
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Delete ${collectedShow.show_title} from Collection?")
            .setMessage("Are you sure you want to delete ${collectedShow.show_title} from your Trakt Collection?")
            .setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->
                viewModel.deleteShowFromCollection(collectedShow)
            })
            .setNegativeButton("No", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            .create()

        alertDialog.show()

    }

    private fun setupViewSwipeBehaviour() {

        var itemTouchHelper: ItemTouchHelper? = null

        itemTouchHelper = ItemTouchHelper(
            object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    viewHolder.itemView.background = null

                    return true
                }

                override fun onChildDraw(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    val colorAlert = ContextCompat.getColor(requireContext(), R.color.red)
                    val teal200 = ContextCompat.getColor(requireContext(), R.color.teal_200)
                    val defaultWhiteColor = ContextCompat.getColor(requireContext(), R.color.white)

                    ItemDecorator.Builder(c, recyclerView, viewHolder, dX, actionState).set(
                        iconHorizontalMargin = 23f,
                        backgroundColorFromStartToEnd = teal200,
                        backgroundColorFromEndToStart = colorAlert,
                        textFromStartToEnd = "",
                        textFromEndToStart = "Remove from Collection",
                        textColorFromStartToEnd = defaultWhiteColor,
                        textColorFromEndToStart = defaultWhiteColor,
                        iconTintColorFromStartToEnd = defaultWhiteColor,
                        iconTintColorFromEndToStart = defaultWhiteColor,
                        textSizeFromStartToEnd = 16f,
                        textSizeFromEndToStart = 16f,
                        typeFaceFromStartToEnd = Typeface.DEFAULT_BOLD,
                        typeFaceFromEndToStart = Typeface.SANS_SERIF,
                        iconResIdFromStartToEnd = R.drawable.ic_baseline_delete_forever_24,
                        iconResIdFromEndToStart = R.drawable.ic_trakt_svgrepo_com
                    )

                    super.onChildDraw(
                        c,
                        recyclerView,
                        viewHolder,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )

                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val showsList: MutableList<CollectedShow> = mutableListOf()
                    showsList.addAll(adapter.currentList)

                    val showPosition = viewHolder.layoutPosition
                    val show = showsList[showPosition]

                    when (direction) {
                        ItemTouchHelper.LEFT -> {
                            val updatedList: MutableList<CollectedShow> = mutableListOf()
                            updatedList.addAll(showsList)
                            updatedList.remove(show)

                            adapter.submitList(updatedList)

                            val timer = getTimer() {
                                Log.e(TAG, "onFinish: Timer ended for remove show ${show.show_title}!")
                                viewModel.deleteShowFromCollection(show)

                            }.start()

                            getSnackbar(
                                bindings.collectedshowsfragmentRecyclerview,
                                "You have removed collected Show: ${show.show_title}"
                            ) {
                                timer.cancel()
                                adapter.submitList(showsList) {
                                    // For first and last element, always scroll to the position to bring the element to focus
                                    if (showPosition == 0) {
                                        recyclerView.scrollToPosition(0)
                                    } else if (showPosition == showsList.size - 1) {
                                        recyclerView.scrollToPosition(showsList.size - 1)
                                    }
                                }
                            }.show()
                        }
                    }
                }
            }
        )

        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun getTimer(doAction: () -> Unit): CountDownTimer {
        return object : CountDownTimer(5000, 1000) {
            override fun onTick(p0: Long) {
            }

            override fun onFinish() {
                doAction()
            }
        }
    }

    private fun getSnackbar(v: View, message: String, listener: View.OnClickListener): Snackbar {
        return Snackbar.make(
            v,
            message,
            Snackbar.LENGTH_LONG
        )
            .setAction("Cancel", listener)
    }

    private fun displayMessageToast(message: String, duration: Int) {
        Toast.makeText(context, message, duration).show()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.collectedfiltermenu_title -> {
                scrollToTop = true
                viewModel.sortShows(SortBy.TITLE)
            }
            R.id.collectedfiltermenu_collected_at -> {
                scrollToTop = true
                viewModel.sortShows(SortBy.ADDED)

            }
        }

        return false
    }

    override fun onResume() {
        super.onResume()

        if (isLoggedIn) {
            viewModel.onStart()
        }
    }

    override fun onRefresh() {
        if (isLoggedIn) {
            scrollToTop = true
            viewModel.onRefresh()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            CollectedShowsFragment()
    }
}