package com.nickrankin.traktapp.dao.show.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "tracked_shows")
data class TrackedShow(@PrimaryKey val trakt_id: Int, val tmdb_id: Int?, val title: String?, val overview: String?, val releaseDate: OffsetDateTime?, val runtime: Int?, var tracked_on: OffsetDateTime)