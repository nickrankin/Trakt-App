package com.nickrankin.traktapp.dao.movies.model

import androidx.room.Embedded
import androidx.room.Relation
import com.nickrankin.traktapp.dao.stats.model.RatingsMoviesStats
import com.nickrankin.traktapp.dao.stats.model.WatchedMoviesStats

data class WatchedMovieAndStats(
    @Embedded val watchedMovie: WatchedMovie,
    @Relation(
        parentColumn = "trakt_id",
        entityColumn = "trakt_id"
    )
    val watchedMoviesStats: WatchedMoviesStats?,
    @Relation(
        parentColumn = "trakt_id",
        entityColumn = "trakt_id"
    )
    val ratedMovieStats: RatingsMoviesStats?
)
