package com.nickrankin.traktapp.dao.stats.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.trakt5.enums.Type
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "stats_ratings_episodes")
data class RatingsEpisodesStats(
    @PrimaryKey override val trakt_id: Int,
    override val rating: Int,
    val rated_at: OffsetDateTime): RatingStats