package com.nickrankin.traktapp.dao.history

import androidx.room.*
import com.nickrankin.traktapp.dao.history.model.EpisodeWatchedHistoryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeWatchedHistoryEntryDao {

    @Query("SELECT * FROM episode_watched_history ORDER BY watched_date DESC LIMIT 10")
    fun getLatestWatchedEpisodes(): Flow<List<EpisodeWatchedHistoryEntry>>

    @Query("SELECT * FROM episode_watched_history WHERE show_trakt_id = :showTraktId AND season = :season AND episode = :episode ORDER BY watched_date DESC")
    fun getWatchedEpisodesPerShow(showTraktId: Int, season: Int, episode: Int): Flow<List<EpisodeWatchedHistoryEntry>>

    @Query("SELECT * FROM episode_watched_history WHERE show_trakt_id = :showTraktId ORDER BY watched_date DESC")
    fun getWatchedEpisodesPerShow(showTraktId: Int): Flow<List<EpisodeWatchedHistoryEntry>>

    @Query("SELECT * FROM episode_watched_history WHERE trakt_id = :episodeTraktId ORDER BY watched_date DESC")
    fun getWatchedEpisodesPerEpisode(episodeTraktId: Int): Flow<List<EpisodeWatchedHistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(watchedHistoryEntries: List<EpisodeWatchedHistoryEntry>)

    @Query("DELETE FROM episode_watched_history WHERE show_trakt_id = :showTraktId")
    fun deleteWatchedHistoryPerShow(showTraktId: Int)

    @Query("DELETE FROM episode_watched_history WHERE trakt_id = :episodeTraktId")
    fun deleteWatchedHistoryPerEpisode(episodeTraktId: Int)

    @Query("DELETE FROM episode_watched_history WHERE history_id = :historyId")
    fun deleteWatchedHistoryEntry(historyId: Long)

    @Query("DELETE FROM episode_watched_history")
    fun deleteAllWatchedHistory()
}
