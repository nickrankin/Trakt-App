package com.nickrankin.traktapp.api.services.tmdb

import com.uwetrottmann.tmdb2.entities.*
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface TmPersonService {
    /**
     * Get the general person information for a specific id.
     *
     * @param personId A Person TMDb id.
     */
    @GET("person/{person_id}")
    suspend fun summary(
        @Path("person_id") personId: Int,
        @Query("language") language: String?
    ): Person

    /**
     * Get the general person information for a specific id.
     *
     * @param personId         A Person TMDb id.
     * @param appendToResponse *Optional.* extra requests to append to the result. **Accepted Value(s):** movie_credits, tv_credits, combined_credits, external_ids, images, changes, tagged_images,
     */
    @GET("person/{person_id}")
    suspend fun summary(
        @Path("person_id") personId: Int,
        @Query("language") language: String?,
        @Query("append_to_response") appendToResponse: AppendToResponse?
    ): Person

    /**
     * Get the general person information for a specific id.
     *
     * @param personId         A Person TMDb id.
     * @param appendToResponse *Optional.* extra requests to append to the result. **Accepted Value(s):** movie_credits, tv_credits, combined_credits, external_ids, images, changes, tagged_images,
     * @param options          *Optional.* parameters for the appended extra results.
     */
    @GET("person/{person_id}")
    suspend fun summary(
        @Path("person_id") personId: Int,
        @Query("language") language: String?,
        @Query("append_to_response") appendToResponse: AppendToResponse?,
        @QueryMap options: Map<String?, String?>
    ): Person

    /**
     * Get the movie credits for a specific person id.
     *
     * @param personId A Person TMDb id.
     * @param language *Optional.* ISO 639-1 code.
     */
    @GET("person/{person_id}/movie_credits")
    suspend fun movieCredits(
        @Path("person_id") personId: Int,
        @Query("language") language: String?
    ): PersonCredits

    /**
     * Get the TV credits for a specific person id.
     *
     * @param personId A Person TMDb id.
     * @param language *Optional.* ISO 639-1 code.
     */
    @GET("person/{person_id}/tv_credits")
    suspend fun tvCredits(
        @Path("person_id") personId: Int,
        @Query("language") language: String?
    ): PersonCredits

    /**
     * Get the movie and TV credits for a specific person id.
     *
     * @param personId A Person TMDb id.
     * @param language *Optional.* ISO 639-1 code.
     */
    @GET("person/{person_id}/combined_credits")
    suspend fun combinedCredits(
        @Path("person_id") personId: Int,
        @Query("language") language: String?
    ): PersonCredits

    /**
     * Get the external ids for a specific person id.
     *
     * @param personId A Person TMDb id.
     */
    @GET("person/{person_id}/external_ids")
    suspend fun externalIds(
        @Path("person_id") personId: Int
    ): PersonExternalIds

    /**
     * Get the images for a specific person id.
     */
    @GET("person/{person_id}/images")
    suspend fun images(
        @Path("person_id") personId: Int
    ): PersonImages

    /**
     * Get the changes for a person. By default only the last 24 hours are returned.
     *
     *
     * You can query up to 14 days in a single query by using the start_date and end_date query parameters.
     *
     * @param personId   A Person TMDb id.
     * @param start_date *Optional.* Starting date of changes occurred to a movie.
     * @param end_date   *Optional.* Ending date of changes occurred to a movie.
     * @param page       *Optional.* Minimum value is 1, maximum 1000, expected value is an integer.
     * @param language   *Optional.* ISO 639-1 code.
     */
    @GET("person/{person_id}/changes")
    suspend fun changes(
        @Path("person_id") personId: Int,
        @Query("language") language: String?,
        @Query("start_date") start_date: TmdbDate?,
        @Query("end_date") end_date: TmdbDate?,
        @Query("page") page: Int?
    ): Changes

    /**
     * Get the images that have been tagged with a specific person id. Return all of the image results with a [ ] object mapped for each image.
     *
     * @param personId A Person TMDb id.
     * @param page     *Optional.* Minimum value is 1, maximum 1000, expected value is an integer.
     * @param language *Optional.* ISO 639-1 code.
     */
    @GET("person/{person_id}/tagged_images")
    suspend fun taggedImages(
        @Path("person_id") personId: Int,
        @Query("page") page: Int?,
        @Query("language") language: String?
    ): TaggedImagesResultsPage

    /**
     * Get the list of popular people on The Movie Database. This list refreshes every day.
     */
    @GET("person/popular")
    suspend fun popular(
        @Query("page") page: Int?
    ): PersonResultsPage

    /**
     * Get the latest person id.
     */
    @GET("person/latest")
    suspend fun latest(): Person
}
