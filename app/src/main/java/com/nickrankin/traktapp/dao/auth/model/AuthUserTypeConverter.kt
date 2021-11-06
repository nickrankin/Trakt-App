package com.nickrankin.traktapp.dao.auth.model

import androidx.room.TypeConverter
import org.threeten.bp.OffsetDateTime

class AuthUserTypeConverter {
    @TypeConverter
    fun convertFromOffsetDateTime(date: OffsetDateTime?): String {
        return date?.toString() ?: ""
    }

    @TypeConverter
    fun convertToOffsetDateTime(dateString: String?): OffsetDateTime? {
        return if(dateString.isNullOrEmpty()) {
            null
        } else {
            OffsetDateTime.parse(dateString)
        }
    }
}