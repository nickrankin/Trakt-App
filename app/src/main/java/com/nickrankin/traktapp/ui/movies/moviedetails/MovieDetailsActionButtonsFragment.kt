package com.nickrankin.traktapp.ui.movies.moviedetails

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.nickrankin.traktapp.dao.history.model.HistoryEntry
import com.nickrankin.traktapp.dao.lists.model.ListEntry
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.dao.lists.model.TraktListEntry
import com.nickrankin.traktapp.dao.movies.model.TmMovie
import com.nickrankin.traktapp.dao.stats.model.CollectedStats
import com.nickrankin.traktapp.dao.stats.model.RatingStats
import com.nickrankin.traktapp.databinding.LayoutActionButtonsBinding
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.ActionButtonEvent
import com.nickrankin.traktapp.model.movies.MovieDetailsViewModel
import com.nickrankin.traktapp.ui.ActionButtonsBaseFragment
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import org.threeten.bp.OffsetDateTime

private const val TAG = "ShowDetailsActionButton"
@AndroidEntryPoint
class MovieDetailsActionButtonsFragment : ActionButtonsBaseFragment() {

    private val viewModel: MovieDetailsViewModel by activityViewModels()

    private lateinit var bindings: LayoutActionButtonsBinding

    private val traktRatingChannel = Channel<Double?>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = LayoutActionButtonsBinding.inflate(inflater)
        return bindings.root
    }

    override fun setup(config: (bindings: LayoutActionButtonsBinding, traktId: Int, title: String, type: Type, enableCheckinButton: Boolean, enableHistoryButton: Boolean) -> Unit)   {

        lifecycleScope.launchWhenStarted {
                viewModel.movie.collectLatest { movieResource ->

                    when (movieResource) {
                        is Resource.Loading -> {
                            Log.e(TAG, "setup: Loading movie...")
                        }
                        is Resource.Success -> {
                            val movie = movieResource.data
                            if (movie != null) {
                                config(bindings, movie.trakt_id, movie.title, Type.MOVIE, true, true)
                                traktRatingChannel.send(movie.trakt_rating)

                            }
                        }
                        is Resource.Error -> {
                            handleError(movieResource.error, null)
                        }
                    }
                }
            }
    }

    override fun onNewEvent(onNewEvent: (event: ActionButtonEvent) -> Unit) {
        lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { eventResource ->
                onNewEvent(eventResource)
            }
        }

    }

    override fun setTraktRating(onRatingChanged: (traktRating: Double?) -> Unit) {
        lifecycleScope.launchWhenStarted {
            traktRatingChannel.receiveAsFlow().collectLatest { rating ->
                onRatingChanged(rating)

            }
        }
    }

    override fun addToWatchedHistory(traktId: Int, watchedDate: OffsetDateTime) {
        viewModel.addToWatchedHistory(traktId, watchedDate)
    }

    override fun updatePlayCount(onPlayCountUpdated: (List<HistoryEntry>) -> Unit) {
        lifecycleScope.launchWhenStarted {
            viewModel.watchedMovieHistoryEntries.collectLatest { watchedHistoryEntriesResource ->
                when(watchedHistoryEntriesResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getPlayCount: Loading")
                    }
                    is Resource.Success -> {
                        val watchedMovieHistory = watchedHistoryEntriesResource.data

                        onPlayCountUpdated(watchedMovieHistory!!)
                    }
                    is Resource.Error -> {
                        handleError(watchedHistoryEntriesResource.error, null)
                    }

                }
            }
        }
    }

    override fun removeHistoryEntry(historyEntry: HistoryEntry) {
        viewModel.removeHistoryEntry(historyEntry)
    }


    override fun updateRatingText(
        onRatingChanged: (ratingStats: RatingStats?) -> Unit
    ) {
        lifecycleScope.launchWhenStarted {
            viewModel.movieRatings.collectLatest { ratingResource ->

                when(ratingResource) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        onRatingChanged(ratingResource.data)
                    }
                    is Resource.Error -> {
                        handleError(ratingResource.error, null)
                    }

                }

            }
        }
    }

    override fun setNewRating(traktId: Int, newRating: Int) {
        viewModel.addRating(newRating, traktId)

    }

    override fun deleteRating(traktId: Int) {
        viewModel.deleteRating(traktId)
    }

    override fun checkin(traktId: Int) {
        viewModel.checkin(traktId, false)

    }

    override fun overrideCheckin(traktId: Int) {
        viewModel.checkin(traktId, true)
    }

    override fun getCollectedStats(traktId: Int, oncollectedStateChanged: (collectedmoviestats: CollectedStats?) -> Unit) {
        lifecycleScope.launchWhenStarted {
            viewModel.collectedMovieStats.collectLatest { collectedMovieStatsResource ->

                when(collectedMovieStatsResource) {
                    is Resource.Loading -> {

                    }
                    is Resource.Success -> {
                        oncollectedStateChanged(collectedMovieStatsResource.data)
                    }
                    is Resource.Error -> {
                        handleError(collectedMovieStatsResource.error, null)
                    }

                }

            }
        }
    }

    override fun addToCollection(traktId: Int) {
        viewModel.addToCollection(traktId)
    }

    override fun removeFromCollection(traktId: Int) {
        viewModel.deleteFromCollection(traktId)
    }

    override fun getLists(onListsChanged: (listEntries: List<Pair<TraktList, List<TraktListEntry>>>) -> Unit) {
        lifecycleScope.launchWhenStarted {
            viewModel.listsAndEntries.collectLatest { listsAndEntriesResource ->
                Log.e(TAG, "getLists: $listsAndEntriesResource")
                when(listsAndEntriesResource) {
                    is Resource.Loading -> {
                        Log.e(TAG, "getLists: Loading ...")
                    }
                    is Resource.Success -> {
                        Log.e(TAG, "getLists: Got lists ${listsAndEntriesResource.data?.size}")

                        onListsChanged(listsAndEntriesResource.data ?: emptyList())

                    }
                    is Resource.Error -> {
                        handleError(listsAndEntriesResource.error, null)
                    }

                }
            }
        }
    }

    override fun addListEntry(traktId: Int, traktList: TraktList) {
        viewModel.addListEntry(traktId, traktList.trakt_id)
    }

    override fun removeListEntry(traktId: Int, traktList: TraktList) {
        viewModel.removeListEntry(
            traktId,
            traktList.trakt_id
        )
    }

    companion object {
        fun newInstance() = MovieDetailsActionButtonsFragment()
    }

}