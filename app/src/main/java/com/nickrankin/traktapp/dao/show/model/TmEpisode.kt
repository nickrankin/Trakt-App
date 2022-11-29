package com.nickrankin.traktapp.dao.show.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.tmdb2.entities.*
import org.threeten.bp.OffsetDateTime
import java.util.*

@Entity(tableName = "episodes")
data class TmEpisode(
    @PrimaryKey val id: Int,
    val episode_trakt_id: Int,
    val episode_tmdb_id: Int?,
    val episode_imdb_id: String?,
    val show_tmdb_id: Int?,
    val show_trakt_id: Int,
    val language: String?,
    val season_number: Int?,
    val episode_number: Int?,
    val production_code: String?,
    val name: String?,
    val overview: String?,
    val runtime: Int?,
    val air_date: OffsetDateTime?,
    val images: Images?,
    val still_path: String?,
    val videos: Videos?,
    val trakt_rating: Double
) {
    companion object {
        const val SOURCE_TRAKT = "trakt"
        const val SOURCE_TMDB = "tmdb"
    }
}