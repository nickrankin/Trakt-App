package com.nickrankin.traktapp.dao.show.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "notifications_episode")
data class TrackedEpisode(@PrimaryKey val trakt_id: Int, val tmdb_id: Int, val show_trakt_id: Int, val show_tmdb_id: Int, val language: String?, val airs_date: OffsetDateTime, val title: String?, val show_title: String, val season: Int, val episode: Int)