package com.nickrankin.traktapp.dao.lists

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Relation
import com.nickrankin.traktapp.dao.lists.model.ListEntry
import com.nickrankin.traktapp.dao.lists.model.TraktList


@Entity
data class ListWithEntries(
    @Embedded val list: TraktList,
    @Relation(
        parentColumn = "trakt_id",
        entityColumn = "trakt_list_id"
    )
    val entries: List<ListEntry?>
)