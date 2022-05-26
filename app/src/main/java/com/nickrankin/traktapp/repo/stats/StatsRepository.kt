package com.nickrankin.traktapp.repo.stats

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.movies.MoviesDatabase
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.stats.model.*
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.ProgressLastActivity
import com.uwetrottmann.trakt5.enums.RatingsFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

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
    //val rated

    private val collectedShowsStatsDao = showsDatabase.collectedShowsStatsDao()
    private val watchedEpisodesStatsDao = showsDatabase.watchedEpisodesStatsDao()
    private val ratingsShowsStatsDao = showsDatabase.ratedShowsStatsDao()
    private val watchedShowsStatsDao = showsDatabase.watchedShowsStatsDao()
    private val watchedSeasonStatsDao = showsDatabase.watchedSeasonStatsDao()
    private val ratingsEpisodesStatsDao = showsDatabase.ratedEpisodesStatsDao()

    // Flows Movies
    val collectedMoviesStats = collectedMoviesStatsDao.getCollectedStats()
    val watchedMoviesStats = watchedMoviesStatsDao.getWatchedStats()
    val ratedMoviesStats = ratingsMoviesStatsDao.getRatingsStats()

    // Flows Shows
    val collectedShowsStats = collectedShowsStatsDao.getCollectedStats()
    val watchedShowsStats = watchedShowsStatsDao.getWatchedStats()
    val watchedSeasonStats = watchedSeasonStatsDao.getWatchedStats()
    val watchedEpisodeStats = watchedEpisodesStatsDao.getWatchedStats()
    val ratedShowsStats = ratingsShowsStatsDao.getRatingsStats()
    val ratedEpisodesStats = ratingsEpisodesStatsDao.getRatingsStats()

    suspend fun getWatchedSeasonStats(showTraktId: Int, shouldFetch: Boolean): Resource<Boolean> {
        Log.d(TAG, "getWatchedSeasonStats: Calling")
        val alreadyCached = watchedSeasonStatsDao.getWatchedStatsByShow(showTraktId).first()

        val lastRefreshedSeasonStats =
            sharedPreferences.getString(LAST_REFRESH_SEASON_PROGRESS_STATS_KEY, null).let { timestamp ->
                if (timestamp != null) {
                    OffsetDateTime.parse(timestamp)
                } else {
                    null
                }
            }

        if (alreadyCached.isEmpty() || shouldFetch || lastRefreshedSeasonStats?.isBefore(
                OffsetDateTime.now().minusHours(
                    REFRESH_INTERVAL
                )
            ) == true
        ) {

            if (shouldFetch) {
                Log.d(TAG, "getWatchedSeasonStats: Refresh Force")
            } else if (!shouldFetch && lastRefreshedSeasonStats?.isBefore(
                    OffsetDateTime.now().minusHours(
                        REFRESH_INTERVAL
                    )
                ) == true
            ) {
                Log.d(TAG, "getWatchedSeasonStats: Refresh scheduled")
            }

            try {
                val baseShow = traktApi.tmShows().watchedProgress(showTraktId.toString(), true, true, false, ProgressLastActivity.WATCHED, null)

                insertWatchedSeasonsStats(showTraktId, baseShow)

                sharedPreferences.edit()
                    .putString(LAST_REFRESH_SEASON_PROGRESS_STATS_KEY, OffsetDateTime.now().toString())
                    .apply()

                return Resource.Success(true)
            } catch (t: Throwable) {
                t.printStackTrace()
                return Resource.Error(t, null)
            }
        } else {
            Log.d(TAG, "getWatchedSeasonStats: No refresh needed")
        }
        return Resource.Success(true)
    }

    suspend fun refreshShowStats(shouldRefresh: Boolean): Resource<Boolean> {

        val lastRefreshedShowStats =
            sharedPreferences.getString(LAST_REFRESH_SHOW_STATS_KEY, null).let { timestamp ->
                if (timestamp != null) {
                    OffsetDateTime.parse(timestamp)
                } else {
                    null
                }
            }

        if (shouldRefresh || lastRefreshedShowStats?.isBefore(
                OffsetDateTime.now().minusHours(
                    REFRESH_INTERVAL
                )
            ) == true
        ) {

            if (shouldRefresh) {
                Log.d(TAG, "refreshShowStats: Refresh Force")
            } else if (!shouldRefresh && lastRefreshedShowStats?.isBefore(
                    OffsetDateTime.now().minusHours(
                        REFRESH_INTERVAL
                    )
                ) == true
            ) {
                Log.d(TAG, "refreshShowStats: Refresh scheduled")
            }

            try {

                refreshCollectedShows()
                refreshWatchedShows()
                refreshShowsRatings()
                refreshEpisodeRatings()

                sharedPreferences.edit()
                    .putString(LAST_REFRESH_SHOW_STATS_KEY, OffsetDateTime.now().toString())
                    .apply()


                return Resource.Success(true)
            } catch (t: Throwable) {
                return Resource.Error(t, null)
            }
        }
        return Resource.Success(true)

    }

    suspend fun refreshMovieStats(shouldRefresh: Boolean): Resource<Boolean> {

        val lastRefreshedMovieStats =
            sharedPreferences.getString(LAST_REFRESH_MOVIE_STATS_KEY, null).let { timestamp ->
                if (timestamp != null) {
                    OffsetDateTime.parse(timestamp)
                } else {
                    null
                }
            }

        if (shouldRefresh || lastRefreshedMovieStats?.isBefore(
                OffsetDateTime.now().minusHours(
                    REFRESH_INTERVAL
                )
            ) == true
        ) {
            if (shouldRefresh) {
                Log.d(TAG, "refreshMovieStats: Refresh Force")
            } else if (!shouldRefresh && lastRefreshedMovieStats?.isBefore(
                    OffsetDateTime.now().minusHours(
                        REFRESH_INTERVAL
                    )
                ) == true
            ) {
                Log.d(TAG, "refreshMovieStats: Refresh scheduled")
            }

            try {

                refreshMovieWatchedStats()
                refreshCollectedMovieStats()
                refreshMovieRatingsStats()

                sharedPreferences.edit()
                    .putString(LAST_REFRESH_MOVIE_STATS_KEY, OffsetDateTime.now().toString())
                    .apply()



                return Resource.Success(true)
            } catch (t: Throwable) {
                return Resource.Error(t, null)
            }
        }
        return Resource.Success(false)

    }

    suspend fun getCollectedMovieStatsById(traktId: Int): Flow<CollectedMoviesStats?> {
        return collectedMoviesStatsDao.getCollectedMovieStatsById(traktId)
    }

    suspend fun refreshMovieWatchedStats() {
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

    suspend fun refreshMovieRatingsStats() {
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

    suspend fun refreshWatchedShows() {
        val watchedShowStatsResponse = traktApi.tmUsers().watchedShows(
            UserSlug(
                sharedPreferences.getString(
                    AuthActivity.USER_SLUG_KEY,
                    "NULL"
                )
            ), null
        )

        insertWatchedShowsStats(watchedShowStatsResponse)

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

    suspend fun refreshShowsRatings() {
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

    suspend fun refreshEpisodeRatings() {
        val episodeRatingsResponse = traktApi.tmUsers().ratingsEpisodes(
            UserSlug(
                sharedPreferences.getString(
                    AuthActivity.USER_SLUG_KEY,
                    "NULL"
                )
            ), RatingsFilter.ALL, null
        )

        insertRatedEpisodesStats(episodeRatingsResponse)
    }


    private suspend fun insertCollectedMoviesStats(movies: List<BaseMovie>) {
        Log.d(TAG, "insertCollectedMoviesStats: Refreshing")

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

        Log.d(TAG, "insertCollectedMoviesStats: Inserting ${collectedMovieStatsList.size}")


        moviesDatabase.withTransaction {
            collectedMoviesStatsDao.deleteCollectedStats()
            collectedMoviesStatsDao.insert(collectedMovieStatsList)
        }
    }

    private suspend fun insertWatchedMoviesStats(movies: List<BaseMovie>) {
        Log.d(TAG, "insertWatchedMoviesStats: Refreshing")
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

        Log.d(TAG, "insertWatchedMoviesStats: Inserting ${watchedMoviesStatsList.size}")

        moviesDatabase.withTransaction {
            watchedMoviesStatsDao.deleteWatchedStats()
            watchedMoviesStatsDao.insert(watchedMoviesStatsList)
        }
    }

    private suspend fun insertRatedMoviesStats(ratedMovies: List<RatedMovie>) {
        Log.d(TAG, "insertRatedMoviesStats: Inserting RatedMovies Stats")
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

        Log.d(TAG, "insertRatedMoviesStats: Inserting ${ratedMoviesStatsList.size} ratings")

        moviesDatabase.withTransaction {
            ratingsMoviesStatsDao.deleteRatingsStats()
            ratingsMoviesStatsDao.insert(ratedMoviesStatsList)
        }
    }

    private suspend fun insertCollectedShowsStats(shows: List<BaseShow>) {
        Log.d(TAG, "insertCollectedShowsStats: Refreshing")

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

        Log.d(TAG, "insertCollectedShowsStats: Inserting ${collectedShowsStatsList.size}")


        showsDatabase.withTransaction {
            collectedShowsStatsDao.deleteCollectedStats()
            collectedShowsStatsDao.insert(collectedShowsStatsList)
        }

    }

    private suspend fun insertWatchedShowsStats(watchedShows: List<BaseShow>) {
        Log.d(TAG, "insertWatchedShowsStats: Refreshing")

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

        Log.d(TAG, "insertWatchedShowsStats: Inserting ${watchedShowsStatsList.size}")


        showsDatabase.withTransaction {
            watchedShowsStatsDao.deleteWatchedStats()
            watchedShowsStatsDao.insert(watchedShowsStatsList)
        }
    }

    private suspend fun insertWatchedSeasonsStats(showTraktId: Int, baseShow: BaseShow) {
        Log.d(TAG, "insertWatchedSeasonsStats: Refreshing")

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
        Log.d(TAG, "insertWatchedEpisodesStats: Refreshing")

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
        Log.d(TAG, "insertRatedShowsStats: Inserting ratedShows Stats")
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

        Log.d(TAG, "insertRatedShowsStats: Inserting ${ratedShowsStatsList.size} ratings")

        showsDatabase.withTransaction {
            ratingsShowsStatsDao.deleteRatingsStats()
            ratingsShowsStatsDao.insert(ratedShowsStatsList)
        }
    }

    private suspend fun insertRatedEpisodesStats(ratedEpisodes: List<RatedEpisode>) {
        Log.d(TAG, "insertRatedEpisodesStats: Inserting ratedEpisodes Stats")
        val ratedEpisodeStatsList: MutableList<RatingsEpisodesStats> = mutableListOf()

        ratedEpisodes.forEach { ratedEpisode ->
            ratedEpisodeStatsList.add(
                RatingsEpisodesStats(
                    ratedEpisode.episode?.ids?.trakt ?: 0,
                    ratedEpisode.show?.ids?.trakt ?: 0,
                    ratedEpisode.show?.ids?.tmdb ?: 0,
                    ratedEpisode.episode?.season ?: 0,
                    ratedEpisode.episode?.number ?: 0,
                    ratedEpisode.rating?.value ?: 0,
                    ratedEpisode.episode?.title ?: "",
                    ratedEpisode.show?.title ?: "",
                    ratedEpisode.rated_at
                )
            )
        }

        Log.d(TAG, "insertRatedEpisodesStats: Inserting ${ratedEpisodeStatsList.size} ratings")

        showsDatabase.withTransaction {
            ratingsEpisodesStatsDao.deleteRatingsStats()
            ratingsEpisodesStatsDao.insert(ratedEpisodeStatsList)
        }

    }

    companion object {
        const val LAST_REFRESH_MOVIE_STATS_KEY = "last_refreshed_movies_stats_key"
        const val LAST_REFRESH_SHOW_STATS_KEY = "last_refreshed_shows_stats_key"
        const val LAST_REFRESH_SEASON_PROGRESS_STATS_KEY = "last_refresh_season_progress_key"
    }
}