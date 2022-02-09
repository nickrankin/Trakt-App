package com.nickrankin.traktapp.dao.credits

import androidx.room.Embedded
import androidx.room.Relation
import com.nickrankin.traktapp.dao.credits.model.CastPerson
import com.nickrankin.traktapp.dao.credits.model.ShowCastPersonData

data class ShowCastPerson(
    @Embedded val showCastPersonData: ShowCastPersonData,
    @Relation(
        parentColumn = "castPersonTraktId",
        entityColumn = "traktId"
    )
    val castPerson: CastPerson
)