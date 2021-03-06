package com.nickrankin.traktapp.repo.shows.suggested

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.helper.Resource
import com.uwetrottmann.trakt5.entities.SearchResult
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.enums.Extended
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

const val START_PAGE_INDEX = 1
const val LIMIT = 50
private const val TAG = "RecommendedShowsPagingS"
class RecommendedShowsRepository @Inject constructor(private val traktApi: TraktApi) {

    private var suggestedShows: List<Show> = listOf()

    suspend fun getSuggestedShows(shouldRefresh: Boolean) = flow {
        emit(Resource.Loading())
        if(!shouldRefresh && suggestedShows.isNotEmpty()) {
            Log.d(TAG, "getSuggestedShows: Returning cached shows")
            emit(Resource.Success(suggestedShows))
        } else {
            Log.d(TAG, "getSuggestedShows: Getting shows from Trakt ShouldRefresh $shouldRefresh")
            try {
                val response = traktApi.tmRecommendations().shows(
                    START_PAGE_INDEX, LIMIT, Extended.FULL)

                suggestedShows = response

                emit(Resource.Success(response))
            } catch (t: Throwable) {
                emit(Resource.Error(t, null))
            }
        }
    }


    suspend fun addToCollection(syncItems: SyncItems): Resource<SyncResponse> {
        return try {
            val result = traktApi.tmSync().addItemsToCollection(syncItems)
            Resource.Success(result)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun removeSuggestion(traktId: String): Pair<Boolean, Throwable?> {
        return try {
            val response = traktApi.tmRecommendations().dismissShow(traktId)

            Pair(true, null)
        } catch(t: Throwable) {
            Pair(false, t)
        }
    }




}