package com.nickrankin.traktapp.dao.show

import androidx.room.Embedded
import androidx.room.Relation
import com.nickrankin.traktapp.dao.show.model.TmSeason
import com.nickrankin.traktapp.dao.stats.model.WatchedSeasonStats

data class TmSeasonAndStats(
    @Embedded val season: TmSeason,
    @Relation(
        parentColumn = "show_trakt_id",
        entityColumn = "show_trakt_id"
    )
    val watchedSeasonStats: List<WatchedSeasonStats?>
)
