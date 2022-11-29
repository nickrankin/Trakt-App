package com.nickrankin.traktapp.dao.credits

import androidx.room.*
import com.nickrankin.traktapp.dao.credits.model.TmCastPerson
import kotlinx.coroutines.flow.Flow

@Dao
interface CastPersonDao {
    @Transaction
    @Query("SELECT * FROM cast_people WHERE person_trakt_id = :personTraktId")
    fun getCastPersonCredits(personTraktId: Int): Flow<List<TmCastPerson>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(creditCharacterPerson: TmCastPerson)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(creditCharacterPersons: List<TmCastPerson>)

    @Update
    fun update(creditCharacterPerson: TmCastPerson)

    @Delete
    fun delete(tmCastPerson: TmCastPerson)
}