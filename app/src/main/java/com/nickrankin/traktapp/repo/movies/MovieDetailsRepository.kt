package com.nickrankin.traktapp.repo.movies

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.history.model.HistoryEntry
import com.nickrankin.traktapp.dao.history.model.MovieWatchedHistoryEntry
import com.nickrankin.traktapp.dao.movies.MoviesDatabase
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.VideoService
import com.nickrankin.traktapp.repo.movies.watched.WatchedMoviesRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.HistoryType
import org.threeten.bp.OffsetDateTime
import java.util.Locale
import javax.inject.Inject

private const val REFRESH_PLAY_COUNT = 24L
private const val TAG = "MovieDetailsRepository"

class MovieDetailsRepository @Inject constructor(
    private val movieDataHelper: MovieDataHelper,
    private val traktApi: TraktApi,
    private val tmdbApi: TmdbApi,
    private val sharedPreferences: SharedPreferences,
    private val moviesDatabase: MoviesDatabase) {
    private val tmMovieDao = moviesDatabase.tmMovieDao()
    private val watchedMoviesDao = moviesDatabase.watchedMoviesDao()
    private val movieWatchedHistoryEntryDao = moviesDatabase.movieWatchedHistoryEntryDao()

    fun getMovieSummary(traktId: Int, shouldRefresh: Boolean) = networkBoundResource(
        query = {
            tmMovieDao.getMovieById(traktId)
        },
        fetch = {
            movieDataHelper.getMovieSummary(traktId)
        },
        shouldFetch = { tmMovie ->
            shouldRefresh || tmMovie == null
        },
        saveFetchResult = { tmMovie ->
            moviesDatabase.withTransaction {
                tmMovieDao.insert(tmMovie)
            }

        }
    )

    suspend fun getVideoStreamingServices(tmdbId: Int?, title: String?): List<VideoService> {

        if (tmdbId == null || title == null) {
            return emptyList()
        }

        val locale = Locale.getDefault().country
        Log.e(TAG, "getVideoStreamingServices: Locale: $locale")
        val videos: MutableList<VideoService> = mutableListOf()

        try {
            val videoServiceResponse = tmdbApi.tmMovieService().watchProviders(tmdbId)

            // Services available to this userw Locale setting
            val availableVideoServices = videoServiceResponse.results.filterKeys { it == locale }

            availableVideoServices.entries.map { entry ->

                entry.value.buy.map { paidEntry ->
                    videos.add(
                        VideoService(
                            tmdbId,
                            title,
                            paidEntry.provider_id,
                            paidEntry.provider_name,
                            paidEntry.display_priority,
                            VideoService.TYPE_BUY
                        )
                    )
                }

                Log.e(TAG, "getVideoStreamingServices:setVideoServices ${entry.value.flatrate}")

                entry.value.flatrate.map { prepaidEntry ->
                    videos.add(
                        VideoService(
                            tmdbId,
                            title,
                            prepaidEntry.provider_id,
                            prepaidEntry.provider_name,
                            prepaidEntry.display_priority,
                            VideoService.TYPE_STREAM
                        )
                    )
                }
            }

            return videos

        } catch (e: Exception) {
            Log.e(TAG, "getVideoStreamingServices: Error getting videos ${e.message}")
        }


        return emptyList()
    }

    companion object {
        const val MOVIE_TRAKT_ID_KEY = "movie_trakt_id_key"
        const val MOVIE_TITLE_KEY = "movie_title_key"
        const val USER_RATINGS_KEY = "movie_trakt_user_ratings_key"
    }
}

