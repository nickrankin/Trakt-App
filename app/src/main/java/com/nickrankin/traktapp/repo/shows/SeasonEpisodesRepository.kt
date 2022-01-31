package com.nickrankin.traktapp.repo.shows

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.dao.show.model.TmSeason
import com.nickrankin.traktapp.helper.ShowDataHelper
import com.nickrankin.traktapp.helper.getTmdbLanguage
import com.nickrankin.traktapp.helper.networkBoundResource
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.TvEpisode
import com.uwetrottmann.tmdb2.entities.TvSeason
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem
import com.uwetrottmann.trakt5.enums.Extended
import kotlinx.coroutines.flow.first
import java.lang.Exception
import javax.inject.Inject

private const val TAG = "SeasonEpisodesRepositor"

class SeasonEpisodesRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val tmdbApi: TmdbApi,
    private val showDataHelper: ShowDataHelper,
    private val showsDatabase: ShowsDatabase,
    private val sharedPreferences: SharedPreferences
) {
    private val showDao = showsDatabase.tmShowDao()
    private val seasonDao = showsDatabase.TmSeasonsDao()
    private val episodesDao = showsDatabase.TmEpisodesDao()

    fun getSeason(
        showTraktId: Int,
        seasonNumber: Int
    ) = seasonDao.getSeason(showTraktId, seasonNumber)

    suspend fun getSeasonEpisodes(
        showTraktId: Int,
        showTmdbId: Int?,
        seasonNumber: Int,
        shouldRefresh: Boolean
    ) = networkBoundResource(
        query = {
            episodesDao.getEpisodes(showTraktId, seasonNumber)
        },
        fetch = {
            showDataHelper.getSeasonEpisodesData(showTraktId, showTmdbId, seasonNumber, null)
        },
        shouldFetch = { episodes ->
            shouldRefresh || episodes.isEmpty()
        },
        saveFetchResult = { episodes ->


            showsDatabase.withTransaction {
                episodesDao.deleteEpisodes(showTraktId, seasonNumber)

                episodesDao.insert(
                  episodes
                )
            }

        }
    )

    companion object {
        const val SHOW_TRAKT_ID_KEY = "show_trakt_id"
        const val SHOW_TMDB_ID_KEY = "show_tmdb_id"
        const val SEASON_NUMBER_KEY = "season_number"
        const val LANGUAGE_KEY = "language"

        const val LAST_FULL_REFRESH_KEY = "last_full_episodes_refresh"
    }
}