package com.nickrankin.traktapp.dao.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nickrankin.traktapp.dao.history.model.MovieWatchedHistoryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieWatchedHistoryEntryDao {
    @Query("SELECT * FROM movie_history_entry WHERE trakt_id = :traktId")
    fun getWatchedHistoryForMovie(traktId: Int): Flow<List<MovieWatchedHistoryEntry>>

    @Query("SELECT * FROM movie_history_entry ORDER BY watched_date DESC LIMIT 10")
    fun getLatestWatchedHistoryForMovie(): Flow<List<MovieWatchedHistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(movieWatchedHistoryEntries: List<MovieWatchedHistoryEntry>)

    @Query("DELETE FROM movie_history_entry WHERE history_id = :historyId")
    fun deleteHistoryById(historyId: Long)

    @Query("DELETE FROM movie_history_entry WHERE trakt_id = :traktId")
    fun deleteMovieWatchedEntries(traktId: Int)

    @Query("DELETE FROM movie_history_entry")
    fun deleteAllMovieWatchedEntries()
}