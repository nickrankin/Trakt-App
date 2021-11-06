package com.nickrankin.traktapp.dao.images.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "show_posters")
data class ShowPosterImage(@PrimaryKey val tmdb_id: Int, val poster_path: String)