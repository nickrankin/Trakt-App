package com.nickrankin.traktapp.dao.lists.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.tmdb2.entities.BaseCompany
import com.uwetrottmann.tmdb2.entities.Country
import com.uwetrottmann.tmdb2.entities.Genre
import com.uwetrottmann.tmdb2.entities.MovieExternalIds
import com.uwetrottmann.tmdb2.enumerations.Status
import org.threeten.bp.LocalDate
import org.threeten.bp.OffsetDateTime
import java.util.*


@Entity(tableName = "movie_entries")
data class MovieEntry(
    @PrimaryKey val trakt_id: Int,
    val tmdb_id: Int?,
    val title: String,
    val overview: String?,
    val release_date: LocalDate?,
    val runtime: Int?,
    val tagline: String?
)
