package com.nickrankin.traktapp.repo.movies

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.api.services.trakt.model.stats.RatingStats
import com.nickrankin.traktapp.dao.base_entity.EpisodeBaseEnity
import com.nickrankin.traktapp.dao.base_entity.MovieBaseEntity
import com.nickrankin.traktapp.dao.base_entity.PersonBaseEntity
import com.nickrankin.traktapp.dao.base_entity.ShowBaseEntity
import com.nickrankin.traktapp.dao.history.model.MovieWatchedHistoryEntry
import com.nickrankin.traktapp.dao.lists.TraktListsDatabase
import com.nickrankin.traktapp.dao.lists.model.ListEntry
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.dao.lists.model.TraktListEntry
import com.nickrankin.traktapp.dao.movies.MoviesDatabase
import com.nickrankin.traktapp.dao.movies.model.CollectedMovie
import com.nickrankin.traktapp.dao.movies.model.WatchedMovieAndStats
import com.nickrankin.traktapp.dao.refresh.LastRefreshedAt
import com.nickrankin.traktapp.dao.refresh.RefreshType
import com.nickrankin.traktapp.dao.stats.model.CollectedStats
import com.nickrankin.traktapp.dao.stats.model.MoviesCollectedStats
import com.nickrankin.traktapp.dao.stats.model.RatingsMoviesStats
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.helper.shouldRefresh
import com.nickrankin.traktapp.repo.IActionButtons
import com.nickrankin.traktapp.repo.lists.ListsRepository
import com.nickrankin.traktapp.repo.movies.watched.WatchedMoviesRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.*
import kotlinx.coroutines.flow.*
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

private const val REFRESH_PLAY_COUNT = 24L
private const val TAG = "MovieActionButtonsRepos"

class MovieActionButtonsRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val listsRepository: ListsRepository,
    private val sharedPreferences: SharedPreferences,
    private val moviesDatabase: MoviesDatabase) :  IActionButtons<MovieWatchedHistoryEntry> {

    private val movieRatingStatsDao = moviesDatabase.ratedMoviesStatsDao()
    private val movieCollectedStatsDao = moviesDatabase.collectedMoviesStatsDao()

    private val watchedMoviesDao = moviesDatabase.watchedMoviesDao()
    private val movieWatchedHistoryEntryDao = moviesDatabase.movieWatchedHistoryEntryDao()

    private val collectedMoviesDao = moviesDatabase.collectedMovieDao()

    private val lastRefreshedAtDao = moviesDatabase.lastRefreshAtDao()



    private val userSlug = UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "NULL"))

    override suspend fun getRatings(traktId: Int, shouldFetch: Boolean): Flow<Resource<com.nickrankin.traktapp.dao.stats.model.RatingStats?>> = networkBoundResource(
        query = {
            movieRatingStatsDao.getRatingsStatsPerMovie(traktId)
        },
        fetch = {
            traktApi.tmUsers().ratingsMovies(userSlug, RatingsFilter.ALL, null)
        },
        shouldFetch = { rating ->
            shouldFetch || shouldRefresh(lastRefreshedAtDao.getLastRefreshed(RefreshType.RATED_MOVIES).first(), null) ?: true
        },
        saveFetchResult = { ratedMovies ->
            moviesDatabase.withTransaction {
                movieRatingStatsDao.deleteRatingsStats()
            }

            ratedMovies.map { ratedMovie ->
                moviesDatabase.withTransaction {
                    movieRatingStatsDao.insert(
                        RatingsMoviesStats(
                            ratedMovie.movie?.ids?.trakt ?: 0,
                            ratedMovie.rating?.value ?: 0,
                            ratedMovie.rated_at ?: OffsetDateTime.now()
                        )
                    )
                }
            }

            moviesDatabase.withTransaction {
                lastRefreshedAtDao.insertLastRefreshStats(LastRefreshedAt(RefreshType.RATED_MOVIES, OffsetDateTime.now()))
            }
        }
    )

    override suspend fun getTraktListsAndItems(shouldFetch: Boolean): Flow<Resource<out List<Pair<TraktList, List<TraktListEntry>>>>> {
        return listsRepository.getTraktListsAndItems(shouldFetch)
    }

    override suspend fun addToList(itemTraktId: Int, listTraktId: Int): Resource<SyncResponse> {
        return listsRepository.addToList(itemTraktId, listTraktId, Type.MOVIE)
    }

    override suspend fun removeFromList(
        itemTraktId: Int,
        listTraktId: Int
    ): Resource<SyncResponse> {
        return listsRepository.removeFromList(itemTraktId, listTraktId, Type.MOVIE)
    }

    override suspend fun getCollectedStats(
        traktId: Int,
        shouldFetch: Boolean
    ): Flow<Resource<CollectedStats?>> = networkBoundResource(
        query = {
            movieCollectedStatsDao.getCollectedMovieStatsById(traktId)
        },
        fetch = {
            traktApi.tmUsers().collectionMovies(userSlug, null)
        },
        shouldFetch = { collectionStats ->
           shouldFetch || shouldRefresh(lastRefreshedAtDao.getLastRefreshed(RefreshType.COLLECTED_MOVIE_STATS).first(), null)
        },
        saveFetchResult = { baseMovie ->
            val collectionStatsList: MutableList<MoviesCollectedStats> = mutableListOf()

            baseMovie.map { collectedMovie ->
                collectionStatsList.add(
                    MoviesCollectedStats(
                        collectedMovie.movie?.ids?.trakt ?: 0,
                        collectedMovie.movie?.ids?.tmdb,
                        collectedMovie.collected_at ?: OffsetDateTime.now(),
                        collectedMovie.movie?.title ?: "",
                        collectedMovie.listed_at
                    )
                )
            }

            moviesDatabase.withTransaction {
                lastRefreshedAtDao.insertLastRefreshStats(
                    LastRefreshedAt(
                        RefreshType.COLLECTED_MOVIE_STATS,
                        OffsetDateTime.now()
                    )
                )
            }
        }
    )

    override suspend fun getPlaybackHistory(traktId: Int, shouldFetch: Boolean) =
        networkBoundResource(
            query = {
                movieWatchedHistoryEntryDao.getWatchedHistoryForMovie(traktId)
            },
            fetch = {
                traktApi.tmUsers().history(
                    userSlug,
                    HistoryType.MOVIES,
                    traktId,
                    1,
                    null,
                    null,
                    null,
                    null
                )
            },
            shouldFetch = { watchedHistoryEntries ->
               shouldFetch || shouldRefresh(lastRefreshedAtDao.getLastRefreshed(RefreshType.PLAYBACK_HISORY_MOVIES).first(), null)

            },
            saveFetchResult = { historyEntries ->
                moviesDatabase.withTransaction {
                    movieWatchedHistoryEntryDao.deleteMovieWatchedEntries(traktId)
                    movieWatchedHistoryEntryDao.insert(convertHistoryEntries(historyEntries))
                }

                moviesDatabase.withTransaction {
                    lastRefreshedAtDao.insertLastRefreshStats(
                        LastRefreshedAt(
                            RefreshType.PLAYBACK_HISORY_MOVIES,
                            OffsetDateTime.now()
                        )
                    )
                }
            }
        )

    private fun convertHistoryEntries(historyEntries: List<com.uwetrottmann.trakt5.entities.HistoryEntry>): List<MovieWatchedHistoryEntry> {
        val movieHistoryEntries: MutableList<MovieWatchedHistoryEntry> = mutableListOf()

        historyEntries.map { entry ->
            movieHistoryEntries.add(
                MovieWatchedHistoryEntry(
                    entry.id ?: 0L,
                    entry.movie?.ids?.trakt ?: 0,
                    entry.movie?.ids?.tmdb,
                    entry.movie?.title ?: "",
                    entry.watched_at ?: OffsetDateTime.now(),
                    OffsetDateTime.now()
                )
            )
        }

        return movieHistoryEntries
    }

    override suspend fun checkin(
        traktId: Int,
        overrideActiveCheckins: Boolean
    ): Resource<BaseCheckinResponse> {
        return try {
            if (overrideActiveCheckins) {
                cancelCheckins()
            }

            val movieCheckin = MovieCheckin.Builder(
                SyncMovie().id(MovieIds.trakt(traktId)),
                AppConstants.APP_VERSION,
                AppConstants.APP_DATE
            )
                .build()
            val checkinResponse = traktApi.tmCheckin().checkin(
                movieCheckin
            )

            Resource.Success(checkinResponse)

        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    override suspend fun cancelCheckins(): Resource<Boolean> {
        return try {

            traktApi.tmCheckin().deleteActiveCheckin()
            Resource.Success(true)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    override suspend fun addRating(
        traktId: Int,
        newRating: Int,
        ratedAt: OffsetDateTime
    ): Resource<SyncResponse> {
        val syncItems = SyncItems().apply {
            movies = listOf(
                SyncMovie()
                    .id(MovieIds.trakt(traktId))
                    .rating(Rating.fromValue(newRating))
                    .ratedAt(OffsetDateTime.now())
            )
        }

        return try {
            val response = traktApi.tmSync().addRatings(syncItems)

            insertRating(RatingsMoviesStats(traktId, newRating, OffsetDateTime.now()), response)

            Resource.Success(response)

        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    private suspend fun insertRating(ratingStats: RatingsMoviesStats, syncResponse: SyncResponse) {
        if ((syncResponse.added?.movies ?: 0) <= 0) {
            Log.e(TAG, "insertRating: Error, sync returned 0, returning")

            return
        }
        moviesDatabase.withTransaction {
            movieRatingStatsDao.insert(ratingStats)
        }
    }

    override suspend fun deleteRating(traktId: Int): Resource<SyncResponse> {
        return try {
            val response = traktApi.tmSync().deleteRatings(getSyncItems(traktId))

            deleteRating(traktId, response)

            Resource.Success(response)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    private suspend fun deleteRating(traktId: Int, syncResponse: SyncResponse) {
        if ((syncResponse.deleted?.movies ?: 0) <= 0) {
            Log.e(TAG, "deleteRating: Error, sync returned 0, returning")

            return
        }
        moviesDatabase.withTransaction {
            movieRatingStatsDao.deleteRatingsStatsById(traktId)
        }
    }

    override suspend fun addToHistory(
        traktId: Int,
        watchedAt: OffsetDateTime
    ): Resource<SyncResponse> {
        return try {
            val syncItems = SyncItems().apply {
                movies = listOf(
                    SyncMovie().id(MovieIds.trakt(traktId))
                        .watchedAt(watchedAt)
                )
            }

            val syncResponse = traktApi.tmSync().addItemsToWatchedHistory(syncItems)

            insertHistoryEntries(traktId, syncResponse)

            Resource.Success(syncResponse)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    private suspend fun insertHistoryEntries(traktId: Int, syncResponse: SyncResponse) {
        if ((syncResponse.added?.movies ?: 0) > 0) {
            val historyEntries = getMovieWatchedHistoryEntries(traktId)

            moviesDatabase.withTransaction {

                // We need to get the watched movies again for this movie, so the PagingData can be updated
                watchedMoviesDao.insert(
                    WatchedMoviesRepository.convertHistoryEntries(
                        historyEntries
                    )
                )

                // Need to invalidate the PagingData due to db manipulations
                watchedMoviesDao.getWatchedMovies().invalidate()
            }

            moviesDatabase.withTransaction {
                // Now we update the history entry stats
                movieWatchedHistoryEntryDao.deleteMovieWatchedEntries(traktId)
                movieWatchedHistoryEntryDao.insert(convertHistoryEntries(historyEntries))
            }
        }
    }

    private suspend fun getMovieWatchedHistoryEntries(traktId: Int): List<com.uwetrottmann.trakt5.entities.HistoryEntry> {
        return try {
            val response = traktApi.tmUsers().history(
                userSlug,
                HistoryType.MOVIES,
                traktId,
                1,
                999,
                Extended.FULL,
                null,
                null
            )

            response
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun removeFromHistory(id: Long): Resource<SyncResponse> {
        return try {
            val syncItems = SyncItems().apply {
                ids = listOf(id)
            }

            val response = traktApi.tmSync().deleteItemsFromWatchedHistory(syncItems)

            deleteHistoryEntries(id, response)

            Resource.Success(response)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    private suspend fun deleteHistoryEntries(historyId: Long, syncResponse: SyncResponse) {
        if ((syncResponse.deleted?.movies ?: 0) <= 0) {
            Log.e(TAG, "deleteHistoryEntries: Error, sync returned 0, returning")

            return
        }

        moviesDatabase.withTransaction {
            watchedMoviesDao.deleteMovieById(historyId)
        }

        moviesDatabase.withTransaction {
            movieWatchedHistoryEntryDao.deleteHistoryById(historyId)
        }
    }

    override suspend fun addToCollection(traktId: Int): Resource<SyncResponse> {
        return try {

            val addCollectionReposnse =
                traktApi.tmSync().addItemsToCollection(getSyncItems(traktId))

            insertCollectedMovie(traktId, addCollectionReposnse)

            Resource.Success(addCollectionReposnse)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    private suspend fun insertCollectedMovie(traktId: Int, syncResponse: SyncResponse) {
        if ((syncResponse.added?.movies ?: 0) <= 0) {
            return
        }

        val collectedMovie = traktApi.tmMovies().summary(traktId.toString(), Extended.FULL)

        moviesDatabase.withTransaction {
            collectedMoviesDao.insert(
                CollectedMovie(
                    collectedMovie.ids?.trakt ?: 0,
                    collectedMovie.ids?.tmdb ?: 0,
                    collectedMovie.language,
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    0,
                    collectedMovie.overview,
                    collectedMovie.released,
                    collectedMovie.runtime,
                    collectedMovie.title
                )
            )

            movieCollectedStatsDao.insert(
                MoviesCollectedStats(
                    collectedMovie.ids?.trakt ?: 0,
                    collectedMovie.ids?.tmdb ?: 0,
                    OffsetDateTime.now(),
                    collectedMovie.title,
                    OffsetDateTime.now()
                )
            )
        }
    }

    override suspend fun removeFromCollection(traktId: Int): Resource<SyncResponse> {
        return try {
            val removeFromCollectionResponse =
                traktApi.tmSync().deleteItemsFromCollection(getSyncItems(traktId))

            deleteCollectedMovie(traktId, removeFromCollectionResponse)

            Resource.Success(removeFromCollectionResponse)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    private suspend fun deleteCollectedMovie(traktId: Int, syncResponse: SyncResponse) {
        if ((syncResponse.deleted?.movies ?: 0) <= 0) {
            Log.e(TAG, "deleteCollectedMovie: Error, sync returned 0, returning")
            return
        }
        moviesDatabase.withTransaction {
            collectedMoviesDao.deleteMovieById(traktId)
            movieCollectedStatsDao.deleteCollectedMovieStatById(traktId)
        }
    }

    private fun getSyncItems(traktId: Int): SyncItems {
        return SyncItems().apply {
            movies = listOf(
                SyncMovie().id(MovieIds.trakt(traktId))
            )
        }
    }
}