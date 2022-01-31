package com.nickrankin.traktapp.dao.show

import androidx.room.*
import com.nickrankin.traktapp.dao.show.model.LastRefreshedShow
import kotlinx.coroutines.flow.Flow

@Dao
interface LastRefreshedShowDao {
    @Transaction
    @Query("SELECT * FROM show_last_refresh WHERE trakt_id = :traktId")
    fun getShowLastRefreshDate(traktId: Int): Flow<LastRefreshedShow?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(lastRefreshedShow: LastRefreshedShow)

    @Delete
    fun delete(lastRefreshedShow: LastRefreshedShow)
}