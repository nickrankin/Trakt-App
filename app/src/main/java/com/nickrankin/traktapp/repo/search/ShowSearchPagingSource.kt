package com.nickrankin.traktapp.repo.search

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.nickrankin.traktapp.api.TraktApi
import com.uwetrottmann.trakt5.entities.SearchResult
import com.uwetrottmann.trakt5.enums.Extended
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

private const val TAG = "ShowSearchPagingSource"
private const val START_PAGE_INDEX = 1
private const val PAGE_LIMIT = 15
class ShowSearchPagingSource constructor(val traktApi: TraktApi, var query: String): PagingSource<Int, SearchResult>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchResult> {
        val page = params.key ?: START_PAGE_INDEX

        return try {
            val response = traktApi.tmSearch().textQueryShow(query, null, null, null, null, null, null, null, null, null, Extended.FULL, page,
                PAGE_LIMIT)

            Log.d(TAG, "load: Got ${response.size} Results!")

            val nextKey = if(response.isEmpty()) {
                null
            } else {
                page + 1
            }

            LoadResult.Page(
                data = response,
                prevKey = if(page == START_PAGE_INDEX) null else page - 1,
                nextKey = nextKey
            )

        } catch(e: IOException) {
            LoadResult.Error(e)
        } catch (e: HttpException) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SearchResult>): Int? {
        // We need to get the previous key (or next key if previous is null) of the page
        // that was closest to the most recently accessed index.
        // Anchor position is the most recently accessed index
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }

    }
}