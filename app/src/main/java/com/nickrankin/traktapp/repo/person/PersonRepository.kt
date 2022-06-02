package com.nickrankin.traktapp.repo.person

import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.credits.CreditsDatabase
import com.nickrankin.traktapp.helper.PersonCreditsHelper
import com.nickrankin.traktapp.helper.networkBoundResource
import com.uwetrottmann.trakt5.enums.Type
import javax.inject.Inject

private const val TAG = "PersonRepository"
class PersonRepository @Inject constructor(private val traktApi: TraktApi, private val personCreditsHelper: PersonCreditsHelper, private val creditsDatabase: CreditsDatabase) {
    private val personDao = creditsDatabase.personDao()
    private val creditCharacterPersonDao = creditsDatabase.creditCharacterPersonDao()

    fun getPerson(personTraktId: Int, shouldRefresh: Boolean) = networkBoundResource(
        query = {
                personDao.getPerson(personTraktId)
        },
        fetch = {
            personCreditsHelper.getCredits(personTraktId)
        },
        shouldFetch = {person ->
            person != null || shouldRefresh
        },
        saveFetchResult = { person ->
            Log.d(TAG, "getPerson: Inserting person $person")
            if(person != null) {
                creditsDatabase.withTransaction {
                    personDao.insert(person)
                }
            }
        }
    )

    fun getPersonMovies(personTraktId: Int, shouldRefresh: Boolean) = networkBoundResource(
        query = {
            creditCharacterPersonDao.getPersonCredits(personTraktId, Type.MOVIE)
        },
        fetch = {
                personCreditsHelper.getPersonMovieCredits(personTraktId)
        },
        shouldFetch = { movieCastPeople ->
            movieCastPeople.isEmpty() || shouldRefresh
        },
        saveFetchResult = { movieCastPersonData ->


            creditsDatabase.withTransaction {
                creditCharacterPersonDao.insert(movieCastPersonData)
            }
        }
    )

    fun getPersonShows(personTraktId: Int, shouldRefresh: Boolean) = networkBoundResource(
        query = {
            creditCharacterPersonDao.getPersonCredits(personTraktId, Type.SHOW)
        },
        fetch = {
            personCreditsHelper.getPersonShowCredits(personTraktId)
        },
        shouldFetch = { showCast ->
            showCast.isEmpty() || shouldRefresh
        },
        saveFetchResult = { showCastData ->


            creditsDatabase.withTransaction {
                creditCharacterPersonDao.insert(showCastData)
            }
        }
    )
}