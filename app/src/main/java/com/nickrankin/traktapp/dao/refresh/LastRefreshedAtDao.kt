package com.nickrankin.traktapp.dao.refresh

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LastRefreshedAtDao {
    @Transaction
    @Query("SELECT * FROM last_refreshed WHERE refresh_type = :type")
    fun getLastRefreshed(type: RefreshType): Flow<LastRefreshedAt?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLastRefreshStats(lastRefreshedAt: LastRefreshedAt)
}