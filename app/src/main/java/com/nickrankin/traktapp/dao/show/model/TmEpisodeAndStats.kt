package com.nickrankin.traktapp.dao.show.model

import androidx.room.Embedded
import androidx.room.Relation
import com.nickrankin.traktapp.dao.history.model.EpisodeWatchedHistoryEntry
import com.nickrankin.traktapp.dao.stats.model.RatingsEpisodesStats
import com.nickrankin.traktapp.dao.stats.model.WatchedEpisodeStats

data class TmEpisodeAndStats(
    @Embedded val episode: TmEpisode,
    @Relation(
        parentColumn = "episode_trakt_id",
        entityColumn = "trakt_id"
    )
    val watchedEpisodeStats: EpisodeWatchedHistoryEntry?,
    @Relation(
        parentColumn = "episode_trakt_id",
        entityColumn = "trakt_id"
    )
    val ratingEpisodeStats: RatingsEpisodesStats?
)
