package com.nickrankin.traktapp.dao.lists

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nickrankin.traktapp.dao.lists.model.TraktList

@Database(entities = [TraktList::class], version = 1, exportSchema = false)
@TypeConverters(TraktListsTypeConverter::class)
abstract class TraktListsDatabase : RoomDatabase() {
    abstract fun traktListDao(): TraktListDao

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
