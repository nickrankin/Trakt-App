package com.nickrankin.traktapp.dao.show.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "show_progress")
data class ShowsProgress(
    @PrimaryKey val show_trakt_id: Int,
    val show_tmdb_id: Int?,
    val total_aired: Int,
    val title: String,
    val overview: String?,
    val last_watched_at: OffsetDateTime,
    val last_updated_at: OffsetDateTime
)