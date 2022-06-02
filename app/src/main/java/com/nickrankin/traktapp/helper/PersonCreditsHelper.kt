package com.nickrankin.traktapp.helper

import android.util.Log
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.credits.MovieCastPerson
import com.nickrankin.traktapp.dao.credits.ShowCastPerson
import com.nickrankin.traktapp.dao.credits.model.CreditCharacterPerson
import com.nickrankin.traktapp.dao.credits.model.MovieCastPersonData
import com.nickrankin.traktapp.dao.credits.model.Person
import com.nickrankin.traktapp.dao.credits.model.ShowCastPersonData
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.Type
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PersonCreditsHelper"

@Singleton
class PersonCreditsHelper @Inject constructor(
    private val traktApi: TraktApi,
    private val tmdbApi: TmdbApi
) {
    suspend fun getCredits(personTraktId: Int): Person? {
        Log.d(TAG, "getCredits: Getting Credits")
        return try {
            val traktPersonResponse =
                traktApi.tmPeople().summary(personTraktId.toString(), Extended.FULL)
            val tmdbPerson =
                tmdbApi.tmPersonService().summary(traktPersonResponse.ids?.tmdb ?: 0, null)

            Log.d(
                TAG,
                "getCredits: Found Trakt: $traktPersonResponse, Found Tmdb $traktPersonResponse"
            )
            Person(
                traktPersonResponse.ids?.trakt ?: 0,
                traktPersonResponse.ids?.tmdb,
                traktPersonResponse.ids?.imdb,
                traktPersonResponse.name ?: "",
                traktPersonResponse.biography,
                traktPersonResponse.birthplace,
                traktPersonResponse.birthday,
                traktPersonResponse.death,
                traktPersonResponse.homepage,
                tmdbPerson.profile_path
            )
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    suspend fun getPersonMovieCredits(traktPersonId: Int): List<CreditCharacterPerson> {
        val movieCredits: MutableList<CreditCharacterPerson> = mutableListOf()

        try {
            val personMovieCredits = traktApi.tmPeople().movieCredits(traktPersonId.toString(), null)
            Log.d(
                TAG,
                "getPersonMovieCredits: Got ${personMovieCredits.cast.size} Credits from Trakt"
            )

            personMovieCredits.cast.mapIndexed { index, castMember ->
                movieCredits.add(
                    CreditCharacterPerson(
                        "P${traktPersonId}M${castMember.movie?.ids?.trakt ?: 0}",
                        traktPersonId,
                        castMember.movie?.ids?.trakt ?: 0,
                        castMember.movie?.ids?.tmdb ?: 0,
                        castMember.movie?.title,
                        castMember.movie?.year,
                        index,
                        castMember.character,
                        Type.MOVIE
                    )
                )
            }

        } catch (t: Throwable) {
            t.printStackTrace()
        }

        Log.d(TAG, "getPersonMovieCredits: Returning ${movieCredits.size} Credits")
        return movieCredits
    }

    suspend fun getPersonShowCredits(traktPersonId: Int): List<CreditCharacterPerson> {
        val showCredits: MutableList<CreditCharacterPerson> = mutableListOf()

        try {
            val personShowCredits = traktApi.tmPeople().showCredits(traktPersonId.toString())

            Log.d(TAG, "getPersonShowCredits: Got ${personShowCredits.cast.size} from Trakt")

            personShowCredits.cast.mapIndexed { index, castMember ->
                showCredits.add(
                    CreditCharacterPerson(
                        "P${traktPersonId}s${castMember.show?.ids?.trakt ?: 0}",
                        traktPersonId,
                        castMember.show?.ids?.trakt ?: 0,
                        castMember.show?.ids?.tmdb,
                        castMember.show?.title ?: "",
                        castMember.show?.year,
                        index,
                        castMember.character,
                        Type.SHOW
                    )
                )
            }

        } catch (t: Throwable) {
            t.printStackTrace()
        }
        Log.d(TAG, "getPersonShowCredits: Returning ${showCredits.size} Credits")
        return showCredits
    }

    suspend fun getMovieCredits(movieTraktId: Int, movieTmdbId: Int?): List<MovieCastPerson> {
        Log.d(TAG, "getMovieCredits: Getting Movie Credits")
        return try {
            val movieCreditsList: MutableList<MovieCastPerson> = mutableListOf()

            val traktResponse = traktApi.tmMovies().people(movieTraktId.toString())
            val tmdbResponse =
                if (movieTmdbId != null) tmdbApi.tmMovieService().credits(movieTmdbId) else null

            Log.d(
                TAG,
                "getMovieCredits: Got ${traktResponse.cast?.size} from Trakt, From TMDB ${tmdbResponse?.cast?.size}"
            )

            traktResponse.cast?.mapIndexed { index, castPerson ->

                val person = castPerson.person
                val movie = castPerson.movie

                var profileImagePath: String? = ""

                val traktPersonId = castPerson.person?.ids?.trakt
                val tmdbPersonId = castPerson.person?.ids?.tmdb
                val character = castPerson.character

                if (tmdbResponse != null) {
                    val tmdbCastMember = tmdbResponse.cast?.find { it.id == tmdbPersonId }

                    if (tmdbCastMember?.profile_path != null) {
                        profileImagePath = tmdbCastMember.profile_path
                    }
                }

                val movieCastPersonData = MovieCastPersonData(
                        "P${traktPersonId}M${movieTraktId}",
                        traktPersonId ?: 0,
                    movieTraktId,
                    movieTmdbId,
                        index,
                        character
                    )

                val personData = Person(
                        person?.ids?.trakt ?: 0,
                        person?.ids?.tmdb,
                        person?.ids?.imdb,
                        person?.name ?: "",
                        person?.biography,
                        person?.birthplace,
                        person?.birthday,
                        person?.death,
                        person?.homepage,
                        profileImagePath
                    )

                movieCreditsList.add(
                    MovieCastPerson(movieCastPersonData, personData)
                )
            }


            Log.d(TAG, "getMovieCredits: Returning ${movieCreditsList.size}")

            movieCreditsList

        } catch (t: Throwable) {
            t.printStackTrace()
            emptyList()
        }
    }

    suspend fun getShowCredits(showTraktId: Int, showTmdbId: Int?): List<ShowCastPerson> {
        Log.d(TAG, "getShowCredits: Getting Show Credit")
        val showCreditsList: MutableList<ShowCastPerson> = mutableListOf()

        try {
            val traktResponse = traktApi.tmShows().people(showTraktId.toString(), "guest_stars")
            val tmdbResponse =
                if (showTmdbId != null) tmdbApi.tmTvService().credits(showTmdbId, null) else null

            Log.d(
                TAG,
                "getShowCredits: Got from Trakt ${traktResponse.cast?.size} , From TMDB ${tmdbResponse?.cast?.size}"
            )

            traktResponse.cast?.mapIndexed { index, castPerson ->
                val person = castPerson.person

                var profileImagePath: String? = ""

                val traktPersonId = castPerson.person?.ids?.trakt
                val tmdbPersonId = castPerson.person?.ids?.tmdb
                val character = castPerson.character

                if (tmdbResponse != null) {
                    val tmdbCastMember = tmdbResponse.cast?.find { it.id == tmdbPersonId }

                    if (tmdbCastMember?.profile_path != null) {
                        profileImagePath = tmdbCastMember.profile_path
                    }
                }

                showCreditsList.add(
                    ShowCastPerson(
                        ShowCastPersonData(
                            "P${traktPersonId}S${showTraktId}",
                            traktPersonId ?: 0,
                            showTraktId,
                            castPerson.show?.ids?.tmdb,
                            index,
                            false,
                            character
                        ),
                        Person(
                            person?.ids?.trakt ?: 0,
                            person?.ids?.tmdb,
                            person?.ids?.imdb,
                            person?.name ?: "",
                            person?.biography,
                            person?.birthplace,
                            person?.birthday,
                            person?.death,
                            person?.homepage,
                            profileImagePath
                        )
                    )
                )
            }

            traktResponse.guest_stars?.mapIndexed { index, castPerson ->
                val person = castPerson.person

                var profileImagePath: String? = ""

                val traktPersonId = castPerson.person?.ids?.trakt
                val tmdbPersonId = castPerson.person?.ids?.tmdb
                val character = castPerson.character

                if (tmdbResponse != null) {
                    val tmdbCastMember = tmdbResponse.cast?.find { it.id == tmdbPersonId }

                    if (tmdbCastMember?.profile_path != null) {
                        profileImagePath = tmdbCastMember.profile_path
                    }
                }

                showCreditsList.add(
                    ShowCastPerson(
                        ShowCastPersonData(
                            "P${traktPersonId}S${showTraktId}",
                            traktPersonId ?: 0,
                            showTraktId,
                            castPerson.show?.ids?.tmdb,
                            index,
                            true,
                            character
                        ),
                        Person(
                            person?.ids?.trakt ?: 0,
                            person?.ids?.tmdb,
                            person?.ids?.imdb,
                            person?.name ?: "",
                            person?.biography,
                            person?.birthplace,
                            person?.birthday,
                            person?.death,
                            person?.homepage,
                            profileImagePath
                        )
                    )
                )
            }

            Log.d(TAG, "getShowCredits: Returning ${showCreditsList.size}")

            return showCreditsList

        } catch (t: Throwable) {
            t.printStackTrace()
        }

        return emptyList()
    }


}