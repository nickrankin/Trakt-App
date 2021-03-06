package com.nickrankin.traktapp.dao.lists

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.uwetrottmann.trakt5.entities.User
import org.threeten.bp.LocalDate
import org.threeten.bp.OffsetDateTime

class TraktListsTypeConverter {
    private val gson = Gson()

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

    @TypeConverter
    fun convertFromUser(user: User): String {
        return  gson.toJson(user)
    }

    @TypeConverter
    fun convertToUser(json: String): User {
        return gson.fromJson(json, User::class.java)
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