package com.nickrankin.traktapp.api.services.trakt

import com.uwetrottmann.trakt5.entities.Movie
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.enums.Extended
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmRecommendations {
        /**
         * **OAuth Required**
         *
         *
         *  Personalized movie recommendations for a user. Results returned with the top recommendation first.
         *
         * @param page Number of page of results to be returned. If `null` defaults to 1.
         * @param limit Number of results to return per page. If `null` defaults to 10.
         */
        @GET("recommendations/movies")
        suspend fun movies(
            @Query("page") page: Int?,
            @Query("limit") limit: Int?,
            @Query(value = "extended", encoded = true) extended: Extended?
        ): List<Movie>

        /**
         * **OAuth Required**
         *
         *
         *  Dismiss a movie from getting recommended anymore.
         *
         * @param movieId trakt ID, trakt slug, or IMDB ID. Example: "tron-legacy-2010".
         */
        @DELETE("recommendations/movies/{id}")
        suspend fun dismissMovie(
            @Path("id") movieId: String?
        ): Response<Unit>

        /**
         * **OAuth Required**
         *
         *
         *  Personalized show recommendations for a user. Results returned with the top recommendation first.
         *
         * @param page Number of page of results to be returned. If `null` defaults to 1.
         * @param limit Number of results to return per page. If `null` defaults to 10.
         */
        @GET("recommendations/shows")
        suspend fun shows(
            @Query("page") page: Int?,
            @Query("limit") limit: Int?,
            @Query(value = "extended", encoded = true) extended: Extended?
        ): List<Show>

        /**
         * **OAuth Required**
         *
         *
         *  Dismiss a show from getting recommended anymore.
         *
         * @param showId trakt ID, trakt slug, or IMDB ID. Example: 922.
         */
        @DELETE("recommendations/shows/{id}")
        suspend fun dismissShow(
            @Path("id") showId: String?
        ): Response<Unit>
}