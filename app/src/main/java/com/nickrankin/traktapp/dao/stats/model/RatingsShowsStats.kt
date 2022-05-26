package com.nickrankin.traktapp.dao.stats.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.trakt5.enums.Type
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "stats_ratings_shows")
data class RatingsShowsStats(
    @PrimaryKey val trakt_id: Int,
    val show_tmdb_id: Int?,
    val rating: Int,
    val title: String,
    val rated_at: OffsetDateTime)