package com.nickrankin.traktapp.dao.show.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.tmdb2.entities.*
import java.util.*


@Entity(tableName = "shows")
data class TmShow(
    @PrimaryKey val trakt_id: Int,
    val tmdb_id: Int?,
    val name: String,
    val overview: String,
    val country: List<String?>,
    val created_by: List<Person?>,
    val external_ids: TvExternalIds?,
    val genres: List<Genre?>,
    val homepage: String?,
    val images: Images?,
    val in_production: Boolean?,
    val languages: List<String?>,
    val first_aired: Date?,
    val last_air_date: Date?,
    val last_aired_episode: BaseTvEpisode?,
    val networks: List<Network?>,
    val next_episode: BaseTvEpisode?,
    val num_episodes: Int?,
    val num_seasons: Int?,
    val runtime: Int?,
    val status: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val type: String?,
    val videos: Videos?,
    var user_tracking: Boolean,
    val trakt_rating: Double)