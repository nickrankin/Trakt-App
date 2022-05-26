package com.nickrankin.traktapp.dao.stats.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "stats_watched_season")
data class WatchedSeasonStats(
    @PrimaryKey val id: String,
    val show_trakt_id: Int,
    val season: Int,
    val aired: Int,
    val completed: Int
)