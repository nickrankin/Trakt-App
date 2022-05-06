package com.nickrankin.traktapp.helper

import android.util.Log
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.credits.model.CastPerson
import com.nickrankin.traktapp.dao.credits.model.MovieCastPersonData
import com.nickrankin.traktapp.dao.credits.model.ShowCastPersonData
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ShowCreditsHelper"

@Singleton
class MovieCreditsHelper @Inject constructor(
    private val traktApi: TraktApi,
    private val tmdbApi: TmdbApi
) {

    suspend fun getMovieCredits(
        movieTraktId: Int,
        movieTmdbId: Int?
    ): List<Pair<MovieCastPersonData, CastPerson>> {
        Log.d(TAG, "getCredits: Getting cast credits")
        val castPeople: MutableList<Pair<MovieCastPersonData, CastPerson>> = mutableListOf()

        // Store TMDB response data as K: TMDBID, V: Image path
        val castImagesMap: MutableMap<Int, String?> = mutableMapOf()

        val traktCastMembers = traktApi.tmMovies().people(movieTraktId.toString())
        Log.d(TAG, "getCredits: Got ${traktCastMembers.cast?.size} from Trakt API")

        if (movieTmdbId != null && movieTmdbId != 0 && movieTmdbId != -1) {
            Log.d(TAG, "getCredits: Getting cast data from TMDB")
            // Try to get Credit data from TMDB
            try {
                val tmdbResponse = tmdbApi.tmMovieService().credits(movieTmdbId)
                Log.d(TAG, "getCredits: Got ${tmdbResponse.cast?.size} from TMDB API")

                if (tmdbResponse.cast?.isNotEmpty() == true) {
                    tmdbResponse.cast?.map { tmdbCastMember ->
                        castImagesMap.put(tmdbCastMember?.id ?: 0, tmdbCastMember.profile_path)
                    }
                }

                // TODO Get Guest Star photos. Tmdb only supports Guest stars from Episode endpoint :(
//                if(tmdbResponse.guest_stars?.isNotEmpty() == true) {
//                    tmdbResponse.guest_stars?.map { tmdbCastMember ->
//                        castImagesMap.put(tmdbCastMember?.id ?: 0, tmdbCastMember.profile_path)
//                    }
//                }
            } catch (e: Exception) {
                Log.e(TAG, "getCredits: Failed to get credits from TMDB ${e.localizedMessage}")
                e.printStackTrace()
            }
        } else {
            Log.d(TAG, "getCredits: Unable to get data for TMDB (TMDB ID: $movieTmdbId)")
        }

        traktCastMembers.cast?.mapIndexed { index, traktCastMember ->
            val posterPath = castImagesMap[traktCastMember.person?.ids?.tmdb]
            Log.d(
                TAG,
                "getCredits: Got poster path for member ${traktCastMember.person?.name} // $posterPath"
            )

            castPeople.add(
                Pair(
                    MovieCastPersonData(
                        "trakt_$movieTraktId-${traktCastMember.person?.ids?.trakt ?: 0}",
                        "trakt_${traktCastMember.person?.ids?.trakt ?: 0}",
                        movieTraktId,
                        index,
                        traktCastMember.character
                    ),
                    CastPerson(
                        "trakt_${traktCastMember.person?.ids?.trakt ?: 0}",
                        "trakt_${traktCastMember.person?.ids?.trakt ?: 0}",
                        traktCastMember.person?.ids?.imdb,
                        traktCastMember.person?.biography,
                        traktCastMember.person?.birthplace,
                        traktCastMember.person?.name ?: "",
                        posterPath
                    )
                )

            )
        }
        return castPeople
    }
}