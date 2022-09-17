package com.nickrankin.traktapp.dao.auth

import androidx.room.*
import com.nickrankin.traktapp.api.services.trakt.model.stats.UserStats
import com.nickrankin.traktapp.dao.auth.model.Stats
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatsDao {
    @Transaction
    @Query("SELECT * FROM user_stats")
    fun getUserStats(): Flow<Stats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUserStats(userStats: Stats)

    @Update
    fun updateUserStats(userStats: Stats)

    @Delete
    fun deleteUserStats(userStats: Stats)
}