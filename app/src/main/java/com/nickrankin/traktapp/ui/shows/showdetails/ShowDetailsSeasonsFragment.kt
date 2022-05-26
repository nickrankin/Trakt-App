package com.nickrankin.traktapp.ui.shows.showdetails

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.adapter.shows.SeasonsAdapter
import com.nickrankin.traktapp.dao.show.TmSeasonAndStats
import com.nickrankin.traktapp.dao.show.model.TmSeason
import com.nickrankin.traktapp.databinding.ShowDetailsSeasonsFragmentBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.SeasonDataModel
import com.nickrankin.traktapp.model.shows.showdetails.ShowDetailsSeasonsViewModel
import com.nickrankin.traktapp.repo.shows.SeasonEpisodesRepository
import com.nickrankin.traktapp.ui.shows.SeasonEpisodesActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

private const val TAG = "ShowDetailsSeasonsFragm"

@AndroidEntryPoint
class ShowDetailsSeasonsFragment() : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    private val viewModel: ShowDetailsSeasonsViewModel by activityViewModels()
    private lateinit var bindings: ShowDetailsSeasonsFragmentBinding

    private lateinit var seasonsRecyclerView: RecyclerView
    private lateinit var seasonsAdapter: SeasonsAdapter


    @Inject
    lateinit var glide: RequestManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindings = ShowDetailsSeasonsFragmentBinding.inflate(layoutInflater)
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

        seasonsAdapter = SeasonsAdapter(glide, callback = { selectedSeasonData ->
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
            viewModel.seasons.collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        bindings.apply {
                            showdetailsseasonsProgressbar.visibility = View.VISIBLE
                            showdetailsseasonsRecyclerview.visibility = View.GONE
                        }
                    }
                    is Resource.Success -> {
                        bindings.apply {
                            showdetailsseasonsMainGroup.visibility = View.VISIBLE
                            showdetailsseasonsProgressbar.visibility = View.GONE
                            showdetailsseasonsRecyclerview.visibility = View.VISIBLE
                        }

                        submitSeasons(resource.data ?: emptyList())
                    }
                    is Resource.Error -> {
                        bindings.apply {
                            showdetailsseasonsMainGroup.visibility = View.GONE

                            showdetailsseasonsProgressbar.visibility = View.GONE
                            showdetailsseasonsRecyclerview.visibility = View.GONE
                        }

                        Log.e(
                            TAG,
                            "getSeasons: Error getting Seasons. ${resource.error?.localizedMessage}",
                        )
                        resource.error?.printStackTrace()

                        if (resource.data != null) {
                            // use cached datas

                            bindings.apply {
                                showdetailsseasonsProgressbar.visibility = View.GONE
                                showdetailsseasonsRecyclerview.visibility = View.VISIBLE
                            }

                            submitSeasons(resource.data ?: emptyList())

                        }

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
        val intent = Intent(requireContext(), SeasonEpisodesActivity::class.java)
        intent.putExtra(SeasonEpisodesActivity.SEASON_DATA_KEY, SeasonDataModel(
            showTraktId, showTmdbId, seasonNumber, title))

        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()

        viewModel.onStart()
    }

    override fun onRefresh() {
        Log.d(TAG, "onRefresh: Refreshing show seasons")
        viewModel.onRefresh()
    }


    companion object {
        fun newInstance() = ShowDetailsSeasonsFragment()
    }

}