package com.nickrankin.traktapp.helper

import android.util.Log
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.api.services.trakt.model.TmCredits
import com.nickrankin.traktapp.dao.credits.model.CastPerson
import com.nickrankin.traktapp.dao.credits.model.CastPersonWithData
import com.nickrankin.traktapp.dao.credits.model.ShowCastPersonData
import com.uwetrottmann.tmdb2.entities.Credits
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
    ): List<CastPersonWithData> {
        Log.d(TAG, "getCredits: Getting cast credits")
        val castPeople: MutableList<CastPersonWithData> = mutableListOf()


        if (showTmdbId != null && showTmdbId != 0 && showTmdbId != -1) {
            Log.d(TAG, "getShowCredits: Getting credits for TMDB id $showTmdbId")
            // we get the cast data from TMDB
            castPeople.addAll(getCastDataFromTmdbCredits(showTraktId, getCastMembersFromTmdb(showTmdbId)))
        }

        if(castPeople.isEmpty()) {
            Log.d(TAG, "getShowCredits: No data from TMDB, fallback to Trakt")
            // No data from TMDB, so fall back to trakt
            castPeople.addAll(getCastDataFromTraktCredits(showTraktId, getCastMembersDataFromTrakt(showTraktId.toString()), false))
        } else {
            Log.d(TAG, "getShowCredits: Getting guest stars from Trakt")
            // Only get the Guest Stars data from Trakt (TMDB API only provides Guest Star data from Season endpoints)
            castPeople.addAll(getCastDataFromTraktCredits(showTraktId, getCastMembersDataFromTrakt(showTraktId.toString()), true))
        }

        Log.d(TAG, "getShowCredits: Returning ${castPeople.size} cast members")

        return castPeople
    }

    private suspend fun getCastMembersDataFromTrakt(showTraktId: String): TmCredits? {
        return try {
            traktApi.tmShows().people(showTraktId, "full,guest_stars")
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getCastMembersFromTmdb(showTmdbId: Int): Credits? {
        return try {
            tmdbApi.tmTvService().credits(showTmdbId, getTmdbLanguage(null))
        } catch (e: Exception) {
            Log.e(TAG, "getCredits: Failed to get credits from TMDB ${e.localizedMessage}")
            e.printStackTrace()
            null
        }
    }

    private fun getCastDataFromTmdbCredits(
        showTraktId: Int,
        credits: Credits?
    ): List<CastPersonWithData> {
        val castPersonList: MutableList<CastPersonWithData> = mutableListOf()

        credits?.cast?.map { castMember ->
            castPersonList.add(
                CastPersonWithData(
                    ShowCastPersonData(
                        "tmdb_$showTraktId-${castMember.id}",
                        "tmdb_${castMember.id}",
                        showTraktId,
                        0,
                        false,
                        castMember.character
                    ),
                    CastPerson(
                        "tmdb_${castMember.id}",
                        "tmdb_${castMember.id}",
                        null,
                        null,
                        null,
                        castMember.name,
                        castMember.profile_path
                    )
                )
            )
        }
        return castPersonList
    }

    private fun getCastDataFromTraktCredits(showTraktId: Int, credits: TmCredits?, onlyGuest: Boolean): List<CastPersonWithData> {
        val castPersonList: MutableList<CastPersonWithData> = mutableListOf()

        if(!onlyGuest) {
            credits?.cast?.map { castMember ->
                castPersonList.add(
                    CastPersonWithData(
                        ShowCastPersonData(
                            "trakt_$showTraktId-${castMember.person?.ids?.trakt}",
                            "trakt_${castMember.person?.ids?.trakt}",
                            showTraktId,
                            0,
                            false,
                            castMember.character
                        ),
                        CastPerson(
                            "trakt_${castMember.person?.ids?.trakt}",
                            "trakt_${castMember.person?.ids?.trakt}",
                            null,
                            castMember.person?.ids?.imdb,
                            null,
                            castMember.person.name,
                            null
                        )
                    )
                )
            }
        }

        credits?.guest_stars?.map { guestStar ->
            castPersonList.add(
                CastPersonWithData(
                    ShowCastPersonData(
                        "trakt_$showTraktId-${guestStar.person?.ids?.trakt}",
                        "trakt_${guestStar.person?.ids?.trakt}",
                        showTraktId,
                        0,
                        true,
                        guestStar.character
                    ),
                    CastPerson(
                        "trakt_${guestStar.person?.ids?.trakt}",
                        "trakt_${guestStar.person?.ids?.trakt}",
                        null,
                        guestStar.person?.ids?.imdb,
                        null,
                        guestStar.person.name,
                        null
                    )
                )
            )
        }

        return castPersonList
    }
}