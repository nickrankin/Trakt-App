package com.nickrankin.traktapp.repo.lists

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.base_entity.EpisodeBaseEnity
import com.nickrankin.traktapp.dao.base_entity.MovieBaseEntity
import com.nickrankin.traktapp.dao.base_entity.PersonBaseEntity
import com.nickrankin.traktapp.dao.base_entity.ShowBaseEntity
import com.nickrankin.traktapp.dao.lists.TraktListsDatabase
import com.nickrankin.traktapp.dao.lists.model.ListEntry
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.dao.lists.model.TraktListAndEntries
import com.nickrankin.traktapp.dao.lists.model.TraktListEntry
import com.nickrankin.traktapp.dao.refresh.LastRefreshedAt
import com.nickrankin.traktapp.dao.refresh.RefreshType
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.helper.shouldRefresh
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.ListPrivacy
import com.uwetrottmann.trakt5.enums.SortBy
import com.uwetrottmann.trakt5.enums.SortHow
import com.uwetrottmann.trakt5.enums.Type
import kotlinx.coroutines.flow.*
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject
import kotlin.reflect.typeOf

private const val TAG = "ListsRepository"

class ListsRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val sharedPreferences: SharedPreferences,
    private val listsDatabase: TraktListsDatabase
) {
    private val traktLIstsDao = listsDatabase.traktListDao()
    private val listEntryDao = listsDatabase.listEntryDao()

    private val lastRefreshedAtDao = listsDatabase.lastRereshedAtDao()

    private val userSlug = UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "NULL"))

//    fun getWatchlist(shouldFetch: Boolean) = networkBoundResource(
//        query = {
//             listEntryDao.getTraktListEntriesById(WATCHLIST_ID)
//        },
//        fetch = {
//        },
//        shouldFetch = {
//            shouldFetch || shouldRefresh(
//                lastRefreshedAtDao.getLastRefreshed(RefreshType.WATCHLIST).first(), null)
//        },
//        saveFetchResult = { listEntries ->
//            insertListItems(WATCHLIST_ID, listEntries)
//
//            listsDatabase.withTransaction {
//                lastRefreshedAtDao.insertLastRefreshStats(
//                    LastRefreshedAt(
//                        RefreshType.WATCHLIST,
//                        OffsetDateTime.now()
//                    )
//                )
//            }
//        }
//    )

    fun getLists(shouldFetch: Boolean) = networkBoundResource(
        query = {
            traktLIstsDao.getAllTraktLists()
        },
        fetch = {
            traktApi.tmUsers().lists(userSlug)
        },
        shouldFetch = { lists ->
            shouldFetch || shouldRefresh(
                lastRefreshedAtDao.getLastRefreshed(RefreshType.LISTS).first(), null
            )
        },
        saveFetchResult = { lists ->

            // Watch list is handled seperately. This will get both list and list items, inserting entries into db
            val watchedList = getWatchedList()

            val listsToBeAdded: MutableList<com.nickrankin.traktapp.dao.lists.model.TraktList> =
                mutableListOf()
            listsToBeAdded.add(watchedList)
            listsToBeAdded.addAll(convertLists(lists))

            listsDatabase.withTransaction {
                traktLIstsDao.deleteTraktListsFromCache()
                traktLIstsDao.insert(listsToBeAdded)
            }

            listsDatabase.withTransaction {
                lastRefreshedAtDao.insertLastRefreshStats(
                    LastRefreshedAt(
                        RefreshType.LISTS,
                        OffsetDateTime.now()
                    )
                )
            }
        }
    )

    fun getLists(sortBy: String, sortHow: String, shouldFetch: Boolean) = networkBoundResource(
        query = {
            val query =
                SimpleSQLiteQuery("SELECT * FROM lists ORDER BY $sortBy COLLATE NOCASE $sortHow")
            Log.d(TAG, "getLists: Sorting ${query.sql}")
            traktLIstsDao.getSortedTraktLists(query)
        },
        fetch = {
            traktApi.tmUsers().lists(userSlug)
        },
        shouldFetch = { lists ->
            shouldFetch || shouldRefresh(
                lastRefreshedAtDao.getLastRefreshed(RefreshType.LISTS).first(), null
            )
        },
        saveFetchResult = { lists ->

            // Watch list is handled seperately. This will get both list and list items, inserting entries into db
            val watchedList = getWatchedList()

            val listsToBeAdded: MutableList<com.nickrankin.traktapp.dao.lists.model.TraktList> =
                mutableListOf()
            listsToBeAdded.add(watchedList)
            listsToBeAdded.addAll(convertLists(lists))

            listsDatabase.withTransaction {
                traktLIstsDao.deleteTraktListsFromCache()
                traktLIstsDao.insert(listsToBeAdded)
            }

            listsDatabase.withTransaction {
                lastRefreshedAtDao.insertLastRefreshStats(
                    LastRefreshedAt(
                        RefreshType.LISTS,
                        OffsetDateTime.now()
                    )
                )
            }
        }
    )

    suspend fun reorderList(
        traktList: com.nickrankin.traktapp.dao.lists.model.TraktList?,
        newSortBy: SortBy): Throwable? {
        if(traktList == null) {
            Log.e(TAG, "reorderList: Trakt List cannot be null!", )
            return null
        }

        val newSortHow = if(traktList.sortBy == newSortBy) {
            // Sorting by same criteria, so flip the SortHow filtering
            if(traktList.sortHow == SortHow.DESC) {
                SortHow.ASC
            } else {
                SortHow.DESC
            }
        } else {
            // New sort criteria specified, so retain existing SortHow
            traktList.sortHow
        }

        // Apply the new changes to our TraktList
        traktList.apply {
            sortBy = newSortBy
            sortHow = newSortHow
        }

        try {
            // Perform the update
            traktApi.tmUsers().updateList(userSlug, traktList.trakt_id.toString(), getUpdatedUweTraktList(traktList))

            // Update the cached list
            listsDatabase.withTransaction {
                traktLIstsDao.update(traktList)
            }

            return null
        } catch (t: Throwable) {
            return t
        }
    }

    private fun getUpdatedUweTraktList(traktList: TraktList): com.uwetrottmann.trakt5.entities.TraktList {
        return TraktList().apply {
            name = traktList.name
            id(ListIds.trakt(traktList.trakt_id))
            sort_by = traktList.sortBy
            sort_how = traktList.sortHow
            privacy = traktList.privacy
            allow_comments = traktList.allow_comments
            display_numbers = traktList.display_numbers
            description = traktList.description
        }
    }

    fun getListById(traktListId: Int?, shouldFetch: Boolean) = networkBoundResource(
        query = {
            traktLIstsDao.getTraktList(traktListId ?: 0)
        },
        fetch = {
            traktApi.tmUsers().lists(userSlug)
        },
        shouldFetch = { lists ->
            traktListId != null && (shouldFetch || shouldRefresh(
                lastRefreshedAtDao.getLastRefreshed(RefreshType.LISTS).first(), null
            ))
        },
        saveFetchResult = { lists ->
            // Watch list is handled seperately. This will get both list and list items, inserting entries into db
            val watchedList = getWatchedList()

            val listsToBeAdded: MutableList<com.nickrankin.traktapp.dao.lists.model.TraktList> =
                mutableListOf()
            listsToBeAdded.add(watchedList)
            listsToBeAdded.addAll(convertLists(lists))

            listsDatabase.withTransaction {
                traktLIstsDao.deleteTraktListsFromCache()
                traktLIstsDao.insert(listsToBeAdded)
            }

            listsDatabase.withTransaction {
                lastRefreshedAtDao.insertLastRefreshStats(
                    LastRefreshedAt(
                        RefreshType.LISTS,
                        OffsetDateTime.now()
                    )
                )
            }
        }
    )

    fun getListEntries(traktListId: Int?, shouldFetch: Boolean) = networkBoundResource(
        query = {
            listEntryDao.getTraktListEntriesById(traktListId ?: 0)
        },
        fetch = {
            if (traktListId == WATCHLIST_ID) {
                traktApi.tmUsers().watchList(null)
            } else {
                traktApi.tmUsers().listItems(userSlug, traktListId.toString(), Extended.FULL)
            }
        },
        shouldFetch = { listEntries ->
            traktListId != null && (shouldFetch || listEntries.isEmpty())
        },
        saveFetchResult = { listEntries ->

            if (traktListId != null) {
                insertListItems(traktListId, listEntries)
            }
        }
    )

    private suspend fun getWatchedList(): TraktList {

        val watchlistItems = traktApi.tmUsers().watchList(null)
        val user = traktApi.tmUsers().profile(userSlug, null)

        insertListItems(WATCHLIST_ID, watchlistItems)

        return TraktList(
            WATCHLIST_ID,
            null,
            "Watchlist",
            "Movies, shows, seasons, and episodes I plan to watch.",
            user.joined_at,
            null,
            false,
            0,
            false,
            watchlistItems.size,
            null,
            ListPrivacy.PRIVATE,
            null,
            null,
            user
        )
    }

    suspend fun getTraktListsAndItems(shouldFetch: Boolean): Flow<Resource<out List<Pair<com.nickrankin.traktapp.dao.lists.model.TraktList, List<TraktListEntry>>>>> =
        flow {
            emit(Resource.Loading(null))

            // The lists from cache
            val listsFromDao = traktLIstsDao.getAllTraktLists()

            // The List items from cache
            val listItemsFromDao = listEntryDao.getTraktListEntries()

            if (shouldFetch || listsFromDao.first().isEmpty()) {
                Log.d(TAG, "getTraktListsAndItems: Refreshing Trakt Lists and Items")
                try {
                    // Get and Convert Trakt Lists response
                    val traktLists = convertLists(traktApi.tmUsers().lists(userSlug))

                    listsDatabase.withTransaction {
                        traktLIstsDao.deleteTraktListsFromCache()
                        listEntryDao.deleteAllListEntries()
                    }

                    // Insert the lists into DB
                    insertLists(traktLists)

                    // Get and insert list items
                    traktLists.map { traktList ->
                        val listEntryResponse = traktApi.tmUsers()
                            .listItems(userSlug, traktList.trakt_id.toString(), Extended.FULL)
                        insertListItems(traktList.trakt_id, listEntryResponse)
                    }
                } catch (t: Throwable) {
                    emit(Resource.Error(t, null))
                }
            }

            // Each time list or list items changes, notify observers
            val listsAndItemsflow = combine(listsFromDao, listItemsFromDao) { lists, listItems ->
                val listsToListItemsMap: MutableMap<TraktList, List<TraktListEntry>> =
                    mutableMapOf()
                val listListItemPairs: MutableList<Pair<com.nickrankin.traktapp.dao.lists.model.TraktList, List<TraktListEntry>>> =
                    mutableListOf()

                lists.map { list ->
                    listListItemPairs.add(
                        Pair(
                            list,
                            listItems.filter { it.entryData.list_trakt_id == list.trakt_id })
                    )

                }
                Resource.Success(listListItemPairs)
            }

            emitAll(listsAndItemsflow)
        }

    private fun convertLists(oldLists: List<com.uwetrottmann.trakt5.entities.TraktList>): List<TraktList> {
        val convertedLists: MutableList<TraktList> = mutableListOf()

        oldLists.map { list ->
            convertedLists.add(
                TraktList(
                    list.ids?.trakt ?: -1,
                    list.ids?.slug ?: "",
                    list.name ?: "Untitled",
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

    private suspend fun insertLists(traktLists: List<TraktList>) {
        listsDatabase.withTransaction {
            traktLIstsDao.insert(traktLists)
        }
    }

    private suspend fun insertListItems(
        traktListId: Int,
        listItems: List<com.uwetrottmann.trakt5.entities.ListEntry>
    ) {
        val listEntries: MutableList<ListEntry> = mutableListOf()
        val movieBaseItems: MutableList<MovieBaseEntity> = mutableListOf()
        val showBaseItems: MutableList<ShowBaseEntity> = mutableListOf()
        val episodeBaseItems: MutableList<EpisodeBaseEnity> = mutableListOf()
        val personBaseItems: MutableList<PersonBaseEntity> = mutableListOf()

        listItems.map { listEntry ->
            when (listEntry.type?.lowercase()) {
                Type.MOVIE.name.lowercase() -> {
                    listEntries.add(
                        com.nickrankin.traktapp.dao.lists.model.ListEntry(
                            listEntry.id,
                            traktListId,
                            listEntry.movie?.ids?.trakt ?: 0,
                            0,
                            listEntry.listed_at,
                            listEntry.rank,
                            Type.MOVIE
                        )
                    )

                    movieBaseItems.add(
                        getMovieBaseEntity(listEntry)
                    )

                }
                Type.SHOW.name.lowercase() -> {
                    listEntries.add(
                        com.nickrankin.traktapp.dao.lists.model.ListEntry(
                            listEntry.id,
                            traktListId,
                            listEntry.show?.ids?.trakt ?: 0,
                            listEntry.show?.ids?.trakt ?: 0,
                            listEntry.listed_at,
                            listEntry.rank,
                            Type.SHOW
                        )
                    )

                    showBaseItems.add(
                        getShowBaseEntity(listEntry)
                    )
                }
                Type.EPISODE.name.lowercase() -> {
                    listEntries.add(
                        com.nickrankin.traktapp.dao.lists.model.ListEntry(
                            listEntry.id,
                            traktListId,
                            listEntry.episode?.ids?.trakt ?: 0,
                            listEntry.show?.ids?.trakt ?: 0,
                            listEntry.listed_at,
                            listEntry.rank,
                            Type.EPISODE
                        )
                    )

                    showBaseItems.add(
                        getShowBaseEntity(listEntry)
                    )

                    episodeBaseItems.add(
                        getEpisodeBaseEntry(listEntry)
                    )
                }
                Type.PERSON.name.lowercase() -> {
                    listEntries.add(
                        com.nickrankin.traktapp.dao.lists.model.ListEntry(
                            listEntry.id,
                            traktListId,
                            listEntry.person?.ids?.trakt ?: 0,
                            0,
                            listEntry.listed_at,
                            listEntry.rank,
                            Type.PERSON
                        )
                    )

                    personBaseItems.add(
                        getPersonEntry(listEntry)
                    )
                }
                else -> {
                    Log.e(TAG, "insertListItems: Type ${listEntry.type} not supported")
                }
            }

        }

        listsDatabase.withTransaction {
            listEntryDao.insertListEntry(listEntries)
            listEntryDao.insertMovie(movieBaseItems)
            listEntryDao.insertShow(showBaseItems)
            listEntryDao.insertEpisode(episodeBaseItems)
            listEntryDao.insertPerson(personBaseItems)
        }
    }

    suspend fun addToList(itemTraktId: Int, listTraktId: Int, type: Type): Resource<SyncResponse> {
        return try {
            Log.d(TAG, "addToList: Adding $itemTraktId to list $listTraktId")

            if (listTraktId == WATCHLIST_ID) {
                val addListResponse = traktApi.tmUsers()
                    .addToWatchList(getSyncItems(itemTraktId, type))

                insertListItem(itemTraktId, listTraktId, type, addListResponse)

                Resource.Success(addListResponse)
            } else {
                val addListResponse = traktApi.tmUsers()
                    .addListItems(userSlug, listTraktId.toString(), getSyncItems(itemTraktId, type))

                insertListItem(itemTraktId, listTraktId, type, addListResponse)

                Resource.Success(addListResponse)
            }
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    private suspend fun insertListItem(
        itemTraktId: Int,
        listTraktId: Int,
        type: Type,
        syncResponse: SyncResponse?
    ) {
        when (type) {
            Type.MOVIE -> {
                if ((syncResponse?.added?.movies ?: 0) <= 0) {
                    Log.e(TAG, "insertListItem: Error, sync returned 0, returning")
                    return
                }
            }
            Type.SHOW -> {
                if ((syncResponse?.added?.shows ?: 0) <= 0) {
                    Log.e(TAG, "insertListItem: Error, sync returned 0, returning")
                    return
                }
            }
            Type.EPISODE -> {
                if ((syncResponse?.added?.episodes ?: 0) <= 0) {
                    Log.e(TAG, "insertListItem: Error, sync returned 0, returning")
                    return
                }
            }
            Type.PERSON -> TODO()
            Type.LIST -> TODO()
        }

        Log.d(TAG, "insertListItem: Inserting listitem $itemTraktId in $listTraktId")

        val listEntry = when (type) {
            Type.MOVIE -> {
                if (listTraktId == WATCHLIST_ID) {
                    traktApi.tmUsers().watchList(null)
                        .find { (it.movie?.ids?.trakt ?: 0) == itemTraktId }
                } else {
                    traktApi.tmUsers().listItems(userSlug, listTraktId.toString(), null)
                        .find { (it.movie?.ids?.trakt ?: 0) == itemTraktId }
                }

            }
            Type.SHOW -> {
                if (listTraktId == WATCHLIST_ID) {
                    traktApi.tmUsers().watchList(null)
                        .find { (it.show?.ids?.trakt ?: 0) == itemTraktId }
                } else {
                    traktApi.tmUsers().listItems(userSlug, listTraktId.toString(), null)
                        .find { (it.show?.ids?.trakt ?: 0) == itemTraktId }
                }
            }
            Type.EPISODE -> {
                if (listTraktId == WATCHLIST_ID) {
                    traktApi.tmUsers().watchList(null)
                        .find { (it.episode?.ids?.trakt ?: 0) == itemTraktId }
                } else {
                    traktApi.tmUsers().listItems(userSlug, listTraktId.toString(), null)
                        .find { (it.episode?.ids?.trakt ?: 0) == itemTraktId }
                }
            }
            Type.PERSON -> {
                if (listTraktId == WATCHLIST_ID) {
                    Log.e(TAG, "insertListItem:  Person should not be in watch list")
                }
                traktApi.tmUsers().listItems(userSlug, listTraktId.toString(), Extended.FULL)
                    .find { (it.person?.ids?.trakt ?: 0) == itemTraktId }
            }
            Type.LIST -> {
                null
            }
        }


        if (listEntry == null) {
            Log.e(TAG, "insertListItem: Listentry should not be null")
            return
        }

        listsDatabase.withTransaction {
            listEntryDao.insertListEntry(
                ListEntry(
                    listEntry.id,
                    listTraktId,
                    itemTraktId,
                    listEntry.show?.ids?.trakt ?: 0,
                    listEntry.listed_at,
                    listEntry.rank,
                    type
                )
            )

            when (type) {
                Type.MOVIE -> {
                    listEntryDao.insertMovie(
                        getMovieBaseEntity(listEntry)
                    )
                }
                Type.SHOW -> {
                    listEntryDao.insertShow(
                        getShowBaseEntity(listEntry)
                    )
                }
                Type.EPISODE -> {
                    listEntryDao.insertShow(
                        getShowBaseEntity(listEntry)
                    )

                    listEntryDao.insertEpisode(getEpisodeBaseEntry(listEntry))
                }
                Type.PERSON -> {
                    listEntryDao.insertPerson(getPersonEntry(listEntry))
                }
                Type.LIST -> TODO()
            }


        }
    }

    private fun getMovieBaseEntity(listEntry: com.uwetrottmann.trakt5.entities.ListEntry): MovieBaseEntity {
        return MovieBaseEntity(
            listEntry.movie?.ids?.trakt ?: 0,
            listEntry.movie?.ids?.tmdb,
            listEntry.movie?.title ?: "Unknown",
            listEntry.movie?.released,
            listEntry.movie?.language
        )
    }

    private fun getShowBaseEntity(listEntry: com.uwetrottmann.trakt5.entities.ListEntry): ShowBaseEntity {
        return ShowBaseEntity(
            listEntry.show?.ids?.trakt ?: 0,
            listEntry.show?.ids?.tmdb,
            listEntry.show?.title ?: "Unknown",
            listEntry.show?.first_aired,
            listEntry.show?.language
        )
    }

    private fun getEpisodeBaseEntry(listEntry: com.uwetrottmann.trakt5.entities.ListEntry): EpisodeBaseEnity {
        return EpisodeBaseEnity(
            listEntry.episode?.ids?.trakt ?: 0,
            listEntry.episode?.ids?.tmdb,
            listEntry.episode?.title ?: "Unknown",
            listEntry.episode?.first_aired,
            listEntry.show?.language,
            listEntry.show?.ids?.trakt ?: 0,
            listEntry.show?.ids?.tmdb,
            listEntry.episode?.season ?: 0,
            listEntry.episode?.number ?: 0
        )
    }

    private fun getPersonEntry(listEntry: com.uwetrottmann.trakt5.entities.ListEntry): PersonBaseEntity {
        return PersonBaseEntity(
            listEntry.person?.ids?.trakt ?: 0,
            listEntry.person?.ids?.tmdb,
            listEntry.person?.name ?: "Unknown",
            listEntry.person?.birthday,
            listEntry.person?.birthplace
        )
    }


    suspend fun removeFromList(
        itemTraktId: Int,
        listTraktId: Int,
        type: Type
    ): Resource<SyncResponse> {
        return try {

            Log.d(TAG, "removeFromList: Removing $itemTraktId from list $listTraktId")

            if (listTraktId == WATCHLIST_ID) {
                val removeFromListResponse = traktApi.tmUsers()
                    .removeFromWatchList(getSyncItems(itemTraktId, type))

                removeListEntryFromDb(itemTraktId, listTraktId, removeFromListResponse, type)

                Resource.Success(removeFromListResponse)
            } else {

                val removeFromListResponse = traktApi.tmUsers()
                    .deleteListItems(
                        userSlug,
                        listTraktId.toString(),
                        getSyncItems(itemTraktId, type)
                    )

                removeListEntryFromDb(itemTraktId, listTraktId, removeFromListResponse, type)

                Resource.Success(removeFromListResponse)
            }

        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    private suspend fun removeListEntryFromDb(
        itemTraktId: Int,
        listTraktId: Int,
        syncResponse: SyncResponse?,
        type: Type
    ) {
        when (type) {
            Type.MOVIE -> {
                if ((syncResponse?.deleted?.movies ?: 0) <= 0) {
                    Log.e(
                        TAG,
                        "removeListEntryFromDb: SyncResponse failed to remove content, won't modify db",
                    )
                    return
                }
            }
            Type.SHOW -> {
                if ((syncResponse?.deleted?.shows ?: 0) <= 0) {
                    Log.e(
                        TAG,
                        "removeListEntryFromDb: SyncResponse failed to remove content, won't modify db",
                    )
                    return
                }
            }
            Type.EPISODE -> {
                if ((syncResponse?.deleted?.episodes ?: 0) <= 0) {
                    Log.e(
                        TAG,
                        "removeListEntryFromDb: SyncResponse failed to remove content, won't modify db",
                    )
                    return
                }
            }
            Type.PERSON -> TODO()
            Type.LIST -> TODO()
        }


        listsDatabase.withTransaction {
            listEntryDao.deleteListEntry(listTraktId, itemTraktId)
        }
    }

    private fun getSyncItems(traktId: Int, type: Type): SyncItems {
        return SyncItems().apply {
            when (type) {
                Type.MOVIE -> {
                    movies = listOf(
                        SyncMovie().id(MovieIds.trakt(traktId))
                    )
                }
                Type.SHOW -> {
                    shows = listOf(
                        SyncShow().id(ShowIds.trakt(traktId))
                    )
                }
                Type.EPISODE -> {
                    episodes = listOf(
                        SyncEpisode().id(EpisodeIds.trakt(traktId))
                    )
                }
                Type.PERSON -> TODO()
                Type.LIST -> TODO()
            }

        }
    }

    suspend fun addTraktList(traktList: com.uwetrottmann.trakt5.entities.TraktList): Resource<TraktList> {
        return try {
            val response = traktApi.tmUsers().createList(
                UserSlug(
                    sharedPreferences.getString(
                        AuthActivity.USER_SLUG_KEY,
                        "NULL"
                    )
                ), traktList
            )

            // Need to convert the list to our dao model so we can cache it
            val convertedList = convertLists(listOf(response)).first()

            listsDatabase.withTransaction {
                traktLIstsDao.insertSingle(convertedList)
            }

            Resource.Success(convertedList)

        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun editTraktList(
        traktList: com.uwetrottmann.trakt5.entities.TraktList,
        listSlug: String
    ): Resource<TraktList> {
        return try {
            val response = traktApi.tmUsers().updateList(
                UserSlug(
                    sharedPreferences.getString(
                        AuthActivity.USER_SLUG_KEY,
                        "NULL"
                    )
                ), listSlug, traktList
            )

            // Need to convert the list to our dao model so we can cache it
            val convertedList = convertLists(listOf(response)).first()

            listsDatabase.withTransaction {
                traktLIstsDao.insertSingle(convertedList)
            }

            Resource.Success(convertedList)

        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun deleteTraktList(listTraktId: String): Resource<Boolean> {
        return try {
            val response = traktApi.tmUsers().deleteList(
                UserSlug(
                    sharedPreferences.getString(
                        AuthActivity.USER_SLUG_KEY,
                        "NULL"
                    )
                ), listTraktId
            )

            listsDatabase.withTransaction {
                traktLIstsDao.deleteListById(listTraktId)
            }

            Resource.Success(true)

        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    companion object {
        const val WATCHLIST_ID = -6

    }
}