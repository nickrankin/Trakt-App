package com.nickrankin.traktapp.dao.lists

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nickrankin.traktapp.dao.base_entity.EpisodeBaseEnity
import com.nickrankin.traktapp.dao.base_entity.MovieBaseEntity
import com.nickrankin.traktapp.dao.base_entity.PersonBaseEntity
import com.nickrankin.traktapp.dao.base_entity.ShowBaseEntity
import com.nickrankin.traktapp.dao.lists.model.*
import com.nickrankin.traktapp.dao.refresh.LastRefreshedAt
import com.nickrankin.traktapp.dao.refresh.LastRefreshedAtDao

@Database(entities = [TraktList::class, ListEntry::class, MovieBaseEntity::class, ShowBaseEntity::class, PersonBaseEntity::class, EpisodeBaseEnity::class, LastRefreshedAt::class], version = 1, exportSchema = false)
@TypeConverters(TraktListsTypeConverter::class)
abstract class TraktListsDatabase : RoomDatabase() {
    abstract fun traktListDao(): TraktListDao
    abstract fun listEntryDao(): ListEntryDao
    abstract fun lastRereshedAtDao(): LastRefreshedAtDao

    companion object {
        @Volatile
        private var INSTANCE: TraktListsDatabase? = null

        fun getDatabase(context: Context): TraktListsDatabase {
            return INSTANCE ?: synchronized(this) {
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    TraktListsDatabase::class.java,
                    "trakt_lists"
                )
                    .build()

                INSTANCE = newInstance

                newInstance
            }
        }
    }
}
