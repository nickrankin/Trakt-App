package com.nickrankin.traktapp.dao.credits

import androidx.room.*
import com.nickrankin.traktapp.dao.credits.model.ShowCastPersonData
import kotlinx.coroutines.flow.Flow

@Dao
interface ShowCastPeopleDao {
    @Transaction
    @Query("SELECT * FROM show_cast WHERE showTraktId = :traktId AND isGuestStar = :showGuestStars ORDER BY ordering ASC")
    fun getShowCast(traktId: Int, showGuestStars: Boolean): Flow<List<ShowCastPerson>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(showCastPersonData: ShowCastPersonData)

    @Update
    fun update(showCastPersonData: ShowCastPersonData)

    @Delete
    fun delete(showCastPersonData: ShowCastPersonData)

    @Transaction
    @Query("DELETE FROM show_cast WHERE showTraktId = :traktId")
    fun deleteShowCast(traktId: Int)
}