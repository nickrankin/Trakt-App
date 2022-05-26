package com.nickrankin.traktapp.dao.stats.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "stats_watched_episode")
data class WatchedEpisodeStats(
    @PrimaryKey val id: String,
    val show_tmdb_id: Int?,
    val show_trakt_id: Int,
    val show_title: String,
    val season: Int,
    val episode: Int,
    val last_watched_at: OffsetDateTime?,
    val plays: Int
)