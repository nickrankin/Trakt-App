package com.nickrankin.traktapp.dao.base_entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.LocalDate
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "person_base_entity")
data class PersonBaseEntity(
    @PrimaryKey override val trakt_id: Int,
    override val tmdb_id: Int?,
    override val title: String,
    val dob: LocalDate?,
    override val language: String?
): BaseEntity