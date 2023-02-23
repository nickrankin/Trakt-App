package com.nickrankin.traktapp.dao.lists

import androidx.room.*
import com.nickrankin.traktapp.dao.base_entity.EpisodeBaseEnity
import com.nickrankin.traktapp.dao.base_entity.MovieBaseEntity
import com.nickrankin.traktapp.dao.base_entity.PersonBaseEntity
import com.nickrankin.traktapp.dao.base_entity.ShowBaseEntity
import com.nickrankin.traktapp.dao.lists.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ListEntryDao {

    @Transaction
    @Query("SELECT * FROM list_entry")
    fun getTraktListEntries(): Flow<List<TraktListEntry>>

    @Transaction
    @Query("SELECT * FROM list_entry WHERE list_trakt_id = :listTraktId")
    fun getTraktListEntriesById(listTraktId: Int): Flow<List<TraktListEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertListEntry(listEntrys: List<ListEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertListEntry(listEntry: ListEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMovie(movieEntrys: List<MovieBaseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMovie(movieEntry: MovieBaseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertShow(showEntrys: List<ShowBaseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertShow(showEntry: ShowBaseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPerson(personEntrys: List<PersonBaseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPerson(personEntry: PersonBaseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEpisode(episodeEntrys: List<EpisodeBaseEnity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEpisode(episodeEntry: EpisodeBaseEnity)

    @Transaction
    @Query("DELETE FROM list_entry WHERE list_trakt_id = :listTraktId AND item_trakt_id = :itemTraktId")
    fun deleteListEntry(listTraktId: Int, itemTraktId: Int)

    @Transaction
    @Query("DELETE FROM list_entry")
    fun deleteAllListEntries()
}