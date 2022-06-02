package com.nickrankin.traktapp.dao.credits.model

import androidx.room.Embedded
import androidx.room.Relation
import com.nickrankin.traktapp.dao.movies.model.TmMovie

data class CastPersonWithMovie(
    @Embedded val movieCastPersonData: MovieCastPersonData,
    @Relation(
        parentColumn = "person_trakt_id",
        entityColumn = "trakt_id"
    )
    val person: Person?,
    @Relation(
        parentColumn = "movie_trakt_id",
        entityColumn = "trakt_id"
    )
    val movie: TmMovie?
)