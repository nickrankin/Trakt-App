package com.nickrankin.traktapp.dao.lists

import androidx.room.*
import com.nickrankin.traktapp.dao.lists.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ListEntryDao {
    @Transaction
    @Query("SELECT * FROM list_entries WHERE trakt_list_id = :traktListId")
    fun getListEntries(traktListId: Int): Flow<List<TraktListEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertListEntry(listEntry: ListEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertListEntries(listEntrys: List<ListEntry>)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMovie(movieEntry: MovieEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertShow(showEntry: ShowEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPerson(personEntry: PersonEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEpisode(episodeEntry: EpisodeEntry)

    @Transaction
    @Query("DELETE FROM list_entries WHERE trakt_list_id = :traktListId AND list_entry_trakt_id = :entryId")
    fun deleteListEntry(traktListId: Int, entryId: Int)

}