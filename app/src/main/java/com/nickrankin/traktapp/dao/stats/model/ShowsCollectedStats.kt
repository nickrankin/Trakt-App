package com.nickrankin.traktapp.dao.stats.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "stats_collected_shows")
data class ShowsCollectedStats(
    @PrimaryKey override val trakt_id: Int,
    override val tmdb_id: Int?,
    override val collected_at: OffsetDateTime?,
    override val title: String,
    override val listedAt: OffsetDateTime?,
    val last_updated_at: OffsetDateTime?,
    val reset_at: OffsetDateTime?,
    val completed: Int): CollectedStats