package com.nickrankin.traktapp.repo

import com.nickrankin.traktapp.dao.history.model.HistoryEntry
import com.nickrankin.traktapp.dao.lists.model.ListEntry
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.dao.lists.model.TraktListEntry
import com.nickrankin.traktapp.dao.stats.model.CollectedStats
import com.nickrankin.traktapp.dao.stats.model.RatingStats
import com.nickrankin.traktapp.helper.Resource
import com.uwetrottmann.trakt5.entities.BaseCheckinResponse
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.enums.Type
import kotlinx.coroutines.flow.Flow
import org.threeten.bp.OffsetDateTime

/**
 *
 * An Interface to define Action button Repository class methods
 * @param T Subclass of HistoryEntry. Defines the history Entry of the current type to display
 *
 */
interface IActionButtons<T: HistoryEntry> {
    suspend fun getRatings(traktId: Int, shouldFetch: Boolean): Flow<Resource<RatingStats?>>
    suspend fun getCollectedStats(traktId: Int, shouldFetch: Boolean): Flow<Resource<CollectedStats?>>
    suspend fun getPlaybackHistory(traktId: Int, shouldFetch: Boolean): Flow<Resource<List<T>>>
    suspend fun getTraktListsAndItems(shouldFetch: Boolean): Flow<Resource<out List<Pair<TraktList, List<TraktListEntry>>>>>

    suspend fun checkin(traktId: Int, overrideActiveCheckins: Boolean): Resource<BaseCheckinResponse>
    suspend fun cancelCheckins(): Resource<Boolean>
    suspend fun addRating(traktId: Int, newRating: Int, ratedAt: OffsetDateTime): Resource<SyncResponse>
    suspend fun deleteRating(traktId: Int): Resource<SyncResponse>
    suspend fun addToHistory(traktId: Int, watchedAt: OffsetDateTime): Resource<SyncResponse>
    suspend fun removeFromHistory(id: Long): Resource<SyncResponse>
    suspend fun addToCollection(traktId: Int): Resource<SyncResponse>
    suspend fun removeFromCollection(traktId: Int): Resource<SyncResponse>
    suspend fun addToList(itemTraktId: Int, listTraktId: Int): Resource<SyncResponse>
    suspend fun removeFromList(itemTraktId: Int, listTraktId: Int): Resource<SyncResponse>
}