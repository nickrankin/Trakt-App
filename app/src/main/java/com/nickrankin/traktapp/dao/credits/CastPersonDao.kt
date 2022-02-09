package com.nickrankin.traktapp.dao.credits

import androidx.room.*
import com.nickrankin.traktapp.dao.credits.model.CastPerson
import kotlinx.coroutines.flow.Flow

@Dao
interface CastPersonDao {
    @Transaction
    @Query("SELECT * FROM cast_persons WHERE traktId = :traktId")
    fun getCastPerson(traktId: Int): Flow<CastPerson>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(castPerson: CastPerson)

    @Update
    fun update(castPerson: CastPerson)

    @Delete
    fun delete(castPerson: CastPerson)
}