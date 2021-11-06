package com.nickrankin.traktapp.dao.auth

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nickrankin.traktapp.dao.auth.model.AuthUser
import com.nickrankin.traktapp.dao.auth.model.AuthUserTypeConverter

@Database(entities = [AuthUser::class], version = 1, exportSchema = false)
@TypeConverters(AuthUserTypeConverter::class)
abstract class AuthDatabase: RoomDatabase() {
    abstract fun authUserDao(): AuthUserDao

    companion object {
        @Volatile
        private var INSTANCE: AuthDatabase? = null

        fun getDatabase(context: Context): AuthDatabase {
            return INSTANCE ?: synchronized(this) {
                val newInstance = Room.databaseBuilder(context.applicationContext,
                AuthDatabase::class.java,
                "user_database")
                    .build()

                INSTANCE = newInstance

                newInstance
            }
        }
    }
}