package com.nickrankin.traktapp.repo.lists

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.lists.ListWithEntries
import com.nickrankin.traktapp.dao.lists.TraktListsDatabase
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.dao.lists.model.TraktListEntry
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.ListEntry
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.entities.UserSlug
import com.uwetrottmann.trakt5.enums.Extended
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

private const val TAG = "TraktListsRepository"
class TraktListsRepository @Inject constructor(private val traktApi: TraktApi, private val sharedPreferences: SharedPreferences, private val listsDatabase: TraktListsDatabase, private val listEntryRepository: ListEntryRepository) {
    private val traktListsDao = listsDatabase.traktListDao()
    private val traktListEntriesDao = listsDatabase.listEntryDao()

    private val listsWithEntries = traktListEntriesDao.getAllListEntries()

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

    suspend fun getListsAndEntries(shouldRefresh: Boolean): Flow<List<ListWithEntries>> {
        val lists = traktListsDao.getAllTraktLists()

        if(lists.first().isEmpty() || shouldRefresh) {
            try {
                Log.d(TAG, "refreshAllListsAndListItems: Refreshing lists and it's entries")
                val lists = traktApi.tmUsers().lists(UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "NULL")))
                Log.d(TAG, "refreshAllListsAndListItems: Got ${lists.size} lists")
                val listItems: MutableMap<Int, List<ListEntry>> = mutableMapOf()


                lists.forEach { list->
                    listItems[list.ids?.trakt ?: 0] = traktApi.tmUsers().listItems(UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "NULL")), list.ids?.trakt?.toString() ?: "", Extended.FULL)
                    Log.e(TAG, "refreshAllListsAndListItems: ${listItems.get(list.ids.trakt)}", )
                }

                Log.d(TAG, "refreshAllListsAndListItems: List items contains ${listItems.size}")

                listsDatabase.withTransaction {
                    traktListsDao.deleteTraktListsFromCache()
                    traktListEntriesDao.deleteAllListEntriesFromCache()
                    
                    Log.d(TAG, "refreshAllListsAndListItems: Inserting ${lists.size} lists")
                    traktListsDao.insert(convertLists(lists))

                    lists.map { list ->
                        val listId =  list.ids?.trakt ?: 0
                        traktListEntriesDao.insertListEntries(listEntryRepository.getListEntries(listId, listItems.get(listId) ?: emptyList()))

                        Log.d(TAG, "refreshAllListsAndListItems: Inserted ${listItems.size} list entries for list ${list.name} ${list.ids.trakt}")
                    }
                }

            } catch(e: Exception) {
                e.printStackTrace()
            }
        }
        return listsWithEntries
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