package com.nickrankin.traktapp.repo.shows

import android.content.SharedPreferences
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.TmSeason
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.helper.networkBoundResource
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.TvSeason
import com.uwetrottmann.tmdb2.entities.TvShow
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem
import com.uwetrottmann.trakt5.enums.ProgressLastActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.lang.Exception
import javax.inject.Inject

class ShowDetailsRepository @Inject constructor(private val traktApi: TraktApi, private val tmdbApi: TmdbApi, private val sharedPreferences: SharedPreferences, private val showsDatabase: ShowsDatabase) {
    private val tmShowDao = showsDatabase.tmShowDao()
    private val tmSeasonsDao = showsDatabase.TmSeasonsDao()


    suspend fun getShowSummary(showTmdbId: Int, language: String, shouldRefresh: Boolean) = networkBoundResource(
        query = {
                tmShowDao.getShow(showTmdbId)
        },
        fetch = {
                tmdbApi.tmTvService().tv(showTmdbId, language+",null", AppendToResponse(AppendToResponseItem.CREDITS, AppendToResponseItem.TV_CREDITS, AppendToResponseItem.EXTERNAL_IDS, AppendToResponseItem.VIDEOS))
        },
        shouldFetch = { tmShow ->
            tmShow == null || shouldRefresh
        },
        saveFetchResult = { tvShow ->
            showsDatabase.withTransaction {
                tmSeasonsDao.deleteAllSeasonsForShow(showTmdbId)

                tmShowDao.insertShow(convertShow(tvShow))
                tmSeasonsDao.insertSeasons(convertSeasons(showTmdbId, tvShow.seasons ?: emptyList()))
            }
        }
    )

    fun getSeasons(showTmdbId: Int): Flow<List<TmSeason>> {
        return tmSeasonsDao.getSeasonsForShow(showTmdbId)
    }

    private fun convertShow(tvShow: TvShow): TmShow {
        return TmShow(
            tvShow.id ?: 0,
            tvShow.name ?: "",
            tvShow.overview ?: "",
            tvShow.origin_country?.first() ?: "",
            tvShow.credits,
            tvShow.external_ids,
            tvShow.genres,
            tvShow.homepage,
            tvShow.images,
            tvShow.in_production,
            tvShow.languages ?: emptyList(),
            tvShow.first_air_date,
            tvShow.last_air_date,
            tvShow.last_episode_to_air,
            tvShow.networks,
            tvShow.next_episode_to_air,
            tvShow.number_of_episodes ?: 0,
            tvShow.number_of_seasons ?: 0,
            tvShow.status ?: "",
            tvShow.poster_path,
            tvShow.backdrop_path,
            tvShow.type,
            tvShow.videos
        )
    }

    private fun convertSeasons(showTmdbId: Int, seasons: List<TvSeason>): List<TmSeason> {
        val tmSeasons: MutableList<TmSeason> = mutableListOf()

        seasons.map { tvSeason ->
            tmSeasons.add(
                TmSeason(
                    tvSeason.id ?: 0,
                    showTmdbId,
                    tvSeason.name ?: "",
                    tvSeason.overview ?: "",
                    tvSeason.credits,
                    tvSeason.external_ids,
                    tvSeason.images,
                    tvSeason.videos,
                    tvSeason.air_date,
                    tvSeason.episode_count ?: 0,
                    tvSeason.season_number ?: 0,
                    tvSeason.poster_path
                )
            )
        }

        return tmSeasons
    }

    fun getShowProgress(showTraktId: Int) = flow {
        try {
            val progressCall = traktApi.tmShows().watchedProgress(showTraktId.toString(), false, false, false, ProgressLastActivity.WATCHED, null)

            emit(progressCall)
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val SHOW_TRAKT_ID_KEY = "show_trakt_id"
        const val SHOW_TMDB_ID_KEY = "show_tmdb_id"
        const val SHOW_LANGUAGE_KEY = "show_language"
    }
}