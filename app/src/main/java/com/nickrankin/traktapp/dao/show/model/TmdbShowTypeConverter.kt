package com.nickrankin.traktapp.dao.show.model

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.uwetrottmann.tmdb2.entities.*
import org.apache.commons.lang3.time.DateFormatUtils
import org.apache.commons.lang3.time.FastDateFormat
import org.apache.commons.lang3.time.FastDateParser
import java.util.*
import javax.sql.StatementEvent

class TmdbShowTypeConverter {
    private val gson = Gson()

    @TypeConverter
    fun convertFromCredits(credits: Credits?): String {
        return gson.toJson(credits)
    }

    @TypeConverter
    fun convertToCredits(json: String): Credits? {
        return gson.fromJson(json, Credits::class.java)
    }

    @TypeConverter
    fun convertFromTvExternalIds(tvExternalIds: TvExternalIds?): String {
        return gson.toJson(tvExternalIds)
    }

    @TypeConverter
    fun convertToTvExternalIds(json: String): TvExternalIds? {
        return gson.fromJson(json, TvExternalIds::class.java)
    }

    @TypeConverter
    fun convertFromTvSeasonExternalIds(tvSeasonExternalIds: TvSeasonExternalIds?): String {
        return gson.toJson(tvSeasonExternalIds)
    }

    @TypeConverter
    fun convertToTvSeasonExternalIds(json: String): TvSeasonExternalIds? {
        return gson.fromJson(json, TvSeasonExternalIds::class.java)
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
    fun convertFromBaseTvEpisode(baseTvEpisode: BaseTvEpisode?): String {
        return gson.toJson(baseTvEpisode)
    }

    @TypeConverter
    fun convertToBaseTvEpisode(json: String): BaseTvEpisode? {
        return gson.fromJson(json, BaseTvEpisode::class.java)
    }

    @TypeConverter
    fun convertFromNetworks(networks: List<Network?>): String {
        return gson.toJson(networks)
    }

    @TypeConverter
    fun convertToNetworks(json: String): List<Network?> {
        return gson.fromJson(json, Array<Network?>::class.java).toList()
    }

    @TypeConverter
    fun convertFromVideos(videos: Videos?): String {
        return gson.toJson(videos)
    }

    @TypeConverter
    fun convertToVideos(json: String): Videos? {
        return gson.fromJson(json, Videos::class.java)
    }

    @TypeConverter
    fun convertFromTvEpisodeExternalIds(tvEpisodeExternalIds: TvEpisodeExternalIds?): String {
        return gson.toJson(tvEpisodeExternalIds)
    }

    @TypeConverter
    fun convertToTvEpisodeIds(json: String): TvEpisodeExternalIds? {
        return gson.fromJson(json, TvEpisodeExternalIds::class.java)
    }

    @TypeConverter
    fun convertFromCrewList(crewMembers: List<CrewMember>?): String {
        return gson.toJson(crewMembers)
    }

    @TypeConverter
    fun convertToCrewList(json: String): List<CrewMember>? {
        return gson.fromJson(json, Array<CrewMember>::class.java).toList()
    }

    @TypeConverter
    fun convertFromCastList(castMembers: List<CastMember>?): String {
        return gson.toJson(castMembers)
    }

    @TypeConverter
    fun convertToCastList(json: String): List<CastMember>? {
        return gson.fromJson(json, Array<CastMember>::class.java).toList()
    }

    @TypeConverter
    fun convertFromPersons(persons: List<Person?>): String {
        return gson.toJson(persons)
    }

    @TypeConverter
    fun convertToPersons(json: String): List<Person?> {
        return gson.fromJson(json, Array<Person?>::class.java).toList()
    }

}
