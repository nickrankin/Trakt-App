package com.nickrankin.traktapp.dao.credits

import androidx.room.TypeConverter
import org.threeten.bp.LocalDate
import org.threeten.bp.OffsetDateTime

class CreditsTypeConverter {
    @TypeConverter
    fun convertFromOffsetDateTime(offsetDateTime: OffsetDateTime?): String {
        return offsetDateTime.toString()
    }

    @TypeConverter
    fun convertToOffsetDatetime(timestamp: String): OffsetDateTime? {
        return OffsetDateTime.parse(timestamp)
    }

    @TypeConverter
    fun convertFromLocalDate(localDate: LocalDate?): String? {
        if(localDate == null) {
            return null
        }
        return  localDate.toString()
    }

    @TypeConverter
    fun convertToLocalDate(timeStamp: String?): LocalDate? {
        if(timeStamp == null || timeStamp == "null") {
            return null
        }
        return LocalDate.parse(timeStamp)
    }

}