package com.nickrankin.traktapp.dao.calendars.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_show_calendar_entries")
data class HiddenShowCalendarEntry(@PrimaryKey val showTmdbId: Int, val isHidden: Boolean)