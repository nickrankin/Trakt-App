package com.nickrankin.traktapp.repo.stats

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.movies.MoviesDatabase
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.stats.model.CollectedMoviesStats
import com.nickrankin.traktapp.dao.stats.model.RatingsEpisodesStats
import com.nickrankin.traktapp.dao.stats.model.RatingsMoviesStats
import com.nickrankin.traktapp.dao.stats.model.WatchedMoviesStats
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.BaseMovie
import com.uwetrottmann.trakt5.entities.RatedEpisode
import com.uwetrottmann.trakt5.entities.RatedMovie
import com.uwetrottmann.trakt5.entities.UserSlug
import com.uwetrottmann.trakt5.enums.RatingsFilter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

private const val TAG = "EpisodesStatsRepository"
class EpisodesStatsRepository @Inject constructor(private val traktApi: TraktApi, private val showsDatabase: ShowsDatabase, private val sharedPreferences: SharedPreferences) {

    private val watchedEpisodesStatsDao = showsDatabase.watchedEpisodesStatsDao()
    private val ratingsEpisodesStatsDao = showsDatabase.ratedEpisodesStatsDao()
    val watchedEpisodeStats = watchedEpisodesStatsDao.getWatchedStats()
    val ratedEpisodesStats = ratingsEpisodesStatsDao.getRatingsStats()


    suspend fun refreshEpisodeStats() {
        refreshEpisodeRatings()
    }

    private suspend fun refreshEpisodeRatings() {


        Log.d(TAG, "refreshEpisodeRatings: Refreshing episode ratings")
        val limit = 100
        var page = 1

        showsDatabase.withTransaction {
            ratingsEpisodesStatsDao.deleteRatingsStats()
        }

        // Too many ratings caused HTTP timeout, quick fix...
            while(true) {
                val result = traktApi.tmUsers().ratingsEpisodes(
                    UserSlug(
                        sharedPreferences.getString(
                            AuthActivity.USER_SLUG_KEY,
                            "NULL"
                        )
                    ), RatingsFilter.ALL, null, limit, page
                )
                insertRatedEpisodesStats(result)

                page = page.inc()

                if(result.isEmpty()) {
                    Log.d(TAG, "refreshEpisodeRatings: Finished")
                    break;
                }
            }
    }





    private suspend fun insertRatedEpisodesStats(ratedEpisodes: List<RatedEpisode>) {
        val ratedEpisodeStatsList: MutableList<RatingsEpisodesStats> = mutableListOf()

        ratedEpisodes.forEach { ratedEpisode ->
            ratedEpisodeStatsList.add(
                RatingsEpisodesStats(
                    ratedEpisode.episode?.ids?.trakt ?: 0,
                    ratedEpisode.show?.ids?.trakt ?: 0,
                    ratedEpisode.episode?.season ?: 0,
                    ratedEpisode.episode?.number ?: 0,
                    ratedEpisode.rating?.value ?: 0,
                    ratedEpisode.rated_at
                )
            )
        }

        showsDatabase.withTransaction {
            ratingsEpisodesStatsDao.insert(ratedEpisodeStatsList)
        }
    }

}

