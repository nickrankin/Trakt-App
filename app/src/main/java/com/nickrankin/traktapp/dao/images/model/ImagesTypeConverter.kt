package com.nickrankin.traktapp.dao.images.model

import androidx.room.TypeConverter
import org.threeten.bp.OffsetDateTime

class ImagesTypeConverter {
    @TypeConverter
    fun convertFromOffsetDateTime(date: OffsetDateTime?): String {
        return date.toString()
    }

    @TypeConverter
    fun convertToOffsetDateTime(dateString: String): OffsetDateTime {
        return OffsetDateTime.parse(dateString)
    }
}