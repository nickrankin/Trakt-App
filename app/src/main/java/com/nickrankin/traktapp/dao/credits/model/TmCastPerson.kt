package com.nickrankin.traktapp.dao.credits.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.trakt5.enums.Type

@Entity(tableName = "cast_people")
data class TmCastPerson(@PrimaryKey override val id: String, override val person_trakt_id: Int, override val trakt_id: Int, override val tmdb_id: Int?, override val title: String?, override val year: Int?, override val ordering: Int,
                        override val type: Type, val character: String?): CreditPerson