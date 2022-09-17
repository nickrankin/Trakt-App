package com.nickrankin.traktapp.dao.auth.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class Stats(
    @PrimaryKey val id: Int,
    val collected_movies: Int,
    val played_movies: Int,
    val watched_movies: Int,
    val watched_movies_duration: Long,
    val collected_shows: Int,
    val played_shows: Int,
    val watched_episodes: Int,
    val watched_episodes_duration: Long
)