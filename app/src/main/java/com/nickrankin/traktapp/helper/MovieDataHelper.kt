package com.nickrankin.traktapp.helper

import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.movies.model.TmMovie
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.BaseMovie
import com.uwetrottmann.tmdb2.entities.Movie
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem
import com.uwetrottmann.trakt5.enums.Extended
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieDataHelper @Inject constructor(
    private val traktApi: TraktApi,
    private val tmdbApi: TmdbApi
) {
    suspend fun getMovieSummary(movieTraktId: Int): TmMovie? {
            val traktMovie = traktApi.tmMovies().summary(movieTraktId.toString(), Extended.FULL)

            val tmMovie = getTmdbData(traktMovie.ids?.tmdb, traktMovie.language)

        return if (tmMovie != null) {
                getMovieDataTmdb(traktMovie, tmMovie)
            } else {
                // Try to find the movie
                val foundMovie = findTmdbMovie(
                    getTmdbLanguage(traktMovie.language),
                    traktMovie.title,
                    traktMovie.year
                )
                var tmdbFoundMovie: Movie? = null

                if (foundMovie != null) {
                    // We have a basemovie including TMDB ID, lets get the info
                    tmdbFoundMovie =
                        getTmdbData(foundMovie.id, getTmdbLanguage(foundMovie.original_language))
                }

                // If we have a valid TMDB Movie, use this source otherwise need to fallback to Trakt
                if (tmdbFoundMovie != null) {
                    getMovieDataTmdb(traktMovie, tmdbFoundMovie)
                } else {
                    getMovieDataTrakt(traktMovie)
                }
            }
    }

    private suspend fun getTmdbData(tmdbId: Int?, language: String?): Movie? {
        return tmdbApi.tmMovieService().summary(tmdbId ?: -1, getTmdbLanguage(language), AppendToResponse(AppendToResponseItem.VIDEOS))
    }

    private suspend fun findTmdbMovie(language: String?, title: String?, year: Int?): BaseMovie? {
        val foundTmdbMovie = tmdbApi.tmSearchService()
            .movie(title, 1, getTmdbLanguage(language), null, true, year, null)

        return foundTmdbMovie.results?.first()
    }

    /**
     *     @PrimaryKey val trakt_id: Int,
    val tmdb_id: Int?,
    val title: String,
    val overview: String?,
    val original_language: String,
    val original_title: String?,
    val external_ids: MovieExternalIds,
    val homepage: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val imdb_id: String?,
    val production_companies: List<BaseCompany?>
    val production_countries: List<Country?>,
    val release_date: Date?,
    val revenue: Int?,
    val runtime: Int?,
    val status: Status?,
    val tagline: String?,
    val videos: Videos?
     *
     *
     * */

    private fun getMovieDataTmdb(traktMovie: com.uwetrottmann.trakt5.entities.Movie, tmdbMovie: Movie): TmMovie? {
        return TmMovie(
            traktMovie.ids?.trakt ?: 0,
            tmdbMovie.id,
            tmdbMovie.title ?: "Unknown Movie",
            tmdbMovie.overview,
            tmdbMovie.genres ?: emptyList(),
            tmdbMovie.original_language,
            tmdbMovie.original_title,
            tmdbMovie.external_ids,
            tmdbMovie.homepage,
            tmdbMovie.poster_path,
            tmdbMovie.backdrop_path,
            tmdbMovie.imdb_id,
            tmdbMovie.production_companies ?: emptyList(),
            tmdbMovie.production_countries ?: emptyList(),
            tmdbMovie.release_date,
            tmdbMovie.revenue,
            tmdbMovie.runtime,
            tmdbMovie.status,
            tmdbMovie.tagline,
            traktMovie.trailer
        )
    }

    // Fallback behaviour
    private fun getMovieDataTrakt(traktMovie: com.uwetrottmann.trakt5.entities.Movie): TmMovie? {
        return TmMovie(
            traktMovie.ids?.trakt ?: -1,
            null,
            traktMovie.title ?: "Unknown Movie",
            traktMovie.overview,
            emptyList(),
            traktMovie.language,
            null,
            null,
            traktMovie.homepage,
            null,
            null,
            traktMovie.ids?.imdb,
            emptyList(),
            emptyList(),
            null,
            null,
            traktMovie.runtime,
            null,
            traktMovie.tagline,
            traktMovie.trailer

        )
    }
}