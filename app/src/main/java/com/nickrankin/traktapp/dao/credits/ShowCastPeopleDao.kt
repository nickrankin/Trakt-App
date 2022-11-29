package com.nickrankin.traktapp.dao.credits

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShowCastPeopleDao {
    @Transaction
    @Query("SELECT * FROM show_cast_person WHERE show_trakt_id = :traktId AND is_guest_star = 0 ORDER BY ordering ASC")
    fun getShowCast(traktId: Int): Flow<List<ShowCastPerson>>

    @Transaction
    @Query("SELECT * FROM show_cast_person WHERE show_trakt_id = :traktId AND is_guest_star = 1 AND season = :season AND episode = :episode ORDER BY ordering ASC")
    fun getShowGuestStarsCast(traktId: Int, season: Int, episode: Int): Flow<List<ShowCastPerson>>


    @Transaction
    @Query("SELECT * FROM show_cast_person WHERE person_trakt_id = :traktId")
    fun getPersonShows(traktId: Int): Flow<List<ShowCastPerson>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(showCastPersonData: ShowCastPerson)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(showCastPersonData: List<ShowCastPerson>)

    @Update
    fun update(showCastPersonData: ShowCastPerson)

    @Delete
    fun delete(showCastPersonData: ShowCastPerson)

    @Transaction
    @Query("DELETE FROM show_cast_person WHERE show_trakt_id = :traktId")
    fun deleteShowCast(traktId: Int)
}