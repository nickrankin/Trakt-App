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

            val tmMovie = getTmdbData(traktMovie.ids?.tmdb)

        return if (tmMovie != null) {
                getMovieDataTmdb(traktMovie, tmMovie)
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
                    getMovieDataTmdb(traktMovie, tmdbFoundMovie)
                } else {
                    getMovieDataTrakt(traktMovie)
                }
            }
    }

    private suspend fun getTmdbData(tmdbId: Int?): Movie? {
        return tmdbApi.tmMovieService().summary(tmdbId ?: -1, getTmdbLanguage(), AppendToResponse(AppendToResponseItem.VIDEOS))
    }

    private suspend fun findTmdbMovie(title: String?, year: Int?): BaseMovie? {
        val foundTmdbMovie = tmdbApi.tmSearchService()
            .movie(title, 1, getTmdbLanguage(), null, true, year, null)

        return foundTmdbMovie.results?.first()
    }

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