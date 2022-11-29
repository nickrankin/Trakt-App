package com.nickrankin.traktapp.dao.credits

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.nickrankin.traktapp.dao.credits.model.Person
import org.threeten.bp.LocalDate

@Entity(tableName = "show_cast_person")
data class ShowCastPerson(
    @PrimaryKey val person_show_trakt_id: String,
    val person_trakt_id: Int,
    val show_trakt_id: Int,
    val show_tmdb_id: Int?,
    val ordering: Int,
    val character: String?,
    val name: String,
    val picture_path: String?,
    val is_guest_star: Boolean,
    val season: Int?,
    val episode: Int?
)