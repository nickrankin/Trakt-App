package com.nickrankin.traktapp.helper

import android.util.Log
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.api.services.trakt.model.TmCredits
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.dao.show.model.TmSeason
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.TvEpisode
import com.uwetrottmann.tmdb2.entities.TvShow
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem
import com.uwetrottmann.trakt5.entities.Credits
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
private const val TRAKT_PRODUCER = "EXECUTIVE PRODUCER"
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
        var tmShow: TmShow?

        val traktShowResponse = traktApi.tmShows().summary(traktId.toString(), Extended.FULL)
        val traktCreditsResponse = traktApi.tmShows().people(traktId.toString(), null)
        val traktRatings = traktApi.tmShows().ratings(traktId.toString())

        val showTmdbId = traktShowResponse.ids?.tmdb ?: -1

        if (showTmdbId != -1) {
            // We can get Show using Tmdb Id
            // Catch any errors that happen here, revert back to Trakt for data if errors occur using TMDB API.
            try {
                val response = tmdbApi.tmTvService().tv(
                    showTmdbId,
                    null,
                    AppendToResponse(
                        AppendToResponseItem.EXTERNAL_IDS,
                        AppendToResponseItem.VIDEOS
                    )
                )
                return convertTmdbShow(traktShowResponse, traktCreditsResponse, response, traktRatings.rating ?: 0.0)
            } catch(e: HttpException) {
                Log.e(TAG, "getShow: HttpException occurred. Code: ${e.code()}. ${e.message()}")
                return convertTraktShow(traktShowResponse, traktCreditsResponse,traktRatings.rating ?: 0.0)

            }catch(e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "getShow: Error getting TMDB Data")
                return convertTraktShow(traktShowResponse, traktCreditsResponse,traktRatings.rating ?: 0.0)
            }

        } else {
            // Try to find TV Show on TMDB
            tmShow = findTmdbShow(traktShowResponse, traktCreditsResponse,traktRatings.rating ?: 0.0)

            // If TmShow still null, fallback to Trakt Data
            return tmShow ?: convertTraktShow(traktShowResponse, traktCreditsResponse,traktRatings.rating ?: 0.0)
        }
    }

    private fun convertTmdbShow(traktShow: Show, credits: TmCredits, tmdbShow: TvShow?, traktRating: Double): TmShow? {
        if (tmdbShow == null) {
            return null
        }

        val traktId = traktShow.ids?.trakt ?: -1


        return TmShow(
            traktId,
            tmdbShow.id ?: 0,
            traktShow.ids?.imdb,
            traktShow.title ?: "",
            traktShow.overview ?: "",
            traktShow.country,
            traktShow.genres,
            credits.crew?.production?.filter { it.job?.uppercase() == TRAKT_PRODUCER },
            traktShow.homepage,
            traktShow.status,
            traktShow.language,
            traktShow.first_aired,
            traktShow.network,
            tmdbShow.number_of_episodes ?: 0,
            tmdbShow.number_of_seasons ?: 0,
            traktShow.runtime,
            tmdbShow.poster_path,
            tmdbShow.backdrop_path,
            tmdbShow.videos,
            false,
            traktRating)
    }

    /**
     *
     * Fallback method to use Trakt' own data and get the season data from Trakt API
     *
     * **/
    private suspend fun convertTraktShow(traktShow: Show, credits: TmCredits, traktRating: Double): TmShow {
        Log.d(TAG, "convertTraktShowToTmShow: Falling back to Trakt metadata")

        return TmShow(
            traktShow.ids?.trakt ?: -1,
            null,
            traktShow.ids?.imdb,
            traktShow.title ?: "",
            traktShow.overview ?: "",
            traktShow.country,
            traktShow.genres,
            credits.crew?.production?.filter { it.job?.uppercase() == TRAKT_PRODUCER },
            traktShow.homepage,
            traktShow.status,
            traktShow.language,
            traktShow.first_aired,
            traktShow.network,
            0,
            0,
            traktShow.runtime,
            null,
            null,
            null,
            false,
            traktRating)
    }

    private suspend fun findTmdbShow(traktShow: Show, credits: TmCredits, traktRating: Double): TmShow? {
        val findShowResponse =
            tmdbApi.tmSearchService().tv(traktShow.title, 1, getTmdbLanguage(null), traktShow.year, false)

        if (findShowResponse.results?.isNotEmpty() == true) {
            // Base TV Show gets us Tmdb ID
            val foundShow = findShowResponse.results!!.first()

            // Get full show Data
            val tvShow = tmdbApi.tmTvService().tv(
                foundShow?.id ?: -1, getTmdbLanguage(traktShow.language),
                AppendToResponse(
                    AppendToResponseItem.EXTERNAL_IDS,
                    AppendToResponseItem.VIDEOS
                )
            )

            Log.d(TAG, "findTmdbShow: Found show $tvShow")

            return convertTmdbShow(traktShow, credits, tvShow, traktRating)
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
                                traktSeason.title ?: "",
                                traktSeason.overview ?: "",
                                tvSeason?.images,
                                tvSeason?.videos,
                                traktSeason.first_aired,
                                traktSeason.episode_count ?: -1,
                                traktSeason.number ?: 0,
                                tvSeason?.poster_path,
                                TmSeason.SOURCE_TMDB
                            )
                        )
                    }
                }
            } catch(e: HttpException) {
                Log.e(TAG, "getSeasons: Error getting Season Data from TMDB. Code ${e.code()}. ${e.message()}")
                e.printStackTrace()

                tmSeasons.addAll(traktSeasonSource(showTmdbId, showTraktId, language, traktSeasons))
            } catch(e: Exception) {
                Log.e(TAG, "getSeasons: Error getting Tmdb Data.")
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
                    tvSeason.first_aired,
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
                            traktEpisode.ids?.imdb,
                            showTmdbId,
                            showTraktId,
                            language,
                            tmdbEpisode?.season_number ?: 0,
                            tmdbEpisode?.episode_number ?: 0,
                            tmdbEpisode?.production_code,
                            tmdbEpisode?.name ?: "",
                            traktEpisode.overview,
                            traktEpisode.runtime,
                            traktEpisode.first_aired,
                            tmdbEpisode?.images,
                            tmdbEpisode?.still_path,
                            tmdbEpisode?.videos,
                            traktEpisode.rating ?: 0.0
                        )
                    )
                }
            } catch(e: HttpException) {
                Log.e(TAG, "getSeasonEpisodesData: HttpException Error getting episode data from Tmdb. Code: ${e.code()}. ${e.message()}")
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
        Log.d(TAG, "traktEpisodeSource: Getting Trakt Episode Data")
        var episodes: MutableList<TmEpisode> = mutableListOf()

        traktSeasonEpisodes.map { traktEpisode ->
            episodes.add(
                TmEpisode(
                    traktEpisode.ids?.trakt ?: 0,
                    traktEpisode.ids?.trakt ?: 0,
                    null,
                    traktEpisode.ids?.imdb,
                    showTmdbId,
                    showTraktId,
                    language,
                    traktEpisode.season ?: 0,
                    traktEpisode.number ?: 0,
                    null,
                    traktEpisode.title ?: "",
                    traktEpisode.overview,
                    traktEpisode.runtime,
                    traktEpisode.first_aired,
                    null,
                    null,
                    null,
                    traktEpisode.rating
                )
            )
        }
        return episodes
    }
}
