package com.nickrankin.traktapp.dao.credits

import androidx.room.*
import com.nickrankin.traktapp.dao.credits.model.TmCastPerson
import com.nickrankin.traktapp.dao.credits.model.TmCrewPerson
import kotlinx.coroutines.flow.Flow

@Dao
interface CrewPersonDao {
    @Transaction
    @Query("SELECT * FROM crew_people WHERE person_trakt_id = :personTraktId")
    fun getCrewPersonCredits(personTraktId: Int): Flow<List<TmCrewPerson>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(creditCharacterPerson: TmCrewPerson)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(creditCharacterPersons: List<TmCrewPerson>)

    @Update
    fun update(creditCharacterPerson: TmCrewPerson)

    @Delete
    fun delete(tmCastPerson: TmCrewPerson)
}