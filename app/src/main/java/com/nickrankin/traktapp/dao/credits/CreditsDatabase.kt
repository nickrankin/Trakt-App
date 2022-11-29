package com.nickrankin.traktapp.dao.credits

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nickrankin.traktapp.dao.credits.model.TmCastPerson
import com.nickrankin.traktapp.dao.credits.model.Person
import com.nickrankin.traktapp.dao.credits.model.TmCrewPerson

@Database(
    entities = [Person::class,
        MovieCastPerson::class,
        ShowCastPerson::class,
               TmCastPerson::class,
               TmCrewPerson::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(CreditsTypeConverter::class)
abstract class CreditsDatabase : RoomDatabase() {
    abstract fun showCastPeopleDao(): ShowCastPeopleDao
    abstract fun movieCastPeopleDao(): MovieCastPeopleDao
    abstract fun personDao(): PersonDao
    abstract fun castPersonDao(): CastPersonDao
    abstract fun crewPersonDao(): CrewPersonDao

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