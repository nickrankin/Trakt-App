package com.nickrankin.traktapp.dao.stats.model

import androidx.room.TypeConverter
import org.threeten.bp.OffsetDateTime

class StatsTypeConverter {
    @TypeConverter
    fun convertFromOffsetDateTime(offsetDateTime: OffsetDateTime?): String {
        if(offsetDateTime == null) {
            return ""
        }
        return offsetDateTime.toString()
    }

    @TypeConverter
    fun convertToOffsetDateTime(timestamp: String): OffsetDateTime? {
        if(timestamp .isBlank()) {
            return null
        }
        return OffsetDateTime.parse(timestamp)
    }
}