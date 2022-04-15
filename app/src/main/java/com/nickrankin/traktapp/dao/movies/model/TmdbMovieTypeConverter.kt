package com.nickrankin.traktapp.dao.movies.model

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.uwetrottmann.tmdb2.entities.*
import org.apache.commons.lang3.time.DateFormatUtils
import org.apache.commons.lang3.time.FastDateFormat
import org.apache.commons.lang3.time.FastDateParser
import org.threeten.bp.LocalDate
import org.threeten.bp.OffsetDateTime
import java.util.*
import javax.sql.StatementEvent

class TmdbMovieTypeConverter {
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
    fun convertFromLocalDate(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun convertToLocalDate(dateString: String?): LocalDate? {
        if(dateString == null) {
            return null
        }
        return LocalDate.parse(dateString)
    }

    @TypeConverter
    fun convertFromCountryList(data: List<Country?>): String {
        return gson.toJson(data)
    }

    @TypeConverter
    fun convertToCountriesList(json: String): List<Country?> {
        return gson.fromJson(json, Array<Country?>::class.java).toList()
    }

    @TypeConverter
    fun convertFromBaseCompanyList(data: List<BaseCompany?>): String {
        return gson.toJson(data)
    }

    @TypeConverter
    fun convertToBaseCompanyList(json: String): List<BaseCompany?> {
        return gson.fromJson(json, Array<BaseCompany?>::class.java).toList()
    }


    @TypeConverter
    fun convertFromMovieExternalIds(data: MovieExternalIds?): String {
        return gson.toJson(data)
    }

    @TypeConverter
    fun convertFromMovieExternalIds(json: String): MovieExternalIds? {
        return gson.fromJson(json, MovieExternalIds::class.java)
    }

    @TypeConverter
    fun convertFromGenres(genres: List<Genre?>): String {
        return gson.toJson(genres)
    }

    @TypeConverter
    fun convertToGenres(json: String): List<Genre?> {
        return gson.fromJson(json, Array<Genre?>::class.java).toList()
    }

    @TypeConverter
    fun convertFromImages(ikmages: Images?): String {
        return gson.toJson(ikmages)
    }

    @TypeConverter
    fun convertToImages(json: String): Images? {
        return gson.fromJson(json, Images::class.java)
    }

    @TypeConverter
    fun convertFromStringList(strings: List<String?>): String {
        return gson.toJson(strings)
    }

    @TypeConverter
    fun convertToStringList(json: String):List<String?> {
        return gson.fromJson(json, Array<String?>::class.java).toList()
    }

    @TypeConverter
    fun convertFromDate(date: Date?): String {
        if(date == null) {
            return ""
        }
        return DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(date)
    }

    @TypeConverter
    fun convertToDate(dateString: String): Date? {
        if(dateString.isEmpty()) {
            return null
        }
        return DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.parse(dateString)
    }

    @TypeConverter
    fun convertFromVideos(videos: Videos?): String {
        return gson.toJson(videos)
    }

    @TypeConverter
    fun convertToVideos(json: String): Videos? {
        return gson.fromJson(json, Videos::class.java)
    }

}
