package com.nickrankin.traktapp.helper

import android.util.Log
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.dao.show.model.TmSeason
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.TvEpisode
import com.uwetrottmann.tmdb2.entities.TvShow
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem
import com.uwetrottmann.trakt5.entities.Episode
import com.uwetrottmann.trakt5.entities.Season
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.enums.Extended
import org.threeten.bp.DateTimeUtils
import retrofit2.HttpException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Exception

private const val TAG = "ShowDataHelper"

/***
 *
 * Helper class to load Show Data from TMDB using Trakt API as a fallback.
 * Exceptions calling Trakt endpoints to be caught in Repository layer (networkBoundResource). Exceptions thrown by TMDB API will force fallback to Trakt information provider.
 *
 * **/
@Singleton
class ShowDataHelper @Inject constructor(
    private val traktApi: TraktApi,
    private val tmdbApi: TmdbApi
) {
    suspend fun getShow(
        traktId: Int
    ): TmShow? {
        var tmShow: TmShow? = null

        val traktShowResponse = traktApi.tmShows().summary(traktId.toString(), Extended.FULL)

        val showTmdbId = traktShowResponse.ids?.tmdb ?: -1

        if (showTmdbId != -1) {
            // We can get Show using Tmdb Id
            // Catch any errors that happen here, revert back to Trakt for data if errors occur using TMDB API.
            try {
                val response = tmdbApi.tmTvService().tv(
                    showTmdbId,
                    null,
                    AppendToResponse(
                        AppendToResponseItem.CREDITS,
                        AppendToResponseItem.TV_CREDITS,
                        AppendToResponseItem.EXTERNAL_IDS,
                        AppendToResponseItem.VIDEOS
                    )
                )
                return convertTmdbShow(traktShowResponse, response)
            } catch(e: HttpException) {
                Log.e(TAG, "getShow: HttpException occurred. Code: ${e.code()}. ${e.message()}", )
                return convertTraktShow(traktShowResponse)

            }catch(e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "getShow: Error getting TMDB Data", )
                return convertTraktShow(traktShowResponse)
            }

        } else {
            // Try to find TV Show on TMDB
            tmShow = findTmdbShow(traktShowResponse)

            // If TmShow still null, fallback to Trakt Data
            return tmShow ?: convertTraktShow(traktShowResponse)
        }
    }

    suspend fun findTmdbShow(traktShow: Show): TmShow? {
        val findShowResponse =
            tmdbApi.tmSearchService().tv(traktShow.title, 1, getTmdbLanguage(traktShow.language), traktShow.year, false)

        if (findShowResponse.results?.isNotEmpty() == true) {
            // Base TV Show gets us Tmdb ID
            val foundShow = findShowResponse.results!!.first()

            // Get full show Data
            val tvShow = tmdbApi.tmTvService().tv(
                foundShow?.id ?: -1, getTmdbLanguage(foundShow.original_language),
                AppendToResponse(
                    AppendToResponseItem.CREDITS,
                    AppendToResponseItem.TV_CREDITS,
                    AppendToResponseItem.EXTERNAL_IDS,
                    AppendToResponseItem.VIDEOS
                )
            )

            Log.d(TAG, "findTmdbShow: Found show $tvShow")

            return convertTmdbShow(traktShow, tvShow)
        } else {
            return null
        }
    }

    suspend fun getSeasons(
        showTraktId: Int,
        showTmdbId: Int?,
        language: String?
    ): List<TmSeason> {
        val tmSeasons: MutableList<TmSeason> = mutableListOf()

        val traktSeasons = traktApi.tmSeasons().summary(showTraktId.toString(), Extended.FULL)

        if (traktSeasons.isEmpty()) {
            Log.e(TAG, "getSeasons: Couldn't get Seasons from Trakt")
            return emptyList()
        }

        if (showTmdbId != null && showTmdbId != 0) {
            Log.d(TAG, "getSeasons: Getting Seasons from TMDB. TMDB ID: $showTmdbId. Trakt ID $showTraktId")
            // Catch errors from TMDB API and revert to Trakt as fallback
            try {
                val response = tmdbApi.tmTvService().tv(showTmdbId, language)

                traktSeasons.map { traktSeason ->

                    // Get matching Tmdb Season object
                    val tvSeason = response.seasons?.find { tvSeason ->
                        tvSeason.id == traktSeason.ids?.tmdb ?: -1
                    }

                    if (traktSeason.ids?.tmdb ?: -1 != -1) {
                        // Use TMDB for data
                        tmSeasons.add(
                            TmSeason(
                                traktSeason.ids?.trakt ?: 0,
                                null,
                                tvSeason?.id ?: 0,
                                showTmdbId,
                                showTraktId,
                                language,
                                tvSeason?.name ?: "",
                                tvSeason?.overview ?: "",
                                tvSeason?.credits,
                                tvSeason?.external_ids,
                                tvSeason?.images,
                                tvSeason?.videos,
                                tvSeason?.air_date,
                                tvSeason?.episode_count ?: 0,
                                tvSeason?.season_number ?: 0,
                                tvSeason?.poster_path,
                                TmSeason.SOURCE_TMDB
                            )
                        )
                    }
                }
            } catch(e: HttpException) {
                Log.e(TAG, "getSeasons: Error getting Season Data from TMDB. Code ${e.code()}. ${e.message()}", )
                e.printStackTrace()

                tmSeasons.addAll(traktSeasonSource(showTmdbId, showTraktId, language, traktSeasons))
            } catch(e: Exception) {
                Log.e(TAG, "getSeasons: Error getting Tmdb Data.", )
                e.printStackTrace()
                tmSeasons.addAll(traktSeasonSource(showTmdbId, showTraktId, language, traktSeasons))
            }
        } else {
            Log.d(TAG, "getSeasons: Getting Seasons from Trakt. TMDB ID: $showTmdbId. Trakt ID $showTraktId")
            // Fallback to Trakt Data ..
            tmSeasons.addAll(traktSeasonSource(showTmdbId, showTraktId, language, traktSeasons))
        }

        Log.d(TAG, "getSeasons: Returning ${tmSeasons.size} seasons")
        return tmSeasons
    }

    private fun traktSeasonSource(showTmdbId: Int?, showTraktId: Int, language: String?, traktSeasons: List<Season>): List<TmSeason> {
        val tmSeasons: MutableList<TmSeason> = mutableListOf()
        traktSeasons.map { tvSeason ->

            var airedDate: Date? = null

            if(tvSeason.first_aired != null) {
                airedDate = DateTimeUtils.toDate(tvSeason.first_aired?.toInstant())
            }

            tmSeasons.add(
                TmSeason(
                    tvSeason.ids?.trakt ?: 0,
                    tvSeason.ids?.trakt ?: 0,
                    null,
                    showTmdbId,
                    showTraktId,
                    language,
                    tvSeason.title ?: "",
                    tvSeason.overview ?: "",
                    null,
                    null,
                    null,
                    null,
                    airedDate,
                    tvSeason.episode_count ?: 0,
                    tvSeason.number ?: 0,
                    null,
                    TmSeason.SOURCE_TRAKT
                )
            )
        }
        return tmSeasons
    }

    suspend fun getSeasonEpisodesData(
        showTraktId: Int,
        showTmdbId: Int?,
        seasonNumber: Int,
        language: String?
    ): List<TmEpisode> {

        Log.d(
            TAG,
            "getSeasonEpisodesData: Getting Season Episodes data (Trakt ID $showTraktId, TMDB ID: $showTmdbId, Number: $seasonNumber)"
        )

        var episodes: MutableList<TmEpisode> = mutableListOf()

        val traktSeasonEpisodes =
            traktApi.tmSeasons().season(showTraktId.toString(), seasonNumber, Extended.FULLEPISODES)

        if (showTmdbId != -1 && showTmdbId != 0) {
            Log.d(
                TAG,
                "getSeasonEpisodesData: Using TMDB Source (Trakt ID $showTraktId, TMDB ID: $showTmdbId, Number: $seasonNumber)"
            )
            // Catch errors from TMDB API and revert to Trakt as fallback
            try {
                val tmdbSeasonEpisodes = tmdbApi.tmTvSeasonService()
                    .season(showTmdbId ?: -1, seasonNumber, language).episodes

                Log.d(TAG, "getSeasonEpisodesData: Got ${tmdbSeasonEpisodes.size} Episodes")

                traktSeasonEpisodes.map { traktEpisode ->
                    val tmdbEpisode = tmdbSeasonEpisodes.find { tmdbEpisode ->
                        traktEpisode.number == tmdbEpisode.episode_number && traktEpisode.season == tmdbEpisode.season_number
                    }

                    episodes.add(
                        TmEpisode(
                            tmdbEpisode?.id ?: 0,
                            traktEpisode.ids?.trakt ?: 0,
                            tmdbEpisode?.id ?: 0,
                            showTmdbId,
                            showTraktId,
                            language,
                            tmdbEpisode?.season_number ?: 0,
                            tmdbEpisode?.episode_number ?: 0,
                            tmdbEpisode?.production_code,
                            tmdbEpisode?.name ?: "",
                            tmdbEpisode?.overview,
                            tmdbEpisode?.air_date,
                            tmdbEpisode?.credits,
                            tmdbEpisode?.crew ?: emptyList(),
                            tmdbEpisode?.guest_stars ?: emptyList(),
                            tmdbEpisode?.images,
                            tmdbEpisode?.external_ids,
                            tmdbEpisode?.still_path,
                            tmdbEpisode?.videos,
                            null,
                            TmEpisode.SOURCE_TMDB
                        )
                    )
                }
            } catch(e: HttpException) {
                Log.e(TAG, "getSeasonEpisodesData: HttpException Error getting episode data from Tmdb. Code: ${e.code()}. ${e.message()}", )
                e.printStackTrace()
                episodes.addAll(traktEpisodeSource(showTmdbId, showTraktId, language, traktSeasonEpisodes))
            }catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "getSeasonEpisodesData: Default to Trakt for Episode data")
                traktEpisodeSource(showTmdbId, showTraktId, language, traktSeasonEpisodes)

                //Trakt data source
                episodes.addAll(
                    traktEpisodeSource(
                        showTmdbId,
                        showTraktId,
                        language,
                        traktSeasonEpisodes
                    )
                )
                Log.d(TAG, "getSeasonEpisodesData: Returning ${episodes.size} episodes")
            }
        } else {
            Log.d(
                TAG,
                "getSeasonEpisodesData: Using Trakt Source (Trakt ID $showTraktId, Number: $seasonNumber)"
            )

            Log.d(TAG, "getSeasonEpisodesData: Got ${traktSeasonEpisodes.size} Episodes")

            //Trakt data source
            episodes.addAll(
                traktEpisodeSource(
                    showTmdbId,
                    showTraktId,
                    language,
                    traktSeasonEpisodes
                )
            )
            Log.d(TAG, "getSeasonEpisodesData: Returning ${episodes.size} episodes")

        }
        return episodes
    }

    private fun traktEpisodeSource(
        showTmdbId: Int?,
        showTraktId: Int,
        language: String?,
        traktSeasonEpisodes: List<Episode>
    ): List<TmEpisode> {
        Log.d(TAG, "traktEpisodeSource: Getting Trakt Episode Data", )
        var episodes: MutableList<TmEpisode> = mutableListOf()

        traktSeasonEpisodes.map { traktEpisode ->

            var airedDate: Date? = null

            if(traktEpisode.first_aired != null) {
                airedDate = DateTimeUtils.toDate(traktEpisode.first_aired?.toInstant())
            }

            episodes.add(
                TmEpisode(
                    traktEpisode.ids?.trakt ?: 0,
                    traktEpisode.ids?.trakt ?: 0,
                    null,
                    showTmdbId,
                    showTraktId,
                    language,
                    traktEpisode.season ?: 0,
                    traktEpisode.number ?: 0,
                    null,
                    traktEpisode.title ?: "",
                    traktEpisode.overview,
                    airedDate,
                    null,
                    emptyList(),
                    emptyList(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    TmEpisode.SOURCE_TRAKT
                )
            )
        }
        return episodes
    }

    private fun convertTmdbShow(traktShow: Show, tmdbShow: TvShow?): TmShow? {
        if (tmdbShow == null) {
            return null
        }

        val traktId = traktShow.ids?.trakt ?: -1


        return TmShow(
            traktId,
            traktId,
            tmdbShow.id ?: 0,
            tmdbShow.name ?: "",
            tmdbShow.overview ?: "",
            tmdbShow.origin_country ?: emptyList(),
            tmdbShow.created_by ?: emptyList(),
            tmdbShow.external_ids,
            tmdbShow.genres,
            tmdbShow.homepage,
            tmdbShow.images,
            tmdbShow.in_production,
            tmdbShow.languages ?: emptyList(),
            tmdbShow.first_air_date,
            tmdbShow.last_air_date,
            tmdbShow.last_episode_to_air,
            tmdbShow.networks,
            tmdbShow.next_episode_to_air,
            tmdbShow.number_of_episodes ?: 0,
            tmdbShow.number_of_seasons ?: 0,
            traktShow.runtime,
            tmdbShow.status ?: "",
            tmdbShow.poster_path,
            tmdbShow.backdrop_path,
            tmdbShow.type,
            tmdbShow.videos,
            false,
            TmShow.SOURCE_TMDB
        )
    }

    /**
     *
     * Fallback method to use Trakt' own data and get the season data from Trakt API
     *
     * **/
    private suspend fun convertTraktShow(show: Show): TmShow {
        Log.d(TAG, "convertTraktShowToTmShow: Falling back to Trakt metadata")

        var airedDate: Date? = null

        if(show.first_aired != null) {
            airedDate = DateTimeUtils.toDate(show.first_aired?.toInstant())
        }

        return TmShow(
            show.ids?.trakt ?: -1,
            show.ids?.trakt ?: -1,
            null,
            show.title ?: "Unknown",
            show.overview ?: "",
            emptyList(),
            emptyList(),
            null,
            emptyList(),
            null,
            null,
            null,
            emptyList(),
            airedDate,
            null,
            null,
            emptyList(),
            null,
            null,
            null,
            show.runtime,
            null,
            null,
            null,
            null,
            null,
            false,
            TmShow.SOURCE_TRAKT
        )
    }
}
