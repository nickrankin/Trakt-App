package com.nickrankin.traktapp.dao.calendars

import androidx.room.*
import com.nickrankin.traktapp.dao.calendars.model.ShowCalendarEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ShowCalendarEntryDao {
    @Transaction
    @Query("SELECT * FROM show_calendar_entry")
    fun getShowCalendarEntries(): Flow<List<ShowCalendarEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(calendarEntries: List<ShowCalendarEntry>)

    @Update
    fun update(showCalendarEntry: ShowCalendarEntry)

    @Delete
    fun delete(showCalendarEntry: ShowCalendarEntry)

    @Transaction
    @Query("DELETE FROM show_calendar_entry")
    fun deleteShowCalendarEntries()
}