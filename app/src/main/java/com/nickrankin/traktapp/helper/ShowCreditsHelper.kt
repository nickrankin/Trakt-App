package com.nickrankin.traktapp.helper

import android.util.Log
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.credits.model.CastPerson
import com.nickrankin.traktapp.dao.credits.model.ShowCastPersonData
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ShowCreditsHelper"

@Singleton
class ShowCreditsHelper @Inject constructor(
    private val traktApi: TraktApi,
    private val tmdbApi: TmdbApi
) {

    suspend fun getShowCredits(
        showTraktId: Int,
        showTmdbId: Int?
    ): List<Pair<ShowCastPersonData, CastPerson>> {
        Log.d(TAG, "getCredits: Getting cast credits")
        val castPeople: MutableList<Pair<ShowCastPersonData, CastPerson>> = mutableListOf()

        // Store TMDB response data as K: TMDBID, V: Image path
        val castImagesMap: MutableMap<Int, String?> = mutableMapOf()

        val traktCastMembers = traktApi.tmShows().people(showTraktId.toString(), "full,guest_stars")
        Log.d(TAG, "getCredits: Got ${traktCastMembers.cast?.size} from Trakt API")

        if (showTmdbId != null && showTmdbId != 0 && showTmdbId != -1) {
            Log.d(TAG, "getCredits: Getting cast data from TMDB")
            // Try to get Credit data from TMDB
            try {
                val tmdbResponse = tmdbApi.tmTvService().credits(showTmdbId, null)
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
            Log.d(TAG, "getCredits: Unable to get data for TMDB (TMDB ID: $showTmdbId)")
        }

        traktCastMembers.cast?.mapIndexed { index, traktCastMember ->
            val posterPath = castImagesMap[traktCastMember.person?.ids?.tmdb]
            Log.d(
                TAG,
                "getCredits: Got poster path for member ${traktCastMember.person?.name} // $posterPath"
            )

            castPeople.add(
                Pair(
                    ShowCastPersonData(
                        traktCastMember.person?.ids?.trakt ?: 0,
                        showTraktId,
                        index,
                        false,
                        traktCastMember.character
                    ),
                    CastPerson(
                        traktCastMember.person?.ids?.trakt ?: 0,
                        traktCastMember.person?.ids?.tmdb,
                        traktCastMember.person?.ids?.imdb,
                        traktCastMember.person?.biography,
                        traktCastMember.person?.birthplace,
                        traktCastMember.person?.name ?: "",
                        posterPath
                    )
                )

            )
        }

        traktCastMembers.guest_stars?.mapIndexed { index, traktCastMember ->
            val posterPath = castImagesMap[traktCastMember.person?.ids?.tmdb]
            Log.d(
                TAG,
                "getCredits: Got poster path for member ${traktCastMember.person?.name} // $posterPath"
            )

            castPeople.add(
                Pair(
                    ShowCastPersonData(
                        traktCastMember.person?.ids?.trakt ?: 0,
                        showTraktId,
                        index,
                        true,
                        traktCastMember.character
                    ),
                    CastPerson(
                        traktCastMember.person?.ids?.trakt ?: 0,
                        traktCastMember.person?.ids?.tmdb,
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