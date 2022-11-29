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
import androidx.work.WorkInfo
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.adapter.credits.ShowCastCreditsAdapter
import com.nickrankin.traktapp.dao.credits.ShowCastPerson
import com.nickrankin.traktapp.databinding.ShowDetailsOverviewFragmentBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.shows.ShowDetailsViewModel
import com.nickrankin.traktapp.ui.IOnStatistcsRefreshListener
import com.nickrankin.traktapp.ui.person.PersonActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "ShowDetailsOverviewFrag"

@AndroidEntryPoint
class ShowDetailsOverviewFragment : Fragment() {

    private lateinit var bindings: ShowDetailsOverviewFragmentBinding
    private val viewModel: ShowDetailsViewModel by activityViewModels()

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
    }

    private fun getShow() {
        lifecycleScope.launchWhenStarted {
            viewModel.show.collectLatest { showResource ->

                when(showResource){
                    is Resource.Loading -> {

                    }
                    is Resource.Success -> {
                     val show = showResource.data

                    if(show != null) {
                        bindings.showdetailsoverviewOverview.text = show.overview
                    }
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "getShow: Error getting show ${showResource.error?.message}", )
                    }

                }

            }
        }
    }

    private fun getCast() {
        viewModel.filterCast(false)
        lifecycleScope.launchWhenStarted {
            viewModel.cast.collectLatest { castMembersResource ->
                when(castMembersResource) {
                    is Resource.Loading -> {

                    }
                    is Resource.Success -> {
                        displayCast(castMembersResource.data ?: emptyList())

                    }
                    is Resource.Error -> {
                        Log.e(TAG, "getCast: Error getting cast ${castMembersResource.error?.message}", )
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

    private fun displayCast(castPersons: List<ShowCastPerson?>) {
        val castToggeButton = bindings.showdetailsactivityCrewToggle
        showCastCreditsAdapter.submitList(castPersons)
    }

    private fun initCastRecycler() {
        castRecyclerView = bindings.showdetailsoverviewCastRecycler
        castRecyclerView.visibility = View.VISIBLE

        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL

        showCastCreditsAdapter = ShowCastCreditsAdapter(glide) { selectedCastPerson ->
            val castPersonIntent = Intent(requireContext(), PersonActivity::class.java)
            castPersonIntent.putExtra(PersonActivity.PERSON_ID_KEY, selectedCastPerson.person_trakt_id)

            startActivity(castPersonIntent)
        }

        castRecyclerView.layoutManager = layoutManager
        castRecyclerView.adapter = showCastCreditsAdapter
    }

    companion object {
        const val OVERVIEW_KEY = "overview_key"
        const val TMDB_ID_KEY = "tmdb_id_key"

        fun newInstance() = ShowDetailsOverviewFragment()
    }
}