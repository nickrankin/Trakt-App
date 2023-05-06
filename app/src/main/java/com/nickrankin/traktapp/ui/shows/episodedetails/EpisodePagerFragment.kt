package com.nickrankin.traktapp.ui.shows.episodedetails

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.dao.show.model.TmEpisodeAndStats
import com.nickrankin.traktapp.databinding.EpisodePagerBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.shows.EpisodeDetailsViewModel
import com.nickrankin.traktapp.model.shows.EpisodePagerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

private const val TAG = "EpisodePagerFragment"
@AndroidEntryPoint
class EpisodePagerFragment(): BaseFragment(), SwipeRefreshLayout.OnRefreshListener {
    private var _bindings: EpisodePagerBinding? = null
    private val bindings get() = _bindings!!
    private lateinit var episodeDataModel: EpisodeDataModel

    private val viewModel: EpisodePagerViewModel by activityViewModels()
    private lateinit var viewPager: ViewPager2
    private lateinit var episodeSeriesAdapter: EpisodeSeriesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _bindings = EpisodePagerBinding.inflate(inflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = bindings.episodepagerPager

        episodeDataModel = arguments!!.getParcelable<EpisodeDataModel>(EpisodeDetailsFragment.EPISODE_DATA_KEY)!!


        viewModel.switchEpisodeDataModel(episodeDataModel)
        initEpisodePager()

        getSeasonEpisodes()
    }

    private fun getSeasonEpisodes() {
        lifecycleScope.launchWhenStarted {
            viewModel.seasonEpisodes.collectLatest { episodeNumberAndseasonEpisodesResource ->
                val currentEpisodeNumber = episodeNumberAndseasonEpisodesResource.first
                val seasonEpisodesResource = episodeNumberAndseasonEpisodesResource.second
                Log.d(TAG, "getSeasonEpisodes: currentEpisodeNumber $currentEpisodeNumber")

                if (currentEpisodeNumber == -1) {
                    Log.e(TAG, "getSeasonEpisodes: Current episode is not correct")
                }

                when (seasonEpisodesResource) {
                    is Resource.Loading -> {
                    }
                    is Resource.Success -> {
                        val episodeAndStatsData = seasonEpisodesResource.data?.sortedBy { it.episode.episode_number } ?: emptyList()
                        episodeSeriesAdapter.updateEpisodes(episodeAndStatsData)

                        viewPager.setCurrentItem(currentEpisodeNumber-1, false)

                    }
                    is Resource.Error -> {
                        handleError(seasonEpisodesResource.error, null)
                    }

                }
            }
        }
    }

    private fun initEpisodePager() {
        viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        episodeSeriesAdapter = EpisodeSeriesAdapter(this)

        viewPager.adapter = episodeSeriesAdapter
    }

    companion object {
        @JvmStatic
        fun newInstance() = EpisodePagerFragment()
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {
        // Refreshing the one fragment will reload the data for all episodes in this season, so we can target the last.
        val episodeFragment = childFragmentManager.fragments.filter { it is EpisodeDetailsFragment }.last()

        try {
            (episodeFragment as SwipeRefreshLayout.OnRefreshListener).onRefresh()
        } catch(cce: ClassCastException) {
            Log.e(TAG, "onRefresh: Class ${episodeFragment.javaClass.name} doesn't implement SwipeRefreshLayout.OnRefresh", )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    class EpisodeSeriesAdapter(fragment: Fragment): FragmentStateAdapter(fragment) {
        private var episodes: List<TmEpisodeAndStats> = emptyList()
        override fun getItemCount(): Int {
            return episodes.size
        }

        override fun createFragment(position: Int): Fragment {
            val fragment = EpisodeDetailsFragment()
            val episodeDataModel = getEpisodeDataModelByNumber(position, episodes)
            fragment.arguments = Bundle().apply {
                putParcelable(EpisodeDetailsFragment.EPISODE_DATA_KEY, episodeDataModel)
            }

            return fragment
        }

        fun updateEpisodes(episodes: List<TmEpisodeAndStats>) {
            this.episodes = episodes
            notifyDataSetChanged()
        }

        private fun getEpisodeDataModelByNumber(position: Int, episodes: List<TmEpisodeAndStats>): EpisodeDataModel {
            return EpisodeDataModel(
                episodes[position].episode.show_trakt_id,
                episodes[position].episode.show_tmdb_id,
                episodes[position].episode.season_number ?: 0,
                episodes[position].episode.episode_number ?: 0,
                ""
            )
        }
    }
}