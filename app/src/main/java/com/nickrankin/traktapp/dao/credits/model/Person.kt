package com.nickrankin.traktapp.dao.credits.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.LocalDate
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "people")
data class Person(
    @PrimaryKey val trakt_id: Int,
    val tmdb_id: Int?,
    val imdb_id: String?,
    val name: String,
    val biography: String?,
    val birthplace: String?,
    val birthday: LocalDate?,
    val death: LocalDate?,
    val homepage: String?,
    val picture_path: String?
)