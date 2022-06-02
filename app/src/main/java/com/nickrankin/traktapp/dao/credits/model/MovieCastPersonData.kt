package com.nickrankin.traktapp.dao.credits.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movie_cast")
data class MovieCastPersonData(@PrimaryKey val person_movie_trakt_id: String, val person_trakt_id: Int, val movie_trakt_id: Int, val movie_tmdb_id: Int?, val ordering: Int, val character: String?)