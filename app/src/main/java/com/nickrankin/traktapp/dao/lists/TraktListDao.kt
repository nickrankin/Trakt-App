package com.nickrankin.traktapp.dao.lists

import androidx.room.*
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.dao.lists.model.TraktListAndEntries
import kotlinx.coroutines.flow.Flow

@Dao
interface TraktListDao {
    @Transaction
    @Query("SELECT * FROM lists")
    fun getAllTraktLists(): Flow<List<TraktList>>

    @Transaction
    @Query("SELECT * FROM lists WHERE trakt_id = :traktId")
    fun getTraktList(traktId: Int): Flow<TraktList?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(lists: List<TraktList>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSingle(list: TraktList)

    @Update
    fun update(traktList: TraktList)

    @Delete
    fun delete(traktList: TraktList)

    @Transaction
    @Query("DELETE FROM lists where trakt_id = :traktId")
    fun deleteListById(traktId: String)

    @Transaction
    @Query("DELETE FROM lists")
    fun deleteTraktListsFromCache()
}