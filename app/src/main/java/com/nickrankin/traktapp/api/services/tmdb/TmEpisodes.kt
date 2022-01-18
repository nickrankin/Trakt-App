package com.nickrankin.traktapp.api.services.tmdb

import com.uwetrottmann.tmdb2.entities.*
import retrofit2.http.*

interface TmEpisodes {

    /**
     * Get the primary information about a TV episode by combination of a season and episode number.
     *
     * @param tvShowId            A Tv Show TMDb id.
     * @param tvShowSeasonNumber  TvSeason Number.
     * @param tvShowEpisodeNumber TvEpisode Number.
     * @param language            *Optional.* ISO 639-1 code.
     */
    @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}")
    suspend fun episode(
        @Path("tv_id") tvShowId: Int,
        @Path("season_number") tvShowSeasonNumber: Int,
        @Path("episode_number") tvShowEpisodeNumber: Int,
        @Query("language") language: String
    ): TvEpisode

    /**
     * Get the primary information about a TV episode by combination of a season and episode number.
     *
     * @param tvShowId            A Tv Show TMDb id.
     * @param tvShowSeasonNumber  TvSeason Number.
     * @param tvShowEpisodeNumber TvEpisode Number.
     * @param language            *Optional.* ISO 639-1 code.
     * @param appendToResponse    *Optional.* extra requests to append to the result.
     */
    @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}")
    suspend fun episode(
        @Path("tv_id") tvShowId: Int,
        @Path("season_number") tvShowSeasonNumber: Int,
        @Path("episode_number") tvShowEpisodeNumber: Int,
        @Query("language") language: String,
        @Query("append_to_response") appendToResponse: AppendToResponse
    ): TvEpisode

    /**
     * Get the primary information about a TV episode by combination of a season and episode number.
     *
     * @param tvShowId            A Tv Show TMDb id.
     * @param tvShowSeasonNumber  TvSeason Number.
     * @param tvShowEpisodeNumber TvEpisode Number.
     * @param language            *Optional.* ISO 639-1 code.
     * @param appendToResponse    *Optional.* extra requests to append to the result.
     * @param options             *Optional.* parameters for the appended extra results.
     */
    @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}")
    suspend fun episode(
        @Path("tv_id") tvShowId: Int,
        @Path("season_number") tvShowSeasonNumber: Int,
        @Path("episode_number") tvShowEpisodeNumber: Int,
        @Query("language") language: String,
        @Query("append_to_response") appendToResponse: AppendToResponse,
        @QueryMap options: Map<String, String>
    ): TvEpisode

    /**
     * Get the changes for a TV episode. By default only the last 24 hours are returned.
     *
     * You can query up to 14 days in a single query by using the start_date and end_date query parameters.
     *
     * @param tvShowEpisodeId A Tv Show TvEpisode TMDb id.
     * @param start_date      *Optional.* Starting date of changes occurred to a movie.
     * @param end_date        *Optional.* Ending date of changes occurred to a movie.
     * @param page            *Optional.* Minimum value is 1, expected value is an integer.
     */
    @GET("tv/episode/{episode_id}/changes")
    suspend fun changes(
        @Path("episode_id") tvShowEpisodeId: Int,
        @Query("start_date") start_date: TmdbDate,
        @Query("end_date") end_date: TmdbDate,
        @Query("page") page: Int
    ): Changes

    /**
     * Get the TV episode credits by combination of season and episode number.
     *
     * @param tvShowId            A Tv Show TMDb id.
     * @param tvShowSeasonNumber  TvSeason Number.
     * @param tvShowEpisodeNumber TvEpisode Number.
     */
    @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}/credits")
    suspend fun credits(
        @Path("tv_id") tvShowId: Int,
        @Path("season_number") tvShowSeasonNumber: Int,
        @Path("episode_number") tvShowEpisodeNumber: Int
    ): Credits

    /**
     * Get the external ids for a TV episode by combination of a season and episode number.
     *
     * @param tvShowId            A Tv Show TMDb id.
     * @param tvShowSeasonNumber  TvSeason Number.
     * @param tvShowEpisodeNumber TvEpisode Number.
     */
    @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}/external_ids")
    suspend fun externalIds(
        @Path("tv_id") tvShowId: Int,
        @Path("season_number") tvShowSeasonNumber: Int,
        @Path("episode_number") tvShowEpisodeNumber: Int
    ): TvEpisodeExternalIds

    /**
     * Get the images (episode stills) for a TV episode by combination of a season and episode number. Since episode
     * stills don't have a language, this call will always return all images.
     *
     * @param tvShowId            A Tv Show TMDb id.
     * @param tvShowSeasonNumber  TvSeason Number.
     * @param tvShowEpisodeNumber TvEpisode Number.
     */
    @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}/images")
    suspend fun images(
        @Path("tv_id") tvShowId: Int,
        @Path("season_number") tvShowSeasonNumber: Int,
        @Path("episode_number") tvShowEpisodeNumber: Int
    ): Images

    /**
     * Get the videos that have been added to a TV episode (teasers, clips, etc...)
     *
     * @param tvShowId            A Tv Show TMDb id.
     * @param tvShowSeasonNumber  TvSeason Number.
     * @param tvShowEpisodeNumber TvEpisode Number.
     * @param language            *Optional.* ISO 639-1 code.
     */
    @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}/videos")
    suspend fun videos(
        @Path("tv_id") tvShowId: Int,
        @Path("season_number") tvShowSeasonNumber: Int,
        @Path("episode_number") tvShowEpisodeNumber: Int,
        @Query("language") language: String
    ): Videos

    /**
     * Grab the following account states for a session:
     *
     * * TV Episode rating
     *
     * **Requires an active Session.**
     *
     * @param tvShowId            TMDb id.
     * @param tvShowSeasonNumber  TvSeason Number.
     * @param tvShowEpisodeNumber TvEpisode Number.
     */
    @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}/account_states")
    suspend fun accountStates(
        @Path("tv_id") tvShowId: Int,
        @Path("season_number") tvShowSeasonNumber: Int,
        @Path("episode_number") tvShowEpisodeNumber: Int
    ): BaseAccountStates

    /**
     * Rate a TV show.
     *
     * **Requires an active Session.**
     *
     * @param tvShowId            TMDb id.
     * @param tvShowSeasonNumber  TvSeason Number.
     * @param tvShowEpisodeNumber TvEpisode Number.
     * @param body                *Required.* A ReviewObject Object. Minimum value is 0.5 and Maximum 10.0, expected value is a number.
     */
    @POST("tv/{tv_id}/season/{season_number}/episode/{episode_number}/rating")
    suspend fun addRating(
        @Path("tv_id") tvShowId: Int,
        @Path("season_number") tvShowSeasonNumber: Int,
        @Path("episode_number") tvShowEpisodeNumber: Int,
        @Body body: RatingObject
    ): Status

    /**
     * Remove your rating for a TV show.
     *
     * **Requires an active Session.**
     *
     * @param tvShowId            TMDb id.
     * @param tvShowSeasonNumber  TvSeason Number.
     * @param tvShowEpisodeNumber TvEpisode Number.
     */
    @DELETE("tv/{tv_id}/season/{season_number}/episode/{episode_number}/rating")
    suspend fun deleteRating(
        @Path("tv_id") tvShowId: Int,
        @Path("season_number") tvShowSeasonNumber: Int,
        @Path("episode_number") tvShowEpisodeNumber: Int
    ): Status
}