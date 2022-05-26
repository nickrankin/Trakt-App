package com.nickrankin.traktapp.dao.show.model

import androidx.room.Embedded
import androidx.room.Relation
import com.nickrankin.traktapp.dao.stats.model.RatingsEpisodesStats
import com.nickrankin.traktapp.dao.stats.model.WatchedEpisodeStats

data class TmEpisodeAndStats(
    @Embedded val episode: TmEpisode,
    @Relation(
        parentColumn = "show_trakt_id",
        entityColumn = "show_trakt_id"
    )
    val watchedEpisodeStats: List<WatchedEpisodeStats?>,
    @Relation(
        parentColumn = "show_trakt_id",
        entityColumn = "show_trakt_id"
    )
    val ratingEpisodeStats: List<RatingsEpisodesStats>?
)
