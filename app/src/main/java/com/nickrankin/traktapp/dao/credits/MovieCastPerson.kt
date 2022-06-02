package com.nickrankin.traktapp.dao.credits

import androidx.room.Embedded
import androidx.room.Relation
import com.nickrankin.traktapp.dao.credits.model.MovieCastPersonData
import com.nickrankin.traktapp.dao.credits.model.Person
import com.nickrankin.traktapp.dao.credits.model.ShowCastPersonData

data class MovieCastPerson(
    @Embedded val movieCastPersonData: MovieCastPersonData,
    @Relation(
        parentColumn = "person_trakt_id",
        entityColumn = "trakt_id"
    )
    val person: Person
)