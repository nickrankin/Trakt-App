package com.nickrankin.traktapp.ui.shows.showdetails

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.shows.showdetails.ShowDetailsOverviewViewModel
import com.nickrankin.traktapp.ui.person.PersonActivity
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

        getShow()
        getCast()

        bindings.showdetailsoverviewMainGroup.visibility = View.VISIBLE
    }

    private fun getShow() {
        lifecycleScope.launchWhenStarted {
            viewModel.show.collectLatest { show ->
                bindings.showdetailsoverviewOverview.text = show?.overview
            }
        }
    }

    private fun getCast() {
        viewModel.filterCast(false)
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

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    private fun displayCast(castPersons: List<ShowCastPerson>) {
        val castToggeButton = bindings.showdetailsactivityCrewToggle

        // Some shows do not have Guest stars, this check is needed to prevent toggle dissapearing if guest stars are empty
        if (castPersons.isNotEmpty()) {
            castToggeButton.visibility = View.VISIBLE

            showCastCreditsAdapter.submitList(castPersons)
        } else {
            castToggeButton.visibility = View.GONE
        }
    }

    private fun initCastRecycler() {
        castRecyclerView = bindings.showdetailsoverviewCastRecycler
        castRecyclerView.visibility = View.VISIBLE

        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL

        showCastCreditsAdapter = ShowCastCreditsAdapter(glide) { selectedCastPerson ->
            val castPersonIntent = Intent(requireContext(), PersonActivity::class.java)
            castPersonIntent.putExtra(PersonActivity.PERSON_ID_KEY, selectedCastPerson.person.trakt_id)

            startActivity(castPersonIntent)
        }

        castRecyclerView.layoutManager = layoutManager
        castRecyclerView.adapter = showCastCreditsAdapter
    }



    override fun onRefresh() {
        Log.d(TAG, "onRefresh: Refreshing Overview")
        viewModel.onRefresh()
    }

    companion object {
        const val OVERVIEW_KEY = "overview_key"
        const val TMDB_ID_KEY = "tmdb_id_key"

        fun newInstance() = ShowDetailsOverviewFragment()
    }
}