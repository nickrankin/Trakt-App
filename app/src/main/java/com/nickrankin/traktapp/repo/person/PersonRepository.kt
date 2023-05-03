package com.nickrankin.traktapp.repo.person

import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.dao.credits.CreditsDatabase
import com.nickrankin.traktapp.helper.PersonCreditsHelper
import com.nickrankin.traktapp.helper.networkBoundResource
import javax.inject.Inject

private const val TAG = "PersonRepository"
class PersonRepository @Inject constructor(private val personCreditsHelper: PersonCreditsHelper, private val creditsDatabase: CreditsDatabase) {
    private val castPersonDao = creditsDatabase.castPersonDao()
    private val crewPersonDao = creditsDatabase.crewPersonDao()
    private val creditsDao = creditsDatabase.personDao()

    fun getPerson(personTraktId: Int, shouldRefresh: Boolean) = networkBoundResource(
        query = {
            creditsDao.getPerson(personTraktId)
        },
        fetch = {
            personCreditsHelper.getCredits(personTraktId)
        },
        shouldFetch = { person ->
            shouldRefresh || person == null
        },
        saveFetchResult = { traktPerson ->
            
            if(traktPerson != null) {
                creditsDatabase.withTransaction {
                    creditsDao.insert(
                        traktPerson
                    )
                }
            } else {
                Log.e(TAG, "getPerson: Person with Trakt ID $personTraktId not existing")
            }

        }
    )

    fun getcastPersonCredits(personTraktId: Int, shouldRefresh: Boolean) = networkBoundResource(
        query = {
            castPersonDao.getCastPersonCredits(personTraktId)
        },
        fetch = {
                personCreditsHelper.getCastPersonCredits(personTraktId)
        },
        shouldFetch = { castPeople ->
            castPeople.isEmpty() || shouldRefresh
        },
        saveFetchResult = { castPeople ->
            creditsDatabase.withTransaction {
                castPersonDao.insert(castPeople)
            }
        }
    )

    fun getCrewPersonCredits(personTraktId: Int, shouldRefresh: Boolean) = networkBoundResource(
        query = {
            crewPersonDao.getCrewPersonCredits(personTraktId)
        },
        fetch = {
            personCreditsHelper.getCrewPersonCredits(personTraktId)
        },
        shouldFetch = { castPeople ->
            castPeople.isEmpty() || shouldRefresh
        },
        saveFetchResult = { castPeople ->
            creditsDatabase.withTransaction {
                crewPersonDao.insert(castPeople)
            }
        }
    )
}