package com.nickrankin.traktapp.dao.lists.model

import androidx.room.Embedded
import androidx.room.Relation

data class TraktListAndEntries(
    @Embedded
    val list: TraktList,

    @Relation(
        parentColumn = "trakt_id",
        entityColumn = "list_trakt_id"
    )
    val listEntries: List<ListEntry>
)