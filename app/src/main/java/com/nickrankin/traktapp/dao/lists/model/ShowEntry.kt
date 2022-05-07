package com.nickrankin.traktapp.dao.lists.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.tmdb2.enumerations.Status
import org.threeten.bp.OffsetDateTime
import java.util.*

@Entity(tableName = "show_entries")
data class ShowEntry(
    @PrimaryKey val trakt_id: Int,
    val tmdb_id: Int?,
    val title: String,
    val overview: String?,
    val first_aired: OffsetDateTime?,
    val runtime: Int?,
    val language: String?)