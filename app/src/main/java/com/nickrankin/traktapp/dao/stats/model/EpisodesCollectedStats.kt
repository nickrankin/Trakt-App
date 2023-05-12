package com.nickrankin.traktapp.dao.stats.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "collected_episodes_stats")
data class EpisodesCollectedStats(
    @PrimaryKey(autoGenerate = true) val id: Int,
    override val trakt_id: Int,
    override val tmdb_id: Int?,
    override val collected_at: OffsetDateTime?,
    override val title: String,
    override val listedAt: OffsetDateTime?,
    val season: Int,
    val episode: Int
) : CollectedStats