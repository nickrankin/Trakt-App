package com.nickrankin.traktapp.dao.show.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.tmdb2.entities.*
import org.threeten.bp.OffsetDateTime
import java.util.*


@Entity(tableName = "shows")
data class TmShow(
    @PrimaryKey val trakt_id: Int,
    val tmdb_id: Int?,
    val imdb_id: String?,
    val name: String,
    val overview: String,
    val country: String?,
    val genres: List<String?>?,
    val created_by: List<com.uwetrottmann.trakt5.entities.CrewMember>?,
    val homepage: String?,
    val status: com.uwetrottmann.trakt5.enums.Status?,
    val language: String?,
    val first_aired: OffsetDateTime?,
    val network: String?,
    val num_episodes: Int?,
    val num_seasons: Int?,
    val runtime: Int?,
    val poster_path: String?,
    val backdrop_path: String?,
    val videos: Videos?,
    var user_tracking: Boolean,
    val trakt_rating: Double)