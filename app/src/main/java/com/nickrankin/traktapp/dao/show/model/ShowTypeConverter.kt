package com.nickrankin.traktapp.dao.show.model

import androidx.room.TypeConverter
import org.threeten.bp.OffsetDateTime

class ShowTypeConverter {
    @TypeConverter
    fun convertFromOffsetDatetime(date: OffsetDateTime?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun convertToOffsetDateTime(dateString: String?): OffsetDateTime? {
        if(dateString == null) {
            return null
        }
        return OffsetDateTime.parse(dateString)
    }
}