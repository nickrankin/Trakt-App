package com.nickrankin.traktapp.helper

import android.util.Log
import com.nickrankin.traktapp.api.TraktApi
import com.uwetrottmann.trakt5.entities.Person
import com.uwetrottmann.trakt5.enums.IdType
import com.uwetrottmann.trakt5.enums.Type
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TmdbToTraktIdHelper"
@Singleton
class  TmdbToTraktIdHelper @Inject constructor(private val traktApi: TraktApi) {
    init {
        Log.d(TAG, "Getting Instance ${this.javaClass.hashCode()}: ")
    }

    suspend fun getTraktPersonByTmdbId(tmdbId: Int): Person? {
        try {
            val lookupResponse = traktApi.tmSearch().idLookup(IdType.TMDB, tmdbId.toString(), Type.PERSON, null, 1, 1)

            if(lookupResponse.isNotEmpty()) {
                return lookupResponse.first().person
            }

        } catch(e: Exception) {
            e.printStackTrace()
        }

        return null
    }

}