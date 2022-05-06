package com.nickrankin.traktapp.repo.lists

import android.content.SharedPreferences
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.lists.TraktListsDatabase
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.entities.UserSlug
import javax.inject.Inject

class TraktListsRepository @Inject constructor(private val traktApi: TraktApi, private val sharedPreferences: SharedPreferences, private val listsDatabase: TraktListsDatabase) {
    private val traktListsDao = listsDatabase.traktListDao()

    fun getLists(shouldRefresh: Boolean) = networkBoundResource(
        query = {
                traktListsDao.getAllTraktLists()
        },
        fetch = {
                traktApi.tmUsers().lists(UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "NULL")))
        },
        shouldFetch = { traktLists ->
            shouldRefresh || traktLists.isEmpty()
        },
        saveFetchResult = { traktLists ->
            listsDatabase.withTransaction {
                traktListsDao.insert(convertLists(traktLists))
            }
        }
    )

    private fun convertLists(oldLists: List<com.uwetrottmann.trakt5.entities.TraktList>): List<TraktList> {
        val convertedLists: MutableList<TraktList> = mutableListOf()

        oldLists.map { list ->
            convertedLists.add(
                TraktList(
                    list.ids?.trakt ?: -1,
                    list.ids?.slug ?: "",
                    list?.name ?: "Untitled",
                    list.description,
                    list.created_at,
                    list.updated_at,
                    list.allow_comments,
                    list.comment_count,
                    list.display_numbers,
                    list.item_count,
                    list.likes,
                    list.privacy,
                    list.sort_by,
                    list.sort_how,
                    list.user
                )
            )
        }
        return convertedLists
    }

    suspend fun addTraktList(traktList: com.uwetrottmann.trakt5.entities.TraktList): Resource<TraktList> {
        return try {
            val response = traktApi.tmUsers().createList(UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "NULL")), traktList)

            // Need to convert the list to our dao model so we can cache it
            val convertedList = convertLists(listOf(response)).first()

            listsDatabase.withTransaction {
                traktListsDao.insertSingle(convertedList)
            }

            Resource.Success(convertedList)

        } catch(t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun editTraktList(traktList: com.uwetrottmann.trakt5.entities.TraktList, listSlug: String): Resource<TraktList> {
        return try {
            val response = traktApi.tmUsers().updateList(UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "NULL")), listSlug, traktList)

            // Need to convert the list to our dao model so we can cache it
            val convertedList = convertLists(listOf(response)).first()

            listsDatabase.withTransaction {
                traktListsDao.insertSingle(convertedList)
            }

            Resource.Success(convertedList)

        } catch(t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun deleteTraktList(listTraktId: String): Resource<Boolean> {
        return try {
            val response = traktApi.tmUsers().deleteList(UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "NULL")), listTraktId)

            listsDatabase.withTransaction {
                traktListsDao.deleteListById(listTraktId)
            }

            Resource.Success(true)

        } catch(t: Throwable) {
            Resource.Error(t, null)
        }
    }
}