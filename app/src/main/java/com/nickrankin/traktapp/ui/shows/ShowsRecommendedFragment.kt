package com.nickrankin.traktapp.ui.shows

import android.content.DialogInterface
import android.content.Intent
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
import com.nickrankin.traktapp.model.shows.RecommendedShowsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import com.nickrankin.traktapp.BaseFragment
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
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: RecommendedShowsAdapter
    private lateinit var recyclerView: RecyclerView

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader

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
                        selectedShow?.title)
                }
                RecommendedShowsAdapter.ACTION_REMOVE -> {
                    deleteRecommendation(selectedShow, pos)
                }
                else -> {
                    navigateToShow(
                        selectedShow?.ids?.trakt ?: 0,
                        selectedShow?.ids?.tmdb ?: 0,
                        selectedShow?.title)
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

    companion object {
        @JvmStatic
        fun newInstance() =
            ShowsRecommendedFragment()
    }
}