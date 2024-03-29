package com.nickrankin.traktapp.dao.show.model

import androidx.room.Embedded
import androidx.room.Relation
import com.nickrankin.traktapp.dao.stats.model.RatingsEpisodesStats
import com.nickrankin.traktapp.dao.stats.model.RatingsMoviesStats
import com.nickrankin.traktapp.dao.stats.model.WatchedEpisodeStats
import com.nickrankin.traktapp.dao.stats.model.WatchedMoviesStats

data class WatchedEpisodeAndStats(
    @Embedded val watchedEpisode: WatchedEpisode,
    @Relation(
        parentColumn = "episode_trakt_id",
        entityColumn = "id"
    )
    val watchedEpisodesStats: List<WatchedEpisodeStats?>,
    @Relation(
        parentColumn = "episode_trakt_id",
        entityColumn = "trakt_id"
    )
    val ratedEpisodesStats: List<RatingsEpisodesStats?>
)
