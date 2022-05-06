package com.nickrankin.traktapp.dao.lists.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.LocalDate

@Entity(tableName = "person_entries")
data class PersonEntry(
    @PrimaryKey val trakt_id: Int,
    val tmdb_id: Int?,
    val name: String,
    val bio: String?,
    val birthday: LocalDate?,
    val death: LocalDate?
)