package com.nickrankin.traktapp.dao.base_entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.LocalDate
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "show_base_entity")
data class ShowBaseEntity(
    @PrimaryKey override val trakt_id: Int,
    override val tmdb_id: Int?,
    override val title: String,
    val first_aired: OffsetDateTime?,
    override val language: String?
): BaseEntity