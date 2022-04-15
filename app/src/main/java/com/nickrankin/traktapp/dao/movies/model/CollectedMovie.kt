package com.nickrankin.traktapp.dao.movies.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.trakt5.enums.Status
import org.threeten.bp.LocalDate
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "collected_movies")
data class CollectedMovie(
    @PrimaryKey
    val trakt_id: Int,
    val tmdb_id: Int?,
    val language: String?,
    val collected_at: OffsetDateTime?,
    val last_updated_at: OffsetDateTime?,
    val listed_at: OffsetDateTime?,
    val plays: Int,
    val movie_overview: String?,
    val release_date: LocalDate?,
    val runtime: Int?,
    val title: String)