package com.nickrankin.traktapp.dao.images.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "movie_posters")
data class MoviePosterImage(@PrimaryKey val trakt_id: Int, val poster_path: String?, val updated: OffsetDateTime)