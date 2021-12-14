package com.nickrankin.traktapp.dao.show.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.tmdb2.entities.*
import java.util.*

@Entity(tableName = "episodes")
data class TmEpisode(
    @PrimaryKey val tmdb_id: Int,
    val show_tmdb_id: Int,
    val show_trakt_id: Int,
    val season_number: Int?,
    val episode_number: Int?,
    val production_code: String?,
    val name: String?,
    val overview: String?,
    val air_date: Date?,
    val credits: Credits?,
    val crew: List<CrewMember>?,
    val guest_stars: List<CastMember>?,
    val images: Images?,
    val externalIds: TvEpisodeExternalIds?,
    val still_path: String?,
    val videos: Videos?
)