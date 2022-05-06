package com.nickrankin.traktapp.dao.images.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.OffsetDateTime

@Entity
data class Image(@PrimaryKey val trakt_id: Int, var tmdbId: Int?, var poster_path: String?, var backdrop_path: String?, val updated: OffsetDateTime)