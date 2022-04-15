package com.nickrankin.traktapp.api.services.tmdb

import com.uwetrottmann.tmdb2.entities.*
import retrofit2.Call
import retrofit2.http.*

interface TmMoviesService {
    /**
     * Get the basic movie information for a specific movie id.
     *
     * @param movieId  A Movie TMDb id.
     * @param language *Optional.* ISO 639-1 code.
     */
    @GET("movie/{movie_id}")
    suspend fun summary(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String
    ): Movie?

    /**
     * Get the basic movie information for a specific movie id.
     *
     * @param movieId          A Movie TMDb id.
     * @param language         *Optional.* ISO 639-1 code.
     * @param appendToResponse *Optional.* extra requests to append to the result. **Accepted Value(s):** alternative_titles, changes, credits, images, keywords, release_dates, videos, translations, recommendations, similar, reviews, lists
     */
    @GET("movie/{movie_id}")
    suspend fun summary(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String,
        @Query("append_to_response") appendToResponse: AppendToResponse
    ): Movie?

    /**
     * Get the basic movie information for a specific movie id.
     *
     * @param movieId          A Movie TMDb id.
     * @param language         *Optional.* ISO 639-1 code.
     * @param appendToResponse *Optional.* extra requests to append to the result. **Accepted Value(s):** alternative_titles, changes, credits, images, keywords, release_dates, videos, translations, recommendations, similar, reviews, lists
     * @param options          *Optional.* parameters for the appended extra results.
     */
    @GET("movie/{movie_id}")
    suspend fun summary(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String,
        @Query("append_to_response") appendToResponse: AppendToResponse,
        @QueryMap options: Map<String, String>
    ): Movie

    /**
     * Grab the following account states for a session:
     *
     * * Movie rating
     * * If it belongs to your watchlist
     * * If it belongs to your favorite list
     *
     * **Requires an active Session.**
     *
     * @param movieId A Movie TMDb id.
     */
    @GET("movie/{movie_id}/account_states")
    suspend fun accountStates(
        @Path("movie_id") movieId: Int
    ): AccountStates

    /**
     * Get the alternative titles for a specific movie id.
     *
     * @param movieId A Movie TMDb id.
     * @param country *Optional.* ISO 3166-1 code.
     */
    @GET("movie/{movie_id}/alternative_titles")
    suspend fun alternativeTitles(
        @Path("movie_id") movieId: Int,
        @Query("country") country: String
    ): AlternativeTitles

    /**
     * Get the changes for a movie. By default only the last 24 hours are returned.
     *
     *
     * You can query up to 14 days in a single query by using the start_date and end_date query parameters.
     *
     * @param movieId    A Movie TMDb id.
     * @param start_date *Optional.* Starting date of changes occurred to a movie.
     * @param end_date   *Optional.* Ending date of changes occurred to a movie.
     * @param page       *Optional.* Minimum value is 1, expected value is an integer.
     */
    @GET("movie/{movie_id}/changes")
    suspend fun changes(
        @Path("movie_id") movieId: Int,
        @Query("start_date") start_date: TmdbDate,
        @Query("end_date") end_date: TmdbDate,
        @Query("page") page: Int
    ): Changes

    /**
     * Get the cast and crew information for a specific movie id.
     *
     * @param movieId A Movie TMDb id.
     */
    @GET("movie/{movie_id}/credits")
    suspend fun credits(
        @Path("movie_id") movieId: Int
    ): Credits

    /**
     * Get the external ids that we have stored for a movie.
     *
     * @param movieId A Movie TMDb id.
     * @param language *Optional.* ISO 639-1 code.
     */
    @GET("movie/{movie_id}/external_ids")
    suspend fun externalIds(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String
    ): MovieExternalIds

    /**
     * Get the images (posters and backdrops) for a specific movie id.
     *
     * @param movieId  A Movie TMDb id.
     * @param language *Optional.* ISO 639-1 code.
     */
    @GET("movie/{movie_id}/images")
    suspend fun images(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String
    ): Images

    /**
     * Get the plot keywords for a specific movie id.
     *
     * @param movieId A Movie TMDb id.
     */
    @GET("movie/{movie_id}/keywords")
    suspend fun keywords(
        @Path("movie_id") movieId: Int
    ): Keywords

    /**
     * Get the lists that the movie belongs to.
     *
     * @param movieId  A Movie TMDb id.
     * @param page     *Optional.* Minimum value is 1, expected value is an integer.
     * @param language *Optional.* ISO 639-1 code.
     */
    @GET("movie/{movie_id}/lists")
    suspend fun lists(
        @Path("movie_id") movieId: Int,
        @Query("page") page: Int,
        @Query("language") language: String
    ): ListResultsPage

    /**
     * Get the similar movies for a specific movie id.
     *
     * @param movieId  A Movie TMDb id.
     * @param page     *Optional.* Minimum value is 1, expected value is an integer.
     * @param language *Optional.* ISO 639-1 code.
     */
    @GET("movie/{movie_id}/similar")
    suspend fun similar(
        @Path("movie_id") movieId: Int,
        @Query("page") page: Int,
        @Query("language") language: String
    ): MovieResultsPage

    /**
     * Get the recommendations for a particular movie id.
     *
     * @param movieId  A Movie TMDb id.
     * @param page     *Optional.* Minimum value is 1, expected value is an integer.
     * @param language *Optional.* ISO 639-1 code.
     */
    @GET("movie/{movie_id}/recommendations")
    suspend fun recommendations(
        @Path("movie_id") movieId: Int,
        @Query("page") page: Int,
        @Query("language") language: String
    ): MovieResultsPage

    /**
     * Get the release dates, certifications and related information by country for a specific movie id.
     *
     * The results are keyed by iso_3166_1 code and contain a type value which on our system, maps to:
     * [ReleaseDate.TYPE_PREMIERE], [ReleaseDate.TYPE_THEATRICAL_LIMITED],
     * [ReleaseDate.TYPE_THEATRICAL], [ReleaseDate.TYPE_DIGITAL], [ReleaseDate.TYPE_PHYSICAL],
     * [ReleaseDate.TYPE_TV]
     *
     * @param movieId A Movie TMDb id.
     */
    @GET("movie/{movie_id}/release_dates")
    suspend fun releaseDates(
        @Path("movie_id") movieId: Int
    ): ReleaseDatesResults

    /**
     * Get the reviews for a particular movie id.
     *
     * @param movieId  A Movie TMDb id.
     * @param page     *Optional.* Minimum value is 1, expected value is an integer.
     * @param language *Optional.* ISO 639-1 code.
     */
    @GET("movie/{movie_id}/reviews")
    suspend fun reviews(
        @Path("movie_id") movieId: Int,
        @Query("page") page: Int,
        @Query("language") language: String
    ): ReviewResultsPage

    /**
     * Get the translations for a specific movie id.
     *
     * @param movieId A Movie TMDb id.
     */
    @GET("movie/{movie_id}/translations")
    suspend fun translations(
        @Path("movie_id") movieId: Int
    ): Translations

    /**
     * Get the videos (trailers, teasers, clips, etc...) for a specific movie id.
     *
     * @param movieId  A Movie TMDb id.
     * @param language *Optional.* ISO 639-1 code.
     */
    @GET("movie/{movie_id}/videos")
    suspend fun videos(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String
    ): Videos

    /**
     * Get a list of the availabilities per country by provider.
     *
     * Please note: In order to use this data you must attribute the source of the data as JustWatch.
     *
     * @see [Documentation](https://developers.themoviedb.org/3/movies/get-movie-watch-providers)
     */
    @GET("movie/{movie_id}/watch/providers")
    suspend fun watchProviders(
        @Path("movie_id") movieId: Int
    ): WatchProviders

    /**
     * Get the latest movie id.
     */
    @GET("movie/latest")
    suspend fun latest(): Movie

    /**
     * Get a list of movies in theatres. This is a release type query that looks
     * for all movies that have a release type of 2 or 3 within the specified date range.
     *
     * You can optionally specify a region parameter which will narrow the search
     * to only look for theatrical release dates within the specified country.
     *
     * @see [Documentation](https://developers.themoviedb.org/3/movies/get-now-playing)
     */
    @GET("movie/now_playing")
    suspend fun nowPlaying(
        @Query("page") page: Int,
        @Query("language") language: String,
        @Query("region") region: String
    ): MovieResultsPage

    /**
     * Get a list of the current popular movies on TMDb. This list updates daily.
     *
     * @see [Documentation](https://developers.themoviedb.org/3/movies/get-popular-movies)
     */
    @GET("movie/popular")
    suspend fun popular(
        @Query("page") page: Int,
        @Query("language") language: String,
        @Query("region") region: String
    ): MovieResultsPage

    /**
     * Get the top rated movies on TMDb.
     *
     * @see [Documentation](https://developers.themoviedb.org/3/movies/get-top-rated-movies)
     */
    @GET("movie/top_rated")
    suspend fun topRated(
        @Query("page") page: Int,
        @Query("language") language: String,
        @Query("region") region: String
    ): MovieResultsPage

    /**
     * Get a list of upcoming movies in theatres. This is a release type query that looks
     * for all movies that have a release type of 2 or 3 within the specified date range.
     *
     * You can optionally specify a region prameter which will narrow the search to
     * only look for theatrical release dates within the specified country.
     *
     * @see [Documentation](https://developers.themoviedb.org/3/movies/get-upcoming)
     */
    @GET("movie/upcoming")
    suspend fun upcoming(
        @Query("page") page: Int,
        @Query("language") language: String,
        @Query("region") region: String
    ): MovieResultsPage

    /**
     * Sets the Rating for the movie with the specified id.
     *
     * **Requires an active Session.**
     *
     * @param movieId A Movie TMDb id.
     * @param body    *Required.* A ReviewObject Object. Minimum value is 0.5 and Maximum 10.0, expected value is a number.
     */
    @POST("movie/{movie_id}/rating")
    suspend fun addRating(
        @Path("movie_id") movieId: Int,
        @Body body: RatingObject
    ): Status

    /**
     * Deletes the Rating for the movie with the specified id.
     *
     * **Requires an active Session.**
     *
     * @param movieId A Movie TMDb id.
     */
    @DELETE("movie/{movie_id}/rating")
    suspend fun deleteRating(
        @Path("movie_id") movieId: Int
    ): Status
}
