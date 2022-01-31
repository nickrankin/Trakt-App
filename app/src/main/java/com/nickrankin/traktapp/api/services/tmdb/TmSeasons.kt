package com.nickrankin.traktapp.api.services.tmdb

import com.uwetrottmann.tmdb2.entities.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface TmSeasons {

    /**
     * Get the primary information about a TV season by its season number.
     *
     * @param tvShowId           A Tv Show TvSeason TMDb id.
     * @param tvShowSeasonNumber TvSeason Number.
     * @param language           *Optional.* ISO 639-1 code.
     */
    @GET("tv/{tv_id}/season/{season_number}")
    suspend fun season(
        @Path("tv_id") tvShowId: Int,
        @Path("season_number") tvShowSeasonNumber: Int,
        @Query("language") language: String?
    ): TvSeason

    /**
     * Get the primary information about a TV season by its season number.
     *
     * @param tvShowId           A Tv Show TvSeason TMDb id.
     * @param tvShowSeasonNumber TvSeason Number.
     * @param language           *Optional.* ISO 639-1 code.
     * @param appendToResponse   *Optional.* extra requests to append to the result.
     */
    @GET("tv/{tv_id}/season/{season_number}")
    suspend fun season(
        @Path("tv_id") tvShowId: Int,
        @Path("season_number") tvShowSeasonNumber: Int,
        @Query("language") language: String,
        @Query("append_to_response") appendToResponse: AppendToResponse
    ): TvSeason

    /**
     * Get the primary information about a TV season by its season number.
     *
     * @param tvShowId           A Tv Show TvSeason TMDb id.
     * @param tvShowSeasonNumber TvSeason Number.
     * @param language           *Optional.* ISO 639-1 code.
     * @param appendToResponse   *Optional.* extra requests to append to the result.
     * @param options            *Optional.* parameters for the appended extra results.
     */
    @GET("tv/{tv_id}/season/{season_number}")
    suspend fun season(
        @Path("tv_id") tvShowId: Int,
        @Path("season_number") tvShowSeasonNumber: Int,
        @Query("language") language: String,
        @Query("append_to_response") appendToResponse: AppendToResponse,
        @QueryMap options: Map<String, String?>
    ): TvSeason

    /**
     * Grab the following account states for a session:
     *
     * Returns all of the user ratings for the season's episodes.
     *
     * **Requires an active Session.**
     *
     * @param tmdbId             A Tv Show TvSeason TMDb id.
     * @param tvShowSeasonNumber TvSeason Number.
     */
    @GET("tv/{tv_id}/season/{season_number}/account_states")
    suspend fun accountStates(
        @Path("tv_id") tmdbId: Int,
        @Path("season_number") tvShowSeasonNumber: Int
    ): AccountStatesResults

    /**
     * Get the changes for a TV show. By default only the last 24 hours are returned.
     *
     * Get the changes for a TV season. By default only the last 24 hours are returned.
     *
     * You can query up to 14 days in a single query by using the start_date and end_date query parameters.
     *
     * @param tvShowSeasonId A Tv Show TvSeason TMDb id.
     * @param start_date     *Optional.* Starting date of changes occurred to a movie.
     * @param end_date       *Optional.* Ending date of changes occurred to a movie.
     * @param page           *Optional.* Minimum value is 1, expected value is an integer.
     */
    @GET("tv/season/{season_id}/changes")
    suspend fun changes(
        @Path("season_id") tvShowSeasonId: Int,
        @Query("start_date") start_date: TmdbDate,
        @Query("end_date") end_date: TmdbDate,
        @Query("page") page: Int
    ): Changes

    /**
     * Get the cast and crew credits for a TV season by season number.
     *
     * @param tvShowId           A Tv Show TvSeason TMDb id.
     * @param tvShowSeasonNumber TvSeason Number.
     */
    @GET("tv/{tv_id}/season/{season_number}/credits")
    suspend fun credits(
        @Path("tv_id") tvShowId: Int,
        @Path("season_number") tvShowSeasonNumber: Int
    ): Credits

    /**
     * Get the external ids that we have stored for a TV season by season number.
     *
     * @param tvShowId           A Tv Show TvSeason TMDb id.
     * @param tvShowSeasonNumber TvSeason Number.
     * @param language           *Optional.* ISO 639-1 code.
     */
    @GET("tv/{tv_id}/season/{season_number}/external_ids")
    suspend fun externalIds(
        @Path("tv_id") tvShowId: Int,
        @Path("season_number") tvShowSeasonNumber: Int,
        @Query("language") language: String
    ): TvSeasonExternalIds

    /**
     * Get the images (posters) that we have stored for a TV season by season number.
     *
     * @param tvShowId           A Tv Show TvSeason TMDb id.
     * @param tvShowSeasonNumber TvSeason Number.
     * @param language           *Optional.* ISO 639-1 code.
     */
    @GET("tv/{tv_id}/season/{season_number}/images")
    suspend fun images(
        @Path("tv_id") tvShowId: Int,
        @Path("season_number") tvShowSeasonNumber: Int,
        @Query("language") language: String
    ): Images

    /**
     * Get the videos that have been added to a TV season (trailers, teasers, etc...)
     *
     * @param tvShowId           A Tv Show TvSeason TMDb id.
     * @param tvShowSeasonNumber TvSeason Number.
     * @param language           *Optional.* ISO 639-1 code.
     */
    @GET("tv/{tv_id}/season/{season_number}/videos")
    suspend fun videos(
        @Path("tv_id") tvShowId: Int,
        @Path("season_number") tvShowSeasonNumber: Int,
        @Query("language") language: String
    ): Videos

}