package com.nickrankin.traktapp.dao.show.model

import androidx.room.Embedded
import androidx.room.Relation

data class ShowAndSeasonProgress(
    @Embedded val showProgress: ShowsProgress,
    @Relation(
        parentColumn = "show_trakt_id",
        entityColumn = "show_trakt_id"
    )
    val showSeasonProgress: List<SeasonProgress?>
)