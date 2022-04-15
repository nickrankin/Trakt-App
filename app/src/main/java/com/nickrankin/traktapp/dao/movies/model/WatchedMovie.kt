package com.nickrankin.traktapp.dao.movies.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.trakt5.enums.Status
import org.threeten.bp.LocalDate
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "watched_movies")
data class WatchedMovie(
@PrimaryKey val id: Long,
val trakt_id: Int,
val tmdb_id: Int,
val language: String?,
val watched_at: OffsetDateTime?,
val overview: String?,
val runtime: Int?,
val title: String?)