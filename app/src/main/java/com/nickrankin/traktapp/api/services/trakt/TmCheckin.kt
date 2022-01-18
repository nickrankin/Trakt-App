package com.nickrankin.traktapp.api.services.trakt

import com.uwetrottmann.trakt5.entities.EpisodeCheckin
import com.uwetrottmann.trakt5.entities.EpisodeCheckinResponse
import com.uwetrottmann.trakt5.entities.MovieCheckin
import com.uwetrottmann.trakt5.entities.MovieCheckinResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

interface TmCheckin {

    /**
     * **OAuth Required**
     *
     *
     *  Check into an episode. This should be tied to a user action to manually indicate they are watching something.
     * The item will display as watching on the site, then automatically switch to watched status once the duration has
     * elapsed.
     */
    @POST("checkin")
    suspend fun checkin(
        @Body episodeCheckin: EpisodeCheckin?
    ): EpisodeCheckinResponse

    /**
     * **OAuth Required**
     *
     *
     *  Check into a movie. This should be tied to a user action to manually indicate they are watching something.
     * The item will display as watching on the site, then automatically switch to watched status once the duration has
     * elapsed.
     */
    @POST("checkin")
    suspend fun checkin(
        @Body movieCheckin: MovieCheckin?
    ): MovieCheckinResponse

    /**
     * **OAuth Required**
     *
     *
     *  Removes any active checkins, no need to provide a specific item.
     */
    @DELETE("checkin")
    suspend fun deleteActiveCheckin(): Response<Unit>
}