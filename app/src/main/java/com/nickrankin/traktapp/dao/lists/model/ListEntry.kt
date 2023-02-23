package com.nickrankin.traktapp.dao.lists.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.trakt5.enums.Type
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "list_entry")
data class ListEntry(
    @PrimaryKey val id: Long,
    val list_trakt_id: Int,
    val item_trakt_id: Int,
    val show_trakt_id: Int,
    val listed_at: OffsetDateTime,
    val rank: Int,
    val type: Type)