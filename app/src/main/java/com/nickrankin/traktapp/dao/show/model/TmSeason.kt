package com.nickrankin.traktapp.dao.show.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.tmdb2.entities.Credits
import com.uwetrottmann.tmdb2.entities.Images
import com.uwetrottmann.tmdb2.entities.TvSeasonExternalIds
import com.uwetrottmann.tmdb2.entities.Videos
import org.threeten.bp.LocalDate
import org.threeten.bp.OffsetDateTime
import java.util.*

@Entity(tableName = "seasons")
data class TmSeason(
    @PrimaryKey val id: Int,
    val trakt_id: Int?,
    val tmdb_id: Int?,
    val show_tmdb_id: Int?,
    val show_trakt_id: Int,
    val language: String?,
    val name: String,
    val overview: String?,
    val images: Images?,
    val videos: Videos?,
    val air_date: OffsetDateTime?,
    val episode_count: Int,
    val season_number: Int,
    val poster_path: String?,
    val source: String
    ) {
    companion object {
        const val SOURCE_TRAKT = "trakt"
        const val SOURCE_TMDB = "tmdb"
    }
}