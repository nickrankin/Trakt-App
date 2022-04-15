package com.nickrankin.traktapp.dao.ratings.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "rated_movies")
data class RatedMovie(@PrimaryKey val trakt_id: Int, val rating: Int, val rated_at: OffsetDateTime?)