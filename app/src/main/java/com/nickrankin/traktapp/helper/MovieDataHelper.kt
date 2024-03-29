package com.nickrankin.traktapp.helper

import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.movies.model.TmMovie
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.BaseMovie
import com.uwetrottmann.tmdb2.entities.Movie
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem
import com.uwetrottmann.trakt5.entities.Credits
import com.uwetrottmann.trakt5.enums.Extended
import java.lang.StringBuilder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieDataHelper @Inject constructor(
    private val traktApi: TraktApi,
    private val tmdbApi: TmdbApi
) {
    suspend fun getMovieSummary(movieTraktId: Int): TmMovie {
            val traktMovie = traktApi.tmMovies().summary(movieTraktId.toString(), Extended.FULL)
            val traktMovieCredits = traktApi.tmMovies().people(movieTraktId.toString())
            val traktMovieRating = traktApi.tmMovies().ratings(movieTraktId.toString())

            val tmMovie = getTmdbData(traktMovie.ids?.tmdb)

        return if (tmMovie != null) {
                getMovieDataTmdb(traktMovie, traktMovieCredits, tmMovie, traktMovieRating.rating)
            } else {
                // Try to find the movie
                val foundMovie = findTmdbMovie(
                    traktMovie.title,
                    traktMovie.year
                )
                var tmdbFoundMovie: Movie? = null

                if (foundMovie != null) {
                    // We have a basemovie including TMDB ID, lets get the info
                    tmdbFoundMovie =
                        getTmdbData(foundMovie.id)
                }

                // If we have a valid TMDB Movie, use this source otherwise need to fallback to Trakt
                if (tmdbFoundMovie != null) {
                    getMovieDataTmdb(traktMovie, traktMovieCredits, tmdbFoundMovie, traktMovieRating.rating ?: 0.0)
                } else {
                    getMovieDataTrakt(traktMovie, traktMovieCredits, traktMovieRating.rating)
                }
            }
    }

    private suspend fun getTmdbData(tmdbId: Int?): Movie? {
        return tmdbApi.tmMovieService().summary(tmdbId ?: -1, getTmdbLanguage(null), AppendToResponse(AppendToResponseItem.VIDEOS))
    }

    private suspend fun findTmdbMovie(title: String?, year: Int?): BaseMovie? {
        val foundTmdbMovie = tmdbApi.tmSearchService()
            .movie(title, 1, getTmdbLanguage(null), null, true, year, null)

        return foundTmdbMovie.results?.first()
    }

    private fun getMovieDataTmdb(traktMovie: com.uwetrottmann.trakt5.entities.Movie, credits: Credits, tmdbMovie: Movie, traktRating: Double?): TmMovie {

        return TmMovie(
            traktMovie.ids?.trakt ?: 0,
            tmdbMovie.id,
            traktMovie.ids?.imdb,
            tmdbMovie.title ?: "Unknown Movie",
            traktMovie.overview,
            credits.crew?.directing ?: emptyList(),
            credits.crew?.writing ?: emptyList(),
            traktMovie.genres ?: emptyList(),
            traktMovie.language,
            tmdbMovie.original_title,
            traktMovie.homepage,
            tmdbMovie.poster_path,
            tmdbMovie.backdrop_path,
            tmdbMovie.production_companies ?: emptyList(),
            tmdbMovie.production_countries ?: emptyList(),
            traktMovie.released,
            tmdbMovie.revenue,
            traktMovie.runtime,
            traktMovie.tagline,
            traktMovie.trailer,
            traktRating
        )
    }

    // Fallback behaviour
    private fun getMovieDataTrakt(traktMovie: com.uwetrottmann.trakt5.entities.Movie, credits: Credits, traktRating: Double?): TmMovie {
        return TmMovie(
            traktMovie.ids?.trakt ?: -1,
            null,
            traktMovie.ids?.imdb,
            traktMovie.title ?: "Unknown Movie",
            traktMovie.overview,
            credits.crew?.directing ?: emptyList(),
            credits.crew?.writing ?: emptyList(),
            emptyList(),
            traktMovie.language,
            null,
            null,
            traktMovie.homepage,
            null,
            null,
            null,
            traktMovie.released,
            null,
            traktMovie.runtime,
            traktMovie.tagline,
            traktMovie.trailer,
            traktRating

        )
    }
}