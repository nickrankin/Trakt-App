package com.nickrankin.traktapp.dao.credits

import androidx.room.*
import com.nickrankin.traktapp.dao.credits.model.ShowCastPersonData
import kotlinx.coroutines.flow.Flow

@Dao
interface ShowCastPeopleDao {
    @Transaction
    @Query("SELECT * FROM show_cast WHERE show_trakt_id = :traktId  AND is_guest_star = :showGuestStars ORDER BY ordering ASC")
    fun getShowCast(traktId: Int, showGuestStars: Boolean): Flow<List<ShowCastPerson>>

    @Transaction
    @Query("SELECT * FROM show_cast WHERE person_trakt_id = :traktId")
    fun getPersonShows(traktId: Int): Flow<List<ShowCastPerson>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(showCastPersonData: ShowCastPersonData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(showCastPersonData: List<ShowCastPersonData>)

    @Update
    fun update(showCastPersonData: ShowCastPersonData)

    @Delete
    fun delete(showCastPersonData: ShowCastPersonData)

    @Transaction
    @Query("DELETE FROM show_cast WHERE show_trakt_id = :traktId")
    fun deleteShowCast(traktId: Int)
}