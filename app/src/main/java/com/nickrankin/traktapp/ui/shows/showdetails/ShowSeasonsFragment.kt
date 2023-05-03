package com.nickrankin.traktapp.ui.shows.showdetails

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.OnNavigateToEntity
import com.nickrankin.traktapp.adapter.shows.SeasonsAdapter
import com.nickrankin.traktapp.dao.show.TmSeasonAndStats
import com.nickrankin.traktapp.databinding.FragmentShowSeasonsBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.SeasonDataModel
import com.nickrankin.traktapp.model.shows.ShowDetailsViewModel
import com.nickrankin.traktapp.ui.shows.SeasonEpisodesFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "ShowSeasonsFragment"
@AndroidEntryPoint
class ShowSeasonsFragment : BaseFragment() {

    private val viewModel: ShowDetailsViewModel by activityViewModels()


    private var _bindings: FragmentShowSeasonsBinding? = null
    private val bindings get() = _bindings!!

    @Inject
    lateinit var glide: RequestManager

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SeasonsAdapter
    private lateinit var popupAdapter: SeasonsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
       _bindings = FragmentShowSeasonsBinding.inflate(layoutInflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()
        initPopupRecylcerAdapter()

        bindings.showdetailsactivityAllSeasonsButton.setOnClickListener {
            getSeasonsPopup().show()
        }

        getSeason()
    }

    private fun getSeason() {
        lifecycleScope.launchWhenStarted {
            val seasonsProgressBar = bindings.showseasonsfragmentProgressbar

            viewModel.seasons.collectLatest { seasonsResource ->
                when(seasonsResource) {
                    is Resource.Loading -> {
                        seasonsProgressBar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        seasonsProgressBar.visibility = View.GONE

                        val sortedSeasonData = seasonsResource.data?.sortedBy { it.season.season_number }

                        adapter.submitList(sortedSeasonData?.reversed())
                        popupAdapter.submitList(sortedSeasonData)
                    }
                    is Resource.Error -> {
                        seasonsProgressBar.visibility = View.GONE

                        if(!seasonsResource.data.isNullOrEmpty()) {
                            adapter.submitList(seasonsResource.data)
                        }

                        handleError(seasonsResource.error, null)
                    }
                }
            }
        }
    }

    private fun initRecyclerView() {
        recyclerView = bindings.showseasonsfragmentRecyclerview

        val lm = LinearLayoutManager(requireContext())

        lm.orientation = LinearLayoutManager.HORIZONTAL

        adapter = SeasonsAdapter(glide, false) {
            navigateToSeason(it)
        }

        recyclerView.layoutManager = lm
        recyclerView.adapter = adapter

    }

    private fun initPopupRecylcerAdapter() {
        popupAdapter = SeasonsAdapter(glide, true) {
            navigateToSeason(it)
        }
    }

    private fun getSeasonsPopup(): AlertDialog {
        val popupRecyclerView = RecyclerView(requireContext())
        val lm = LinearLayoutManager(requireContext())

        popupRecyclerView.layoutManager = lm
        popupRecyclerView.adapter = popupAdapter

        return AlertDialog.Builder(requireContext())
            .setTitle("Season List")
            .setView(popupRecyclerView)
            .setNegativeButton("Close") { d, i ->
                d.dismiss()
            }
            .create()
    }

    private fun navigateToSeason(tmSeasonAndStats: TmSeasonAndStats) {
        Log.e(TAG, "navigateToSeason:  here")

        (activity as OnNavigateToEntity).navigateToSeason(
            SeasonDataModel(
                tmSeasonAndStats.season.show_trakt_id, tmSeasonAndStats.season.show_tmdb_id, tmSeasonAndStats.season.season_number, "Season ${tmSeasonAndStats.season.season_number}")
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _bindings = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = ShowSeasonsFragment()
    }
}