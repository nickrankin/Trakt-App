package com.nickrankin.traktapp.dao.history.model

import androidx.room.TypeConverter
import com.google.gson.Gson
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

class WatchedHistoryTypeConverter {

    @TypeConverter
    fun convertFromOffsetDateTime(offsetDateTime: OffsetDateTime): String {
        return offsetDateTime.toString()
    }

    @TypeConverter
    fun convertToOffsetDateTime(dateString: String): OffsetDateTime {
        return OffsetDateTime.parse(dateString)
    }
}