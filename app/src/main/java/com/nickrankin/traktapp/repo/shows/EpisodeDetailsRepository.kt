package com.nickrankin.traktapp.repo.shows

import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.helper.networkBoundResource
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.TvEpisode
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem
import javax.inject.Inject

class EpisodeDetailsRepository @Inject constructor(
    private val tmdbApi: TmdbApi,
    private val traktApi: TraktApi,
    private val showsDatabase: ShowsDatabase
) {
    private val episodesDao = showsDatabase.TmEpisodesDao()

    fun getEpisode(
        showTmdbId: Int,
        seasonNumber: Int,
        episodeNumber: Int,
        language: String,
        shouldRefresh: Boolean
    ) = networkBoundResource(
        query = {
            episodesDao.getEpisode(showTmdbId, seasonNumber, episodeNumber)
        },
        fetch = {
            tmdbApi.tmTvEpisodesService().episode(
                showTmdbId,
                seasonNumber,
                episodeNumber,
                language,
                AppendToResponse(
                    AppendToResponseItem.TV_CREDITS,
                    AppendToResponseItem.EXTERNAL_IDS,
                    AppendToResponseItem.VIDEOS
                )
            )
        },
        shouldFetch = { tmEpisode ->
            shouldRefresh || tmEpisode == null
        },
        saveFetchResult = { tvEpisode ->

            showsDatabase.withTransaction {
               episodesDao.insert(listOf(convertEpisode(showTmdbId, tvEpisode)))
            }

        }
    )

    private fun convertEpisode(
        showTmdbId: Int,
        tvEpisode: TvEpisode
    ): TmEpisode {

        return TmEpisode(
            tvEpisode.id ?: 0,
            showTmdbId,
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
    }

    companion object {
        const val SHOW_TMDB_ID_KEY = "show_tmdb"
        const val SEASON_NUMBER_KEY = "season_number"
        const val EPISODE_NUMBER_KEY = "episode_number"
        const val LANGUAGE_KEY = "language"
    }
}