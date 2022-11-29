package com.nickrankin.traktapp.dao.credits

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.nickrankin.traktapp.dao.credits.model.Person
import org.threeten.bp.LocalDate

@Entity(tableName = "movie_cast_person")
data class MovieCastPerson(
    @PrimaryKey val person_movie_trakt_id: String,
    val person_trakt_id: Int,
    val movie_trakt_id: Int,
    val movie_tmdb_id: Int?,
    val ordering: Int,
    val character: String?,
    val name: String,
    val picture_path: String?
)