package com.nickrankin.traktapp.dao.credits

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nickrankin.traktapp.dao.credits.model.CreditCharacterPerson
import com.nickrankin.traktapp.dao.credits.model.MovieCastPersonData
import com.nickrankin.traktapp.dao.credits.model.Person
import com.nickrankin.traktapp.dao.credits.model.ShowCastPersonData

@Database(
    entities = [Person::class,
               ShowCastPersonData::class,
               MovieCastPersonData::class,
               CreditCharacterPerson::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(CreditsTypeConverter::class)
abstract class CreditsDatabase : RoomDatabase() {
    abstract fun showCastPeopleDao(): ShowCastPeopleDao
    abstract fun movieCastPeopleDao(): MovieCastPeopleDao
    abstract fun personDao(): PersonDao
    abstract fun creditCharacterPersonDao(): CreditCharacterPersonDao
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