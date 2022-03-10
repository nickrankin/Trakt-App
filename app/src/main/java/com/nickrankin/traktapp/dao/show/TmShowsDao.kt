package com.nickrankin.traktapp.dao.show

import androidx.room.*
import com.nickrankin.traktapp.dao.show.model.TmShow
import kotlinx.coroutines.flow.Flow

@Dao
interface TmShowsDao {
    @Transaction
    @Query("SELECT * FROM shows WHERE trakt_id = :id")
    fun getShow(id: Int): Flow<TmShow?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertShow(tmShow: TmShow)

    @Update
    fun updateShow(tmShow: TmShow)

    @Delete
    fun delete(tmShow: TmShow)

    @Query("DELETE FROM shows")
    fun deleteAllShows()
}