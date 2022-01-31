package com.nickrankin.traktapp.dao.show

import androidx.room.*
import com.nickrankin.traktapp.dao.show.model.TmSeason
import kotlinx.coroutines.flow.Flow

@Dao
interface TmSeasonsDao {
    @Transaction
    @Query("SELECT * FROM seasons WHERE show_trakt_id = :showTraktId")
    fun getSeasonsForShow(showTraktId: Int): Flow<List<TmSeason>>

    @Transaction
    @Query("SELECT * FROM seasons WHERE id = :id AND season_number = :seasonNumber")
    fun getSeason(id: Int, seasonNumber: Int): Flow<TmSeason?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSeasons(seasons: List<TmSeason>)

    @Update
    fun updateSeason(tmSeason: TmSeason)

    @Delete
    fun deleteSeason(tmSeason: TmSeason)

    @Query("DELETE FROM seasons WHERE show_trakt_id = :showTraktId")
    fun deleteAllSeasonsForShow(showTraktId: Int)
}