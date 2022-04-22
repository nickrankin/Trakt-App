package com.nickrankin.traktapp.dao.credits.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movie_cast")
data class MovieCastPersonData(@PrimaryKey val castPersonTraktId: Int, val showTraktId: Int, val ordering: Int, val character: String?)