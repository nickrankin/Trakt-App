package com.nickrankin.traktapp.dao.base_entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.LocalDate
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "episode_table_entity")
data class EpisodeBaseEnity(
    @PrimaryKey override val trakt_id: Int,
    override val tmdb_id: Int?,
    override val title: String,
    val first_aired: OffsetDateTime?,
    override val language: String?,
    val show_trakt_id: Int,
    val show_tmdb_id: Int?,
    val season_number: Int,
    val episode_number: Int
): BaseEntity