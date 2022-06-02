package com.nickrankin.traktapp.api.services.trakt

import com.nickrankin.traktapp.api.services.trakt.model.Credits
import com.nickrankin.traktapp.api.services.trakt.model.Person
import com.uwetrottmann.trakt5.enums.Extended
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmPerson {

    /**
     * Returns a single person's details.
     *
     * @param personId trakt ID, trakt slug, or IMDB ID Example: bryan-cranston.
     */
    @GET("people/{id}")
    suspend fun summary(
        @Path("id") personId: String?,
        @Query("extended") extended: Extended?
    ): Person

    @GET("people/{id}/movies")
    suspend fun movieCredits(
        @Path("id") personId: String?,
        @Query("extended") extended: Extended?
    ): Credits

    @GET("people/{id}/shows")
    suspend fun showCredits(
        @Path("id") personId: String?
    ): Credits
}