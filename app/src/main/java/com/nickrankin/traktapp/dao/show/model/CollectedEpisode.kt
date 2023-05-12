package com.nickrankin.traktapp.dao.show.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "collected_episodes")
data class CollectedEpisode(
    @PrimaryKey val trakt_id: Int,
    val show_trakt_id: Int,
    val season_number: Int,
    val episode_number: Int,
    val title: String?,
    val collected_at: OffsetDateTime?,
    val updated_at: OffsetDateTime?
)