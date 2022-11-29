package com.nickrankin.traktapp.helper

import android.util.Log
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.credits.MovieCastPerson
import com.nickrankin.traktapp.dao.credits.ShowCastPerson
import com.nickrankin.traktapp.dao.credits.model.TmCastPerson
import com.nickrankin.traktapp.dao.credits.model.Person
import com.nickrankin.traktapp.dao.credits.model.TmCrewPerson
import com.uwetrottmann.tmdb2.entities.CastMember
import com.uwetrottmann.tmdb2.entities.Credits
import com.uwetrottmann.tmdb2.entities.CrewMember
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
        val traktApiResponse = traktApi.tmPeople().summary(personTraktId.toString(), Extended.FULL)

        var profileImage: String? = ""
        if (traktApiResponse.ids?.tmdb != null) {
            val tmdbPersonData = getPersonTmdbData(traktApiResponse.ids.tmdb)

            profileImage = tmdbPersonData?.profile_path
        }

        return if (traktApiResponse.name != null) {
            Person(
                traktApiResponse.ids?.trakt ?: 0,
                traktApiResponse.ids?.tmdb,
                traktApiResponse.ids?.imdb,
                traktApiResponse.name ?: "",
                traktApiResponse.biography,
                traktApiResponse.birthplace,
                traktApiResponse.birthday,
                traktApiResponse.death,
                traktApiResponse.homepage,
                profileImage
            )
        } else {
            null
        }
    }

    private suspend fun getPersonTmdbData(tmdbId: Int): com.uwetrottmann.tmdb2.entities.Person? {
        return try {
            tmdbApi.tmPersonService().summary(tmdbId, getTmdbLanguage(null))
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    suspend fun getCastPersonCredits(traktPersonId: Int): List<TmCastPerson> {
        return getCastPersonMovieCredits(traktPersonId).plus(getCastPersonShowCredits(traktPersonId))
    }

    suspend fun getCrewPersonCredits(traktPersonId: Int): List<TmCrewPerson> {
        return getCrewPersonMovieCredits(traktPersonId).plus(getCrewPersonShowCredits(traktPersonId))

    }

    private suspend fun getCrewPersonMovieCredits(traktPersonId: Int): List<TmCrewPerson> {
        val movieCredits: MutableList<TmCrewPerson> = mutableListOf()

        try {
            val personMovieCredits =
                traktApi.tmPeople().movieCredits(traktPersonId.toString(), null)

            personMovieCredits.crew?.directing?.mapIndexed { index, crewMember ->
                movieCredits.add(
                    TmCrewPerson(
                        "P${traktPersonId}s${crewMember.movie?.ids?.trakt ?: 0}",
                        traktPersonId,
                        crewMember.movie?.ids?.trakt ?: 0,
                        crewMember.movie?.ids?.tmdb,
                        crewMember.movie?.title ?: "",
                        crewMember.movie?.year,
                        index,
                        crewMember.person?.name,
                        Type.MOVIE,
                        crewMember.job
                    )
                )
            }

            personMovieCredits.crew?.writing?.mapIndexed { index, crewMember ->
                movieCredits.add(
                    TmCrewPerson(
                        "P${traktPersonId}s${crewMember.movie?.ids?.trakt ?: 0}",
                        traktPersonId,
                        crewMember.movie?.ids?.trakt ?: 0,
                        crewMember.movie?.ids?.tmdb,
                        crewMember.show?.title ?: "",
                        crewMember.movie?.year,
                        index,
                        crewMember.person?.name,
                        Type.MOVIE,
                        crewMember.job
                    )
                )
            }

        } catch (t: Throwable) {
            t.printStackTrace()
        }

        Log.d(TAG, "getPersonMovieCredits: Returning ${movieCredits.size} Credits")
        return movieCredits
    }

    private suspend fun getCrewPersonShowCredits(traktPersonId: Int): List<TmCrewPerson> {
        val showCredits: MutableList<TmCrewPerson> = mutableListOf()

        try {
            val personShowCredits = traktApi.tmPeople().showCredits(traktPersonId.toString())

            personShowCredits.crew?.directing?.mapIndexed { index, crewMember ->
                showCredits.add(
                    TmCrewPerson(
                        "P${traktPersonId}s${crewMember.show?.ids?.trakt ?: 0}",
                        traktPersonId,
                        crewMember.show?.ids?.trakt ?: 0,
                        crewMember.show?.ids?.tmdb,
                        crewMember.show?.title ?: "",
                        crewMember.show?.year,
                        index,
                        crewMember.person?.name,
                        Type.SHOW,
                        crewMember.job
                    )
                )
            }

            personShowCredits.crew?.writing?.mapIndexed { index, crewMember ->

                showCredits.add(
                    TmCrewPerson(
                        "P${traktPersonId}s${crewMember.show?.ids?.trakt ?: 0}",
                        traktPersonId,
                        crewMember.show?.ids?.trakt ?: 0,
                        crewMember.show?.ids?.tmdb,
                        crewMember.show?.title ?: "",
                        crewMember.show?.year,
                        index,
                        crewMember.person?.name,
                        Type.SHOW,
                        crewMember.job
                    )
                )
            }

        } catch (t: Throwable) {
            t.printStackTrace()
        }
        Log.d(TAG, "getPersonShowCredits: Returning ${showCredits.size} Credits")

        return showCredits
    }

    private suspend fun getCastPersonMovieCredits(traktPersonId: Int): List<TmCastPerson> {
        val movieCredits: MutableList<TmCastPerson> = mutableListOf()

        try {
            val personMovieCredits =
                traktApi.tmPeople().movieCredits(traktPersonId.toString(), null)

            personMovieCredits.cast.mapIndexed { index, castMember ->
                movieCredits.add(
                    TmCastPerson(
                        "P${traktPersonId}M${castMember.movie?.ids?.trakt ?: 0}",
                        traktPersonId,
                        castMember.movie?.ids?.trakt ?: 0,
                        castMember.movie?.ids?.tmdb ?: 0,
                        castMember.movie?.title,
                        castMember.movie?.year,
                        index,
                        Type.MOVIE,
                        castMember.character
                    )
                )
            }



        } catch (t: Throwable) {
            t.printStackTrace()
        }

        Log.d(TAG, "getPersonMovieCredits: Returning ${movieCredits.size} Credits")
        return movieCredits
    }

    private suspend fun getCastPersonShowCredits(traktPersonId: Int): List<TmCastPerson> {
        val showCredits: MutableList<TmCastPerson> = mutableListOf()

        try {
            val personShowCredits = traktApi.tmPeople().showCredits(traktPersonId.toString())

            personShowCredits.cast.mapIndexed { index, castMember ->
                showCredits.add(
                    TmCastPerson(
                        "p${traktPersonId}m${castMember.show?.ids?.trakt ?: 0}",
                        traktPersonId,
                        castMember.show?.ids?.trakt ?: 0,
                        castMember.show?.ids?.tmdb,
                        castMember.show?.title ?: "",
                        castMember.show?.year,
                        index,
                        Type.SHOW,
                        castMember.character
                    )
                )
            }

        } catch (t: Throwable) {
            t.printStackTrace()
        }
        Log.d(TAG, "getPersonShowCredits: Returning ${showCredits.size} Credits")

        return showCredits
    }

    suspend fun getMovieCast(movieTraktId: Int, movieTmdbId: Int?): List<MovieCastPerson> {
        return try {
            val movieCreditsList: MutableList<MovieCastPerson> = mutableListOf()

            val traktResponse = traktApi.tmMovies().people(movieTraktId.toString())
            val tmdbResponse =
                if (movieTmdbId != null) tmdbApi.tmMovieService().credits(movieTmdbId) else null

            traktResponse.cast?.mapIndexed { index, castPerson ->

                val person = castPerson.person
                val movie = castPerson.movie


                val traktPersonId = castPerson.person?.ids?.trakt
                val tmdbPersonId = castPerson.person?.ids?.tmdb
                val character = castPerson.character


                movieCreditsList.add(
                    MovieCastPerson(
                        "P${traktPersonId}M${movieTraktId}",
                        traktPersonId ?: 0,
                        movieTraktId,
                        movieTmdbId,
                        index,
                        character,
                        person?.name ?: "",
                        getTmdbCastPersonProfilePath(tmdbResponse, tmdbPersonId)
                    )
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
        val showCreditsList: MutableList<ShowCastPerson> = mutableListOf()

        try {
            val traktResponse = traktApi.tmShows().people(showTraktId.toString(), null)
            val tmdbResponse =
                if (showTmdbId != null) tmdbApi.tmTvService().credits(showTmdbId, null) else null


            traktResponse.cast?.mapIndexed { index, castPerson ->
                val person = castPerson.person


                val traktPersonId = castPerson.person?.ids?.trakt
                val tmdbPersonId = castPerson.person?.ids?.tmdb
                val character = castPerson.character


                showCreditsList.add(
                    ShowCastPerson(
                        "P${traktPersonId}S${showTraktId}",
                        traktPersonId ?: 0,
                        showTraktId,
                        castPerson.show?.ids?.tmdb,
                        index,
                        character,
                        person?.name ?: "",
                        getTmdbCastPersonProfilePath(tmdbResponse, tmdbPersonId),
                        false,
                        null,
                        null
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

    suspend fun getEpisodeGuestCredits(showTraktId: Int, showTmdbId: Int?, seasonNumber: Int, episodeNumber: Int): List<ShowCastPerson> {
        try {
            val showCreditsList: MutableList<ShowCastPerson> = mutableListOf()

            val traktCreditsResponse = traktApi.tmEpisodes().people(showTraktId.toString(), seasonNumber, episodeNumber, "guest_stars")
            val tmdbResponse = tmdbApi.tmTvEpisodesService().credits(showTmdbId ?: 0, seasonNumber, episodeNumber)

            traktCreditsResponse.guest_stars?.mapIndexed { index, castPerson ->
                val person = castPerson.person

                val traktPersonId = castPerson.person?.ids?.trakt
                val tmdbPersonId = castPerson.person?.ids?.tmdb
                val character = castPerson.character


                showCreditsList.add(
                    ShowCastPerson(
                        "P${traktPersonId}S${showTraktId}",
                        traktPersonId ?: 0,
                        showTraktId,
                        castPerson.show?.ids?.tmdb,
                        index,
                        character,
                        person?.name ?: "",
                        getTmdbCastPersonProfilePath(tmdbResponse, tmdbPersonId),
                        true,
                        seasonNumber,
                        episodeNumber,
                    )
                )
            }

            Log.d(TAG, "getShowCredits: Returning ${showCreditsList.size}")

            return showCreditsList

        } catch(e: Exception) {

        }
        return emptyList()
    }

    private suspend fun getTmdbCrewPersonProfilePath(tmdbResponse: Credits?, personTmdbId: Int?): String? {
        if(personTmdbId == null) {
            Log.e(TAG, "getTmdbPerson: Cuodn't get Tmdb Person", )
            return null
        }

        // Try to locate current person in TMDB Cast
        var tmdbCrewMember: CrewMember? = tmdbResponse?.crew?.find { it.id == personTmdbId }

        // Person data was not included in the TMDB response, try manual person lookup since we have a TMDB ID at this point
        if(tmdbCrewMember == null) {
            // Cast person still not found, look up person on TMDB
            return fetchTmdbPerson(personTmdbId)?.profile_path
        }

        return tmdbCrewMember.profile_path
    }

    private suspend fun getTmdbCastPersonProfilePath(tmdbResponse: Credits?, personTmdbId: Int?): String? {
        if(personTmdbId == null) {
            Log.e(TAG, "getTmdbPerson: Cuodn't get Tmdb Person", )
            return null
        }

        // Try to locate current person in TMDB Cast
        var tmdbCastMember: CastMember? = tmdbResponse?.cast?.find { it.id == personTmdbId }

        // Person is not a main Cast member, check if person is a guest star
        if(tmdbCastMember == null) {
            tmdbCastMember = tmdbResponse?.guest_stars?.find { it.id == personTmdbId }
        }

        // Person data was not included in the TMDB response, try manual person lookup since we have a TMDB ID at this point
        if(tmdbCastMember == null) {
            // Cast person still not found, look up person on TMDB
            return fetchTmdbPerson(personTmdbId)?.profile_path
        }

        return tmdbCastMember.profile_path
    }

    private suspend fun fetchTmdbPerson(tmdbId: Int?): com.uwetrottmann.tmdb2.entities.Person? {
        if(tmdbId == null) {
            return null
        }

        try {
            return tmdbApi.tmPersonService().summary(tmdbId, getTmdbLanguage(null))
        } catch(e: Exception) {
            Log.e(TAG, "getTmdbPerson: Error getting person ${e.message}", )

            e.printStackTrace()
        }

        return null
    }
}