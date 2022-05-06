package com.nickrankin.traktapp.dao.credits

import androidx.room.*
import com.nickrankin.traktapp.dao.credits.model.CastPerson
import kotlinx.coroutines.flow.Flow

@Dao
interface CastPersonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(castPerson: CastPerson)

    @Update
    fun update(castPerson: CastPerson)

    @Delete
    fun delete(castPerson: CastPerson)
}