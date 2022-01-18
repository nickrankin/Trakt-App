package com.nickrankin.traktapp.ui.shows

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.shows.RecommendedShowsAdapter
import com.nickrankin.traktapp.databinding.FragmentShowsRecommendedBinding
import com.nickrankin.traktapp.helper.ItemDecorator
import com.nickrankin.traktapp.helper.PosterImageLoader
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.shows.RecommendedShowsViewModel
import com.nickrankin.traktapp.repo.shows.ShowDetailsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import com.google.android.material.snackbar.Snackbar
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.entities.ShowIds
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncShow

private const val TAG = "ShowsRecommendedFragmen"

@AndroidEntryPoint
class ShowsRecommendedFragment : Fragment(), OnNavigateToShow {
    private lateinit var bindings: FragmentShowsRecommendedBinding

    private val viewModel: RecommendedShowsViewModel by activityViewModels()

    private lateinit var progressBar: ProgressBar
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: RecommendedShowsAdapter
    private lateinit var recyclerView: RecyclerView

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var imageLoader: PosterImageLoader

    @Inject
    lateinit var sharedPreferences: SharedPreferences


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = bindings.fragmentreccomendedshowsProgressbar

        val isLoggedIn = sharedPreferences.getBoolean(AuthActivity.IS_LOGGED_IN, false)

        if(!isLoggedIn) {
            // TODO display relevant message to UI
            Log.e(TAG, "onViewCreated: Need login for this action", )
            return
        }

        initRecycler()

        collectShows()
        collectEvents()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onInit()
    }

    private fun initRecycler() {
        recyclerView = bindings.fragmentreccomendedshowsRecyclerview

        layoutManager = LinearLayoutManager(requireContext())

        adapter = RecommendedShowsAdapter(glide, imageLoader, callback = { selectedShow ->
            navigateToShow(
                selectedShow?.ids?.trakt ?: 0,
                selectedShow?.ids?.tmdb ?: 0,
                selectedShow?.language
            )
        })

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        setupViewSwipeBehaviour()
    }

    private fun collectShows() {
        lifecycleScope.launchWhenStarted {
            viewModel.suggestedShows.collectLatest { data ->
                when (data) {
                    is Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        progressBar.visibility = View.GONE
                        adapter.submitList(data.data)
                    }
                    is Resource.Error -> {
                        progressBar.visibility = View.GONE
                        Toast.makeText(
                            requireContext(),
                            "Error loading recommended shows from Trakt. ${data.error?.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun collectEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->
                when(event) {
                    is RecommendedShowsViewModel.Event.AddToCollectionEvent -> {
                        val syncResponse = event.syncResponse

                        if(syncResponse is Resource.Success) {
                            if(syncResponse.data?.added?.episodes ?: 0 > 0) {
                                Log.e(TAG, "collectEvents: Added Collected show success", )
                            } else {
                                displayMessageToast("Failed to add show to your collection.", Toast.LENGTH_LONG)
                            }
                        } else if (syncResponse is Resource.Error) {
                            syncResponse.error?.printStackTrace()

                            displayMessageToast("Error adding show to favourites", Toast.LENGTH_LONG)
                        }

                    }
                    is RecommendedShowsViewModel.Event.RemoveSuggestionEvent -> {
                        if(!event.removedSuccessfully) {
                            event.t?.printStackTrace()
                            displayMessageToast("Failed to remove suggestion. Error: ${event.t?.localizedMessage }", Toast.LENGTH_LONG)
                        }
                    }
                }
            }
        }
    }

    override fun navigateToShow(traktId: Int, tmdbId: Int, language: String?) {
        if (tmdbId == 0) {
            Toast.makeText(context, "Trakt does not have this show's TMDB", Toast.LENGTH_LONG)
                .show()
            return
        }

        val intent = Intent(context, ShowDetailsActivity::class.java)
        intent.putExtra(ShowDetailsRepository.SHOW_TRAKT_ID_KEY, traktId)
        intent.putExtra(ShowDetailsRepository.SHOW_TMDB_ID_KEY, tmdbId)
        intent.putExtra(ShowDetailsRepository.SHOW_LANGUAGE_KEY, language)

        startActivity(intent)
    }

    private fun setupViewSwipeBehaviour() {

        var itemTouchHelper: ItemTouchHelper? = null

        itemTouchHelper = ItemTouchHelper(
            object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
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
                        textFromStartToEnd = "Add to Collection",
                        textFromEndToStart = "Remove from Suggested",
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
                    val showsList: MutableList<Show> = mutableListOf()
                    showsList.addAll(adapter.currentList)

                    val showPosition = viewHolder.layoutPosition
                    val show = showsList[showPosition]

                    when (direction) {
                        ItemTouchHelper.LEFT -> {
                            val updatedList: MutableList<Show> = mutableListOf()
                            updatedList.addAll(showsList)
                            updatedList.remove(show)

                            adapter.submitList(updatedList)

                            val timer = getTimer() {
                                Log.e(TAG, "onFinish: Timer ended for remove show ${show.title}!")
                                viewModel.removeFromSuggestions(show.ids?.trakt?.toString() ?: "")

                            }.start()

                            getSnackbar(
                                bindings.fragmentreccomendedshowsRecyclerview,
                                "You have removed suggestion: ${show.title}"
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

                        ItemTouchHelper.RIGHT -> {
                            val timer = getTimer() {
                                Log.e(TAG, "onFinish: Timer ended for Collect Show ${show.title}!")
                                val syncItems = SyncItems()
                                syncItems.apply {
                                    shows = listOf(
                                        SyncShow()
                                            .id(ShowIds.trakt(show.ids?.trakt ?: 0))
                                    )
                                }
                                viewModel.addToCollection(syncItems)
                            }.start()

                            getSnackbar(
                                bindings.fragmentreccomendedshowsRecyclerview,
                                "You have added ${show.title} to your collection."
                            ) {
                                timer.cancel()
                            }.show()

                            // Force the current show to "bounce back" into view
                            adapter.notifyItemChanged(showPosition)
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

    companion object {
        @JvmStatic
        fun newInstance() =
            ShowsRecommendedFragment()
    }
}