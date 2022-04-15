package com.nickrankin.traktapp.dao.movies

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nickrankin.traktapp.dao.movies.model.WatchedMovie
import com.nickrankin.traktapp.dao.show.WatchedEpisodePageKeyDao
import com.nickrankin.traktapp.dao.show.model.*

@Database(
    entities = [
        WatchedMovie::class,
        WatchedMoviePageKey::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(ShowTypeConverter::class, TmdbShowTypeConverter::class)
abstract class WatchedMoviesMediatorDatabase : RoomDatabase() {
    abstract fun watchedMoviesDao(): WatchedMoviesDao
    abstract fun watchedMoviePageKeyDao(): WatchedMoviePageKeyDao

    companion object {
        @Volatile
        private var INSTANCE: WatchedMoviesMediatorDatabase? = null

        fun getDatabase(context: Context): WatchedMoviesMediatorDatabase {
            return INSTANCE ?: synchronized(this) {
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    WatchedMoviesMediatorDatabase::class.java,
                    "watched_movies"
                ).build()

                INSTANCE = newInstance

                newInstance
            }
        }
    }
}