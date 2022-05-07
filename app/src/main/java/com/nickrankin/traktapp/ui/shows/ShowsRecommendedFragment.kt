package com.nickrankin.traktapp.ui.shows

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.adapter.shows.RecommendedShowsAdapter
import com.nickrankin.traktapp.databinding.FragmentShowsRecommendedBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.shows.RecommendedShowsViewModel
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import com.uwetrottmann.trakt5.entities.Show

private const val TAG = "ShowsRecommendedFragmen"

@AndroidEntryPoint
class ShowsRecommendedFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener, OnNavigateToShow {
    private lateinit var bindings: FragmentShowsRecommendedBinding

    private val viewModel: RecommendedShowsViewModel by activityViewModels()

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: RecommendedShowsAdapter
    private lateinit var recyclerView: RecyclerView

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader

    @Inject
    lateinit var sharedPreferences: SharedPreferences


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        getSuggestedShows()
        getEvents()
    }

    private fun initRecycler() {
        recyclerView = bindings.fragmentreccomendedshowsRecyclerview

        layoutManager = LinearLayoutManager(requireContext())

        adapter = RecommendedShowsAdapter(tmdbImageLoader, callback = { selectedShow, action, pos ->

            when(action) {
                RecommendedShowsAdapter.ACTION_VIEW -> {
                    navigateToShow(
                        selectedShow?.ids?.trakt ?: 0,
                        selectedShow?.ids?.tmdb ?: 0,
                        selectedShow?.title,
                        selectedShow?.language
                    )
                }
                RecommendedShowsAdapter.ACTION_REMOVE -> {
                    deleteRecommendation(selectedShow, pos)
                }
                else -> {
                    navigateToShow(
                        selectedShow?.ids?.trakt ?: 0,
                        selectedShow?.ids?.tmdb ?: 0,
                        selectedShow?.title,
                        selectedShow?.language
                    )
                }
            }
        })

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
    }

    private fun deleteRecommendation(show: Show?, position: Int) {
        if(show == null) {
            return
        }

        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Remove recommendation ${show.title}?")
            .setMessage("Are you sure you want to remove recommendation ${show.title}?")
            .setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->
                viewModel.removeFromSuggestions(show.ids?.trakt?.toString() ?: "")

                val listCopy: MutableList<Show> = mutableListOf()
                listCopy.addAll(adapter.currentList)
                listCopy.removeAt(position)
                adapter.submitList(listCopy)

                dialogInterface.dismiss()
            })
            .setNegativeButton("No", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            .create()

        alertDialog.show()
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
                        adapter.submitList(data.data)
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
                    }
                }
            }
        }
    }

    private fun getEvents() {
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

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {
        viewModel.onRefresh()
    }

    override fun navigateToShow(traktId: Int, tmdbId: Int, showTitle: String?, language: String?) {
        if (tmdbId == 0) {
            Toast.makeText(context, "Trakt does not have this show's TMDB", Toast.LENGTH_LONG)
                .show()
            return
        }

        val intent = Intent(context, ShowDetailsActivity::class.java)
        intent.putExtra(ShowDetailsRepository.SHOW_TRAKT_ID_KEY, traktId)

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

    companion object {
        @JvmStatic
        fun newInstance() =
            ShowsRecommendedFragment()
    }
}