package com.nickrankin.traktapp.dao.credits

import androidx.room.*
import com.nickrankin.traktapp.dao.credits.model.CreditCharacterPerson
import com.uwetrottmann.trakt5.enums.Type
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCharacterPersonDao {
    @Transaction
    @Query("SELECT * FROM characters WHERE person_trakt_id = :personTraktId AND type = :type ")
    fun getPersonCredits(personTraktId: Int, type: Type): Flow<List<CreditCharacterPerson>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(creditCharacterPerson: CreditCharacterPerson)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(creditCharacterPersons: List<CreditCharacterPerson>)

    @Update
    fun update(creditCharacterPerson: CreditCharacterPerson)

    @Delete
    fun delete(creditCharacterPerson: CreditCharacterPerson)
}