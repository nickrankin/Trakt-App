package com.nickrankin.traktapp.dao.calendars

import androidx.room.*
import com.nickrankin.traktapp.dao.calendars.model.HiddenShowCalendarEntry
import com.nickrankin.traktapp.dao.calendars.model.ShowBaseCalendarEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ShowCalendarEntryDao {
    @Transaction
    @Query("SELECT * FROM show_calendar_entry")
    fun getShowCalendarEntries(): Flow<List<ShowBaseCalendarEntry>>

    @Transaction
    @Query("SELECT * FROM show_calendar_entry ORDER BY first_aired DESC LIMIT 3")
    fun getLatestShowCalendarEntries(): Flow<List<ShowBaseCalendarEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(calendarEntries: List<ShowBaseCalendarEntry>)

    @Query("SELECT * FROM hidden_show_calendar_entries")
    fun getShowHiddenStatus(): Flow<List<HiddenShowCalendarEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateHiddenState(hiddenShowCalendarEntry: HiddenShowCalendarEntry)

    @Update
    fun update(showCalendarEntry: ShowBaseCalendarEntry)

    @Delete
    fun delete(showCalendarEntry: ShowBaseCalendarEntry)

    @Transaction
    @Query("DELETE FROM show_calendar_entry")
    fun deleteShowCalendarEntries()
}