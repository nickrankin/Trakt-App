package com.nickrankin.traktapp.dao.movies

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watched_movie_page_keys")
data class WatchedMoviePageKey(
    @PrimaryKey val historyId: Long,
    val prevPage: Int?,
    val nextPage: Int?
)