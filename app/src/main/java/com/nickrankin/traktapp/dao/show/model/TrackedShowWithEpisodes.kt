package com.nickrankin.traktapp.dao.show.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Relation

@Entity
data class TrackedShowWithEpisodes(
    @Embedded val trackedShow: TrackedShow,
    @Relation(
        parentColumn = "trakt_id",
        entityColumn = "show_trakt_id"
    )
    val episodes: List<TrackedEpisode?>
)