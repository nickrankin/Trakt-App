package com.nickrankin.traktapp.dao.movies.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.tmdb2.entities.*
import com.uwetrottmann.tmdb2.enumerations.Status
import com.uwetrottmann.trakt5.entities.CrewMember
import org.threeten.bp.LocalDate
import java.util.*


@Entity(tableName = "movies")
data class TmMovie(
    @PrimaryKey val trakt_id: Int,
    val tmdb_id: Int?,
    val imdb_id: String?,
    val title: String,
    val overview: String?,
    val directed_by: List<CrewMember?>,
    val written_by: List<CrewMember?>,
    val genres: List<String?>,
    val original_language: String?,
    val original_title: String?,
    val homepage: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val production_companies: List<BaseCompany?>?,
    val production_countries: List<Country?>?,
    val release_date: LocalDate?,
    val revenue: Long?,
    val runtime: Int?,
    val tagline: String?,
    val trailer: String?,
    val trakt_rating: Double

)
