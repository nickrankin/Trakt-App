package com.nickrankin.traktapp.dao.credits.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movie_cast")
data class MovieCastPersonData(@PrimaryKey val id: String, val personId: String, val movieTraktId: Int, val ordering: Int, val character: String?)