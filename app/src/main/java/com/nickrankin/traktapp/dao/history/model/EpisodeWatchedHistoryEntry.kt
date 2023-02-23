package com.nickrankin.traktapp.dao.history.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "episode_watched_history")
data class EpisodeWatchedHistoryEntry(
    @PrimaryKey override val history_id: Long,
    override val trakt_id: Int,
    override val tmdb_id: Int?,
    override val title: String,
    override val watched_date: OffsetDateTime,
    override val cached_at: OffsetDateTime,
    val show_trakt_id: Int,
    val show_tmdb_id: Int?,
    val season: Int,
    val episode: Int
) : HistoryEntry