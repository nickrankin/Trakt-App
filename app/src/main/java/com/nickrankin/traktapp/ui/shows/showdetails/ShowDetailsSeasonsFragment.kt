package com.nickrankin.traktapp.ui.shows.showdetails

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.OnNavigateToEntity
import com.nickrankin.traktapp.SplitViewActivity
import com.nickrankin.traktapp.adapter.shows.SeasonsAdapter
import com.nickrankin.traktapp.dao.show.TmSeasonAndStats
import com.nickrankin.traktapp.databinding.ShowDetailsSeasonsFragmentBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.SeasonDataModel
import com.nickrankin.traktapp.model.shows.ShowDetailsViewModel
import com.nickrankin.traktapp.ui.shows.SeasonEpisodesFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "ShowDetailsSeasonsFragm"

@AndroidEntryPoint
class ShowDetailsSeasonsFragment : BaseFragment() {
    private val viewModel: ShowDetailsViewModel by activityViewModels()

    private var _bindings: ShowDetailsSeasonsFragmentBinding? = null
    private val bindings get() = _bindings!!



    private lateinit var seasonsRecyclerView: RecyclerView
    private lateinit var seasonsAdapter: SeasonsAdapter


    @Inject
    lateinit var glide: RequestManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _bindings = ShowDetailsSeasonsFragmentBinding.inflate(layoutInflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecycler()
        getSeasons()

    }

    private fun initRecycler() {
        seasonsRecyclerView = bindings.showdetailsseasonsRecyclerview
        val layoutManager = LinearLayoutManager(requireContext())

        seasonsAdapter = SeasonsAdapter(glide, false, callback = { selectedSeasonData ->
            val selectedSeason = selectedSeasonData.season

            navigateToSeason(
                selectedSeason.show_trakt_id,
                selectedSeason.show_tmdb_id ?: 0,
                selectedSeason.season_number,
                selectedSeason.name
            )
        })

        seasonsRecyclerView.layoutManager = layoutManager
        seasonsRecyclerView.adapter = seasonsAdapter

    }

    private fun getSeasons() {
        lifecycleScope.launchWhenStarted {
            viewModel.seasons.collectLatest { seasonsResource ->
                
                when(seasonsResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getSeasons: Loading Seasons")
                        bindings.showdetailsseasonsProgressbar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        val seasons = seasonsResource.data
                        
                        if(seasons != null) {
                            bindings.apply {
                                showdetailsseasonsMainGroup.visibility = View.VISIBLE
                                showdetailsseasonsProgressbar.visibility = View.GONE
                                showdetailsseasonsRecyclerview.visibility = View.VISIBLE
                            }

                            submitSeasons(seasons)
                        }
                    }
                    is Resource.Error -> {

                        val seasons = seasonsResource.data

                        if(seasons != null) {
                            bindings.apply {
                                showdetailsseasonsMainGroup.visibility = View.VISIBLE
                                showdetailsseasonsProgressbar.visibility = View.GONE
                                showdetailsseasonsRecyclerview.visibility = View.VISIBLE
                            }

                            submitSeasons(seasons)
                        }

                        handleError(seasonsResource.error, null)
                    }

                }


            }
        }
    }

    private fun submitSeasons(seasons: List<TmSeasonAndStats>) {
        seasonsAdapter.submitList(seasons.sortedBy {
            it.season.season_number
        })
    }

    private fun navigateToSeason(showTraktId: Int, showTmdbId: Int, seasonNumber: Int, title: String) {
        (activity as OnNavigateToEntity).navigateToSeason(
            SeasonDataModel(
                showTraktId, showTmdbId, seasonNumber, title)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _bindings = null
    }

    companion object {
        fun newInstance() = ShowDetailsSeasonsFragment()
    }

}