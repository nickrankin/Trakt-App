package com.nickrankin.traktapp.dao.credits.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cast_persons")
data class CastPerson(@PrimaryKey val traktId: Int, val tmdbId: Int?, val imdbId: String?, val bio: String?, val birthplace: String?, val name: String, val photo_path: String?)