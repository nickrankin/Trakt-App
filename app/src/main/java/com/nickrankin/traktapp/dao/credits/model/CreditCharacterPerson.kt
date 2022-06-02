package com.nickrankin.traktapp.dao.credits.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.trakt5.enums.Type

@Entity(tableName = "characters")
data class CreditCharacterPerson(@PrimaryKey val trakt_id_person_id: String, val person_trakt_id: Int, val trakt_id: Int, val tmdb_id: Int?, val title: String?, val year: Int?, val ordering: Int, val character: String?, val type: Type)