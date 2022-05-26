package com.nickrankin.traktapp.dao.stats.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.trakt5.enums.Type
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "stats_collected_shows")
data class CollectedShowsStats(
    @PrimaryKey val trakt_id: Int,
    val tmdb_id: Int?,
    val last_collected_at: OffsetDateTime?,
    val last_updated_at: OffsetDateTime?,
    val reset_at: OffsetDateTime?,
    val completed: Int,
    val title: String,
    val listedAt: OffsetDateTime?)