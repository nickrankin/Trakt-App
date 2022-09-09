package com.nickrankin.traktapp.repo.stats

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.movies.MoviesDatabase
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.stats.model.*
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.ProgressLastActivity
import com.uwetrottmann.trakt5.enums.RatingsFilter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

private const val REFRESH_INTERVAL = 24L
private const val TAG = "StatsRepository"

class StatsRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val sharedPreferences: SharedPreferences,
    private val moviesDatabase: MoviesDatabase,
    private val showsDatabase: ShowsDatabase) {

    // Movies
    private val collectedMoviesStatsDao = moviesDatabase.collectedMoviesStatsDao()
    private val watchedMoviesStatsDao = moviesDatabase.watchedMoviesStatsDao()
    private val ratingsMoviesStatsDao = moviesDatabase.ratedMoviesStatsDao()

    //Shows
    private val collectedShowsStatsDao = showsDatabase.collectedShowsStatsDao()
    private val ratingsShowsStatsDao = showsDatabase.ratedShowsStatsDao()
    private val watchedShowsStatsDao = showsDatabase.watchedShowsStatsDao()

    private val watchedSeasonStatsDao = showsDatabase.watchedSeasonStatsDao()

    private val watchedEpisodesStatsDao = showsDatabase.watchedEpisodesStatsDao()
    private val ratingsEpisodesStatsDao = showsDatabase.ratedEpisodesStatsDao()

    // Flows Movies
    val collectedMoviesStats = collectedMoviesStatsDao.getCollectedStats()
    val watchedMoviesStats = watchedMoviesStatsDao.getWatchedStats()
    val ratedMoviesStats = ratingsMoviesStatsDao.getRatingsStats()

    // Flows Shows
    val collectedShowsStats = collectedShowsStatsDao.getCollectedStats()
    val watchedShowsStats = watchedShowsStatsDao.getWatchedStats()
    val ratedShowsStats = ratingsShowsStatsDao.getRatingsStats()

    val watchedSeasonStats = watchedSeasonStatsDao.getWatchedStats()
    val watchedEpisodeStats = watchedEpisodesStatsDao.getWatchedStats()

    val ratedEpisodesStats = ratingsEpisodesStatsDao.getRatingsStats()

    suspend fun getCollectedMovieStatsById(traktId: Int): Flow<CollectedMoviesStats?> {
        return collectedMoviesStatsDao.getCollectedMovieStatsById(traktId)
    }

    suspend fun refreshAllMovieStats() {
            Log.d(TAG, "refreshAllMovieStats: Refreshing All Movie Stats")
            refreshWatchedMovies()
            refreshCollectedMovieStats()
            refreshMovieRatingsStats()

    }

    suspend fun refreshAllShowStats() {
        purgeWatchedShowDataStata()
        Log.d(TAG, "refreshAllShowStatsTest: Refreshing All Show stats")
            refreshShowsRatings()
            refreshEpisodeRatings()
            refreshCollectedShows()
            // Will also refresh Episodes and Seasons watched stats
            refreshWatchedShows()

    }

    suspend fun refreshWatchedMovies() {
        val watchedMoviesStats = traktApi.tmUsers().watchedMovies(
            UserSlug(
                sharedPreferences.getString(
                    AuthActivity.USER_SLUG_KEY,
                    "NULL"
                )
            ), null
        )
        insertWatchedMoviesStats(watchedMoviesStats)
    }

    suspend fun refreshCollectedMovieStats() {
        val collectedMoviesStats = traktApi.tmUsers().collectionMovies(
            UserSlug(
                sharedPreferences.getString(
                    AuthActivity.USER_SLUG_KEY,
                    "NULL"
                )
            ), null
        )

        insertCollectedMoviesStats(collectedMoviesStats)
    }

    suspend fun refreshWatchedShows() {
      //  Log.d(TAG, "refreshWatchedShows: Refreshing watched Shows")
        val watchedShowStatsResponse = traktApi.tmUsers().watchedShows(
            UserSlug(
                sharedPreferences.getString(
                    AuthActivity.USER_SLUG_KEY,
                    "NULL"
                )
            ), null
        )

        insertWatchedShowsStats(watchedShowStatsResponse)


        // Get the Season watched stats
        watchedShowStatsResponse.map { watchedShow ->
            getWatchedSeasonStatsPerShow(watchedShow.show?.ids?.trakt ?: 0)
        }


    }

    suspend fun refreshCollectedShows() {
        val collectedShowStatsResponse = traktApi.tmUsers().collectionShows(
            UserSlug(
                sharedPreferences.getString(
                    AuthActivity.USER_SLUG_KEY,
                    "NULL"
                )
            ), null
        )

        insertCollectedShowsStats(collectedShowStatsResponse)

    }

    private suspend fun refreshMovieRatingsStats() {
        val ratedMoviesStats = traktApi.tmUsers().ratingsMovies(
            UserSlug(
                sharedPreferences.getString(
                    AuthActivity.USER_SLUG_KEY,
                    "NULL"
                )
            ), RatingsFilter.ALL, null
        )

        insertRatedMoviesStats(ratedMoviesStats)
    }

    private suspend fun refreshShowsRatings() {
        val showRatingsResponse = traktApi.tmUsers().ratingsShows(
            UserSlug(
                sharedPreferences.getString(
                    AuthActivity.USER_SLUG_KEY,
                    "NULL"
                )
            ), RatingsFilter.ALL, null
        )

        insertRatedShowsStats(showRatingsResponse)
    }
    private suspend fun getWatchedSeasonStatsPerShow(showTraktId: Int) {
        Log.d(TAG, "getWatchedSeasonStats: Refreshing Watched Season Stats")
        val baseShow = traktApi.tmShows().watchedProgress(showTraktId.toString(), true, true, false, ProgressLastActivity.WATCHED, null)
        insertWatchedSeasonsStats(showTraktId, baseShow)
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

    private suspend fun insertCollectedMoviesStats(movies: List<BaseMovie>) {
        val collectedMovieStatsList: MutableList<CollectedMoviesStats> = mutableListOf()

        movies.map { baseMovie ->
            collectedMovieStatsList.add(
                CollectedMoviesStats(
                    baseMovie.movie?.ids?.trakt ?: 0,
                    baseMovie.movie?.ids?.tmdb,
                    baseMovie.collected_at,
                    baseMovie.movie?.title ?: "",
                    baseMovie.listed_at
                )
            )
        }

        moviesDatabase.withTransaction {
            collectedMoviesStatsDao.deleteCollectedStats()
            collectedMoviesStatsDao.insert(collectedMovieStatsList)
        }
    }

    private suspend fun insertWatchedMoviesStats(movies: List<BaseMovie>) {
        val watchedMoviesStatsList: MutableList<WatchedMoviesStats> = mutableListOf()

        movies.map { baseMovie ->
            watchedMoviesStatsList.add(
                WatchedMoviesStats(
                    baseMovie.movie?.ids?.trakt ?: 0,
                    baseMovie.movie?.ids?.tmdb,
                    baseMovie.last_watched_at,
                    baseMovie.movie?.title ?: "",
                    baseMovie.listed_at,
                    baseMovie.plays
                )
            )
        }

        moviesDatabase.withTransaction {
            watchedMoviesStatsDao.deleteWatchedStats()
            watchedMoviesStatsDao.insert(watchedMoviesStatsList)
        }
    }

    private suspend fun insertRatedMoviesStats(ratedMovies: List<RatedMovie>) {
        val ratedMoviesStatsList: MutableList<RatingsMoviesStats> = mutableListOf()

        ratedMovies.forEach { ratedMovie ->
            ratedMoviesStatsList.add(
                RatingsMoviesStats(
                    ratedMovie.movie?.ids?.trakt ?: 0,
                    ratedMovie.movie?.ids?.tmdb ?: 0,
                    ratedMovie.rating?.value ?: 0,
                    ratedMovie.movie?.title ?: "",
                    ratedMovie.rated_at
                )
            )
        }

        moviesDatabase.withTransaction {
            ratingsMoviesStatsDao.deleteRatingsStats()
            ratingsMoviesStatsDao.insert(ratedMoviesStatsList)
        }
    }

    private suspend fun insertCollectedShowsStats(shows: List<BaseShow>) {
        val collectedShowsStatsList: MutableList<CollectedShowsStats> = mutableListOf()

        shows.map { baseShow ->
            collectedShowsStatsList.add(
                CollectedShowsStats(
                    baseShow.show?.ids?.trakt ?: 0,
                    baseShow.show?.ids?.tmdb,
                    baseShow.last_collected_at,
                    baseShow.last_updated_at,
                    baseShow.reset_at,
                    baseShow.completed ?: 0,
                    baseShow.show?.title ?: "",
                    baseShow.listed_at
                )
            )
        }

        showsDatabase.withTransaction {
            collectedShowsStatsDao.deleteCollectedStats()
            collectedShowsStatsDao.insert(collectedShowsStatsList)
        }

    }

    private suspend fun purgeWatchedShowDataStata() {
        showsDatabase.withTransaction {
            watchedShowsStatsDao.deleteWatchedStats()
            watchedSeasonStatsDao.deleteWatchedStats()
            watchedEpisodesStatsDao.deleteWatchedStats()

        }
    }

    private suspend fun insertWatchedShowsStats(watchedShows: List<BaseShow>) {
        val watchedShowsStatsList: MutableList<WatchedShowsStats> = mutableListOf()

        watchedShows.map { baseShow ->
            watchedShowsStatsList.add(
                WatchedShowsStats(
                    baseShow.show?.ids?.trakt ?: 0,
                    baseShow.show?.ids?.tmdb,
                    baseShow.last_watched_at,
                    baseShow.reset_at,
                    baseShow.completed ?: 0,
                    baseShow.show?.title ?: "",
                    baseShow.listed_at,
                    baseShow.plays ?: 0
                )
            )

            insertWatchedEpisodesStats(baseShow)
        }

        showsDatabase.withTransaction {
            watchedShowsStatsDao.insert(watchedShowsStatsList)
        }
    }

    private suspend fun insertWatchedSeasonsStats(showTraktId: Int, baseShow: BaseShow) {
        val watchedSeasonsStats: MutableList<WatchedSeasonStats> = mutableListOf()

        baseShow.seasons?.map { baseSeason ->
            watchedSeasonsStats.add(
                WatchedSeasonStats(
                    "${showTraktId}S${baseSeason.number}",
                    showTraktId,
                    baseSeason.number ?: 0,
                    baseSeason?.aired ?: 0,
                    baseSeason?.completed ?: 0
                )
            )
        }

        showsDatabase.withTransaction {
            watchedSeasonStatsDao.insert(watchedSeasonsStats)
        }
    }

    private suspend fun insertWatchedEpisodesStats(baseShow: BaseShow) {
        val watchedEpisodeStats: MutableList<WatchedEpisodeStats> = mutableListOf()

        baseShow.seasons?.map { baseSeason ->
            val seasonNumber = baseSeason.number

            baseSeason.episodes?.map { baseEpisode ->
                watchedEpisodeStats.add(
                    WatchedEpisodeStats(
                        "${baseShow.show?.ids?.trakt}S${baseSeason.number}E${baseEpisode.number}",
                        baseShow.show?.ids?.tmdb,
                        baseShow.show?.ids?.trakt ?: 0,
                        baseShow.show?.title ?: "",
                        baseSeason.number ?: 0,
                        baseEpisode?.number ?: 0,
                        baseEpisode.last_watched_at,
                        baseEpisode.plays ?: 0
                    )
                )
            }
        }

        showsDatabase.withTransaction {
            watchedEpisodesStatsDao.insert(watchedEpisodeStats)
        }
    }

    private suspend fun insertRatedShowsStats(ratedShows: List<RatedShow>) {
        val ratedShowsStatsList: MutableList<RatingsShowsStats> = mutableListOf()

        ratedShows.forEach { ratedShow ->
            ratedShowsStatsList.add(
                RatingsShowsStats(
                    ratedShow.show?.ids?.trakt ?: 0,
                    ratedShow.show?.ids?.tmdb ?: 0,
                    ratedShow.rating?.value ?: 0,
                    ratedShow.show?.title ?: "",
                    ratedShow.rated_at
                )
            )
        }

        showsDatabase.withTransaction {
            ratingsShowsStatsDao.deleteRatingsStats()
            ratingsShowsStatsDao.insert(ratedShowsStatsList)
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