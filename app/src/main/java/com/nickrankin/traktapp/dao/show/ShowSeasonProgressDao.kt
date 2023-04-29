package com.nickrankin.traktapp.dao.show

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nickrankin.traktapp.dao.show.model.SeasonProgress
import com.nickrankin.traktapp.dao.show.model.ShowAndSeasonProgress
import com.nickrankin.traktapp.dao.show.model.ShowsProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface ShowSeasonProgressDao {
    @Query("SELECT * FROM show_progress ORDER BY last_watched_at DESC")
    fun getShowSeasonProgress(): Flow<List<ShowAndSeasonProgress>>

    @Query("SELECT * FROM show_progress WHERE show_trakt_id = :showTraktId ORDER BY last_watched_at DESC")
    fun getShowSeasonProgress(showTraktId: Int): Flow<ShowAndSeasonProgress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertShowProgress(showProgress: ShowsProgress)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertShowProgress(showProgress: List<ShowsProgress>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSeasonProgress(seasonProgress: SeasonProgress)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSeasonProgress(SeasonProgress: List<SeasonProgress>)

    @Query("DELETE FROM show_progress")
    fun clearShowProgress()

    @Query("DELETE FROM show_season_progress")
    fun deleteSeasonProgress()

}