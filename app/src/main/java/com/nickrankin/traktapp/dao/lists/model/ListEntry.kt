package com.nickrankin.traktapp.dao.lists.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "list_entries")
data class ListEntry(
    @PrimaryKey val id: Long,
    val trakt_list_id: Int,
    val show_trakt_id: Int?,
    val list_entry_trakt_id: Int,
    val listed_at: OffsetDateTime,
    val rank: Int,
    val type: String
)