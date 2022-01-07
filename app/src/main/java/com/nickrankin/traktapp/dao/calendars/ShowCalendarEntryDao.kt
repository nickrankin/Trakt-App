package com.nickrankin.traktapp.dao.calendars

import androidx.room.*
import com.nickrankin.traktapp.dao.calendars.model.HiddenShowCalendarEntry
import com.nickrankin.traktapp.dao.calendars.model.ShowCalendarEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ShowCalendarEntryDao {
    @Transaction
    @Query("SELECT * FROM show_calendar_entry")
    fun getShowCalendarEntries(): Flow<List<ShowCalendarEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(calendarEntries: List<ShowCalendarEntry>)

    @Query("SELECT * FROM hidden_show_calendar_entries")
    fun getShowHiddenStatus(): Flow<List<HiddenShowCalendarEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateHiddenState(hiddenShowCalendarEntry: HiddenShowCalendarEntry)

    @Update
    fun update(showCalendarEntry: ShowCalendarEntry)

    @Delete
    fun delete(showCalendarEntry: ShowCalendarEntry)

    @Transaction
    @Query("DELETE FROM show_calendar_entry")
    fun deleteShowCalendarEntries()
}