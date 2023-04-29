package com.nickrankin.traktapp.dao.calendars.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.trakt5.enums.Status
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "show_calendar_entry")
data class ShowBaseCalendarEntry(
    @PrimaryKey override val episode_trakt_id: Int,
    val episode_tmdb_id: Int?,
    val language: String?,
    val show_trakt_id: Int,
    val show_tmdb_id: Int,
    override val first_aired: OffsetDateTime?,
    val episode_season: Int,
    val episode_number: Int,
    val episode_number_abs: Int,
    val episode_overview: String?,
    val episode_runtime: Int?,
    val episode_title: String?,
    val status: Status,
    val show_title: String,
    var hidden: Boolean
): BaseCalendarEntry(episode_trakt_id, first_aired)