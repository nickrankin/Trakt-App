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
import kotlinx.coroutines.flow.*
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

const val START_PAGE_INDEX = 1
const val LIMIT = 50
private const val TAG = "RecommendedShowsPagingS"
class RecommendedShowsRepository @Inject constructor(private val traktApi: TraktApi) {
    private val suggestedShowsList: MutableList<Show> = mutableListOf()
    private val _suggestedShowsStateFlow: MutableStateFlow<Resource<List<Show>>> = MutableStateFlow(Resource.Loading())
    private val suggestShowsFlow = _suggestedShowsStateFlow.asStateFlow()


    suspend fun getSuggestedShows(shouldRefresh: Boolean): Flow<Resource<List<Show>>> {
        Log.d(TAG, "getSuggestedShows: Loading")

        if(shouldRefresh || suggestedShowsList.isEmpty()) {
            _suggestedShowsStateFlow.update { it -> Resource.Loading() }

            Log.d(TAG, "getSuggestedShows: Getting shows from Trakt ShouldRefresh $shouldRefresh")
            try {
                val response = traktApi.tmRecommendations().shows(
                    START_PAGE_INDEX, LIMIT, Extended.FULL)

                suggestedShowsList.clear()
                suggestedShowsList.addAll(response)

                _suggestedShowsStateFlow.update { Resource.Success(suggestedShowsList) }

            } catch (t: Throwable) {
                _suggestedShowsStateFlow.update { Resource.Error(t, suggestedShowsList) }

            }
        } else {
            _suggestedShowsStateFlow.update { Resource.Success(suggestedShowsList) }

        }


//        }

        return suggestShowsFlow
    }


    suspend fun addToCollection(syncItems: SyncItems): Resource<SyncResponse> {
        return try {
            val result = traktApi.tmSync().addItemsToCollection(syncItems)
            Resource.Success(result)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun removeSuggestion(show: Show): Resource<Boolean> {
        return try {
            // Perform the removal
            traktApi.tmRecommendations().dismissShow(show.ids?.trakt?.toString())
            suggestedShowsList.remove(show)

            _suggestedShowsStateFlow.update { Resource.Success(suggestedShowsList) }

            Resource.Success(true)
        } catch(t: Throwable) {
            Resource.Error(t, false)

        }
    }
}