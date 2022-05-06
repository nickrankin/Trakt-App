package com.nickrankin.traktapp.ui.shows.showdetails

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.nickrankin.traktapp.adapter.credits.ShowCastCreditsAdapter
import com.nickrankin.traktapp.dao.credits.ShowCastPerson
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.databinding.ShowDetailsOverviewFragmentBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.shows.showdetails.ShowDetailsOverviewViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "ShowDetailsOverviewFrag"

@AndroidEntryPoint
class ShowDetailsOverviewFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var bindings: ShowDetailsOverviewFragmentBinding
    private val viewModel: ShowDetailsOverviewViewModel by activityViewModels()

    private lateinit var castRecyclerView: RecyclerView
    private lateinit var showCastCreditsAdapter: ShowCastCreditsAdapter

    private lateinit var overview: String

    private var tmdbId = 0


    @Inject
    lateinit var glide: RequestManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = ShowDetailsOverviewFragmentBinding.inflate(inflater)
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initCastRecycler()
        setupCastSwitcher()

        tmdbId = arguments?.getInt(TMDB_ID_KEY, 0) ?: 0
        overview = arguments?.getString(OVERVIEW_KEY, "") ?: ""

        bindShowData()
    }

    private fun getCast() {

        lifecycleScope.launchWhenStarted {
            viewModel.cast.collectLatest { castResource ->
                when (castResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getCast: Loading Cast People")
                    }
                    is Resource.Success -> {
                        bindings.showdetailsoverviewMainGroup.visibility = View.VISIBLE
                        displayCast(castResource.data ?: emptyList())
                    }
                    is Resource.Error -> {
                        bindings.showdetailsoverviewMainGroup.visibility = View.GONE

                        Log.e(
                            TAG,
                            "getCast: Error getting Cast People. ${castResource.error?.localizedMessage}",
                        )
                        castResource.error?.printStackTrace()
                    }
                }
            }
        }
    }

    private fun setupCastSwitcher() {
        bindings.apply {
            val regularCastButton = showdetailsactivityCastRegularButton
            val guestStarsButton = showdetailsactivityCastGuestButton

            regularCastButton.visibility = View.VISIBLE
            guestStarsButton.visibility = View.VISIBLE

            regularCastButton.text = "Season Regulars"
            guestStarsButton.text = "Guest Stars"

            regularCastButton.setOnClickListener {
                regularCastButton.setTypeface(null, Typeface.BOLD)
                guestStarsButton.setTypeface(null, Typeface.NORMAL)

                viewModel.filterCast(false)
            }

            guestStarsButton.setOnClickListener {
                regularCastButton.setTypeface(null, Typeface.NORMAL)
                guestStarsButton.setTypeface(null, Typeface.BOLD)

                viewModel.filterCast(true)
            }


        }
    }

    private fun displayCast(castPersons: List<ShowCastPerson>) {

        if (castPersons.isNotEmpty()) {
            val castToggeButton = bindings.showdetailsactivityCrewToggle
            castToggeButton.visibility = View.VISIBLE

            showCastCreditsAdapter.submitList(castPersons)
        }
    }

    private fun initCastRecycler() {
        castRecyclerView = bindings.showdetailsoverviewCastRecycler
        castRecyclerView.visibility = View.VISIBLE

        val layoutManager = FlexboxLayoutManager(requireContext())
        layoutManager.flexDirection = FlexDirection.ROW
        layoutManager.flexWrap = FlexWrap.NOWRAP

        showCastCreditsAdapter = ShowCastCreditsAdapter(glide)

        castRecyclerView.layoutManager = layoutManager
        castRecyclerView.adapter = showCastCreditsAdapter
    }

    private fun bindShowData() {
        viewModel.setTmdbId(tmdbId)

        viewModel.onStart()
        getCast()
        bindings.showdetailsoverviewMainGroup.visibility = View.VISIBLE

        bindings.apply {
            showdetailsoverviewOverview.text = overview
        }
    }

    override fun onRefresh() {
        viewModel.setTmdbId(tmdbId)

        Log.d(TAG, "onRefresh: Refreshing Show Overview")
        viewModel.onRefresh()
    }

    companion object {
        const val OVERVIEW_KEY = "overview_key"
        const val TMDB_ID_KEY = "tmdb_id_key"

        fun newInstance() = ShowDetailsOverviewFragment()
    }
}