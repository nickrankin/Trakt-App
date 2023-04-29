package com.nickrankin.traktapp.dao.refresh

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.OffsetDateTime

enum class RefreshType { SHOW, WATCHED_MOVIES, PLAYBACK_HISORY_MOVIES, PLAYBACK_HISORY_SHOWS, PLAYBACK_HISTORY_EPISODES, WATCHED_SHOWS, WATCHED_EPISODES, COLLECTED_MOVIES, COLLECTED_MOVIE_STATS, COLLECTED_SHOW_STATS,
    COLLECTED_EPISODE_STATS, COLLECTED_SHOWS, PROGRESS_SHOWS, COLLECTED_EPISODES, RATED_MOVIES, RATED_SHOWS, RATED_EPISODES, LISTS, WATCHLIST,  LISTS_AND_ENTRIES }

@Entity(tableName = "last_refreshed")
data class LastRefreshedAt(@PrimaryKey val refresh_type: RefreshType, val last_refreshed_at: OffsetDateTime)