package com.nickrankin.traktapp.dao.credits

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nickrankin.traktapp.dao.credits.model.CastPerson
import com.nickrankin.traktapp.dao.credits.model.ShowCastPersonData

@Database(
    entities = [CastPerson::class,
               ShowCastPersonData::class],
    version = 1,
    exportSchema = false
)
abstract class CreditsDatabase : RoomDatabase() {
    abstract fun castPersonDao(): CastPersonDao
    abstract fun showCastPeopleDao(): ShowCastPeopleDao

    companion object {
        @Volatile
        private var INSTANCE: CreditsDatabase? = null

        fun getDatabase(context: Context): CreditsDatabase {
            return INSTANCE ?: synchronized(this) {
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    CreditsDatabase::class.java,
                    "credits"
                ).build()

                INSTANCE = newInstance

                newInstance
            }
        }
    }
}