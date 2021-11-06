package com.nickrankin.traktapp.dao.show

import androidx.room.*
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectedShowDao {
    @Transaction
    @Query("SELECT * FROM collected_shows")
    fun getCollectedShows(): Flow<List<CollectedShow>>

    @Insert
    fun insert(collectedShows: List<CollectedShow>)

    @Update
    fun update(collectedShow: CollectedShow)

    @Delete
    fun delete(collectedShow: CollectedShow)

    @Transaction
    @Query("DELETE FROM collected_shows")
    fun deleteAllShows()
}