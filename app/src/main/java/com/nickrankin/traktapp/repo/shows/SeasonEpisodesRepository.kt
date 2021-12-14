package com.nickrankin.traktapp.repo.shows

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.dao.show.model.TmSeason
import com.nickrankin.traktapp.helper.getTmdbLanguage
import com.nickrankin.traktapp.helper.networkBoundResource
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.TvEpisode
import com.uwetrottmann.tmdb2.entities.TvSeason
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

private const val TAG = "SeasonEpisodesRepositor"
class SeasonEpisodesRepository @Inject constructor(
    private val tmdbApi: TmdbApi,
    private val showsDatabase: ShowsDatabase,
    private val sharedPreferences: SharedPreferences
) {
    private val seasonDao = showsDatabase.TmSeasonsDao()
    private val episodesDao = showsDatabase.TmEpisodesDao()

    fun getSeason(showTmdbId: Int,
                          seasonNumber: Int) = seasonDao.getSeason(showTmdbId, seasonNumber)

    suspend fun getSeasonEpisodes(
        showTraktId: Int,
        showTmdbId: Int,
        seasonNumber: Int,
        language: String?,
        shouldRefresh: Boolean
    ) = networkBoundResource(
        query = {
            episodesDao.getEpisodes(showTmdbId, seasonNumber)
        },
        fetch = {
            tmdbApi.tmTvSeasonService().season(
                showTmdbId,
                seasonNumber,
                getTmdbLanguage(language),
                AppendToResponse(AppendToResponseItem.CREDITS)
            )
        },
        shouldFetch = { episodes ->
            shouldRefresh || episodes.isEmpty()
        },
        saveFetchResult = { tvSeason ->

            showsDatabase.withTransaction {
                episodesDao.deleteEpisodes(showTmdbId, seasonNumber)

                seasonDao.insertSeasons(listOf(convertSeason(showTraktId, showTmdbId, language, tvSeason)))
                episodesDao.insert(convertEpisodes(showTraktId, showTmdbId, language, tvSeason.episodes ?: emptyList()))
            }

        }
    )

    private fun convertSeason(showTraktId: Int, showTmdbId: Int, language: String?, tvSeason: TvSeason): TmSeason {

        return TmSeason(
            tvSeason.id ?: 0,
            showTmdbId,
            showTraktId,
            language,
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
    }

    private fun convertEpisodes(showTraktId: Int, showTmdbId: Int, language: String?, episodes: List<TvEpisode>): List<TmEpisode> {
        val tmEpisodes: MutableList<TmEpisode> = mutableListOf()

        episodes.map { tvEpisode ->
            tmEpisodes.add(
                TmEpisode(
                    tvEpisode.id ?: 0,
                    showTmdbId,
                    showTraktId,
                    language,
                    tvEpisode.season_number ?: 0,
                    tvEpisode.episode_number ?: 0,
                    tvEpisode.production_code,
                    tvEpisode.name ?: "",
                    tvEpisode.overview,
                    tvEpisode.air_date,
                    tvEpisode.credits,
                    tvEpisode.crew ?: emptyList(),
                    tvEpisode.guest_stars ?: emptyList(),
                    tvEpisode.images,
                    tvEpisode.external_ids,
                    tvEpisode.still_path,
                    tvEpisode.videos
                )
            )
        }

        return tmEpisodes
    }

    companion object {
        const val SHOW_TRAKT_ID_KEY = "show_trakt_id"
        const val SHOW_TMDB_ID_KEY = "show_tmdb_id"
        const val SEASON_NUMBER_KEY = "season_number"
        const val LANGUAGE_KEY = "language"

        const val LAST_FULL_REFRESH_KEY = "last_full_episodes_refresh"
    }
}