package com.nickrankin.traktapp.ui.shows

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.google.android.material.snackbar.Snackbar
import com.nickrankin.traktapp.MainActivity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.shows.CollectedShowsAdapter
import com.nickrankin.traktapp.dao.show.model.ShowProgress
import com.nickrankin.traktapp.databinding.FragmentCollectedShowsBinding
import com.nickrankin.traktapp.helper.PosterImageLoader
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.calculateProgress
import com.nickrankin.traktapp.model.auth.shows.CollectedShowsViewModel
import com.nickrankin.traktapp.repo.shows.ShowDetailsRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.enums.SortBy
import com.uwetrottmann.trakt5.enums.SortHow
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CollectedShowsFragment"
@AndroidEntryPoint
class CollectedShowsFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    private lateinit var bindings: FragmentCollectedShowsBinding
    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var messageContainer: TextView

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: CollectedShowsAdapter

    private var isLoggedIn = false

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var imageLoader: PosterImageLoader

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
        initRecycler()
        progressBar = bindings.collectedshowsfragmentProgressbar

        if(isLoggedIn) {
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
            when(collectedShowsResource) {
                is Resource.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    Log.d(TAG, "collectCollectedShows: Loading collected shows ...")
                }
                is Resource.Success -> {
                    messageContainer.visibility = View.GONE
                    if(swipeLayout.isRefreshing) {
                        swipeLayout.isRefreshing = false
                    }
                    progressBar.visibility = View.GONE
                    Log.d(TAG, "collectCollectedShows: Got Collected Shows success")

                    val data = collectedShowsResource.data

                    if(data?.isNotEmpty() == true) {
                        adapter.updateData(data)

                    } else {
                        messageContainer.visibility = View.VISIBLE
                        messageContainer.text = "You have nothing in your collection :-("
                    }

                }
                is Resource.Error -> {
                    messageContainer.visibility = View.GONE
                    if(swipeLayout.isRefreshing) {
                        swipeLayout.isRefreshing = false
                    }
                    progressBar.visibility = View.GONE

                    if(collectedShowsResource.data != null) {
                        adapter.updateData(collectedShowsResource.data ?: emptyList())
                    } else {
                        messageContainer.visibility = View.VISIBLE
                        messageContainer.text = "An error occurred while loading your Collected shows. ${collectedShowsResource.error?.localizedMessage} "
                    }
                    Log.e(TAG, "collectCollectedShows: Error collecting shows", )
                    collectedShowsResource.error?.printStackTrace()
                }
            }
        }
    }

    private fun initRecycler() {
        recyclerView = bindings.collectedshowsfragmentRecyclerview
        layoutManager = LinearLayoutManager(context)
        adapter = CollectedShowsAdapter(sharedPreferences, glide, imageLoader, callback = { show ->
            navigateToShow(show.show_trakt_id, show.show_tmdb_id, show.language)
        })

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
    }

    private fun navigateToShow(traktId: Int, tmdbId: Int, langauge: String) {
        val intent = Intent(context, ShowDetailsActivity::class.java)
        intent.putExtra(ShowDetailsRepository.SHOW_TRAKT_ID_KEY, traktId)
        intent.putExtra(ShowDetailsRepository.SHOW_TMDB_ID_KEY, tmdbId)
        intent.putExtra(ShowDetailsRepository.SHOW_LANGUAGE_KEY, langauge)

        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.collectedfiltermenu_title -> {
                viewModel.sortShows(SortBy.TITLE)
            }
            R.id.collectedfiltermenu_collected_at -> {
                viewModel.sortShows(SortBy.ADDED)

            }
        }

        return false
    }

    override fun onStart() {
        super.onStart()

        if(isLoggedIn) {
            viewModel.onStart()
        }
    }

    override fun onRefresh() {
        if(isLoggedIn) {
            viewModel.onRefresh()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            CollectedShowsFragment()
    }
}