package com.nickrankin.traktapp.dao.show.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.trakt5.enums.Status
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "collected_shows")
data class CollectedShow(@PrimaryKey
                         val show_trakt_id: Int,
                         val show_tmdb_id: Int,
                         val language: String?,
                         val collected_at: OffsetDateTime?,
                         val last_updated_at: OffsetDateTime?,
                         val last_watched_at: OffsetDateTime?,
                         val listed_at: OffsetDateTime?,
                         val num_seasons: Int,
                         val plays: Int,
                         val show_overview: String?,
                         val status: Status,
                         val show_title: String,
                         var user_tracking: Boolean)