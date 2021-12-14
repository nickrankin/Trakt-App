package com.nickrankin.traktapp.dao.show.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.tmdb2.entities.Credits
import com.uwetrottmann.tmdb2.entities.Images
import com.uwetrottmann.tmdb2.entities.TvSeasonExternalIds
import com.uwetrottmann.tmdb2.entities.Videos
import java.util.*

@Entity(tableName = "seasons")
data class TmSeason(
    @PrimaryKey val tmdb_id: Int,
    val show_tmdb_id: Int,
    val show_trakt_id: Int,
    val name: String,
    val overview: String?,
    val credits: Credits?,
    val externalIds: TvSeasonExternalIds?,
    val images: Images?,
    val videos: Videos?,
    val air_date: Date?,
    val episode_count: Int,
    val season_number: Int,
    val poster_path: String?
    )