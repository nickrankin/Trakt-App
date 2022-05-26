package com.nickrankin.traktapp.dao.stats.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.trakt5.enums.Type
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "stats_collected_movies")
data class CollectedMoviesStats(
    @PrimaryKey val trakt_id: Int,
    val tmdb_id: Int?,
    val collected_at: OffsetDateTime?,
    val title: String,
    val listedAt: OffsetDateTime?)