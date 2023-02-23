package com.nickrankin.traktapp.dao.show

import androidx.room.*
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectedShowDao {
    @Transaction
    @Query("SELECT * FROM collected_shows")
    fun getCollectedShows(): Flow<List<CollectedShow>>

    @Transaction
    @Query("SELECT * FROM collected_shows WHERE show_trakt_id = :traktId")
    fun getCollectedShow(traktId: Int): Flow<CollectedShow?>

    @Insert
    fun insert(collectedShow: CollectedShow)

    @Insert
    fun insert(collectedShows: List<CollectedShow>)

    @Update
    fun update(collectedShow: CollectedShow)

    @Delete
    fun delete(collectedShow: CollectedShow)

    @Query("DELETE FROM collected_shows WHERE show_trakt_id = :traktId")
    fun deleteShowById(traktId: Int)

    @Transaction
    @Query("DELETE FROM collected_shows")
    fun deleteAllShows()
}