package com.nickrankin.traktapp.dao.watched

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedHistoryShowsDao {
    @Query("SELECT * FROM watched_episodes WHERE show_trakt_id = :traktShowId")
    fun getWatchedEpisodesPerShow(traktShowId: Int): Flow<List<WatchedEpisode>>

    @Insert
    fun insertEpisodes(episodes: List<WatchedEpisode>)

    @Query("DELETE FROM watched_episodes WHERE show_trakt_id = :showTraktId")
    fun deleteAllWatchedEpisodesPerShow(showTraktId: Int)

    @Query("DELETE FROM watched_episodes WHERE id = :episodeId")
    fun deleteWatchedEpisodeById(episodeId: Long)
}