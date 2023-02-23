package com.nickrankin.traktapp.model

import com.nickrankin.traktapp.helper.Resource
import com.uwetrottmann.trakt5.entities.BaseCheckin
import com.uwetrottmann.trakt5.entities.BaseCheckinResponse
import com.uwetrottmann.trakt5.entities.MovieCheckin
import com.uwetrottmann.trakt5.entities.SyncResponse

sealed class ActionButtonEvent {
    data class AddRatingEvent(val syncResponse: Resource<SyncResponse>, val newRating: Int): ActionButtonEvent()
    data class RemoveRatingEvent(val syncResponse: Resource<SyncResponse>): ActionButtonEvent()

    data class AddHistoryEntryEvent(val syncResponse: Resource<SyncResponse>): ActionButtonEvent()
    data class RemoveHistoryEntryEvent(val syncResponse: Resource<SyncResponse>): ActionButtonEvent()

    data class AddToCollectionEvent(val syncResponse: Resource<SyncResponse>):ActionButtonEvent()
    data class RemoveFromCollectionEvent(val syncResponse: Resource<SyncResponse>): ActionButtonEvent()

    data class CheckinEvent(val baseCheckin: Resource<BaseCheckinResponse>): ActionButtonEvent()
    data class DeleteCheckinEvent(val wasDeleted: Resource<Boolean>): ActionButtonEvent()
}
