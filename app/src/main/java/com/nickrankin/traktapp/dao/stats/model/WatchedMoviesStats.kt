package com.nickrankin.traktapp.dao.stats.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.trakt5.enums.Type
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "stats_watched_movies")
data class WatchedMoviesStats(
    @PrimaryKey val trakt_id: Int,
    val tmdb_id: Int?,
    var last_watched_at: OffsetDateTime?,
    val title: String,
    var listed_at: OffsetDateTime?,
    var plays: Int)