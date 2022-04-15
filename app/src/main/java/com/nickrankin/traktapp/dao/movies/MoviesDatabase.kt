package com.nickrankin.traktapp.dao.movies

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nickrankin.traktapp.dao.movies.model.CollectedMovie
import com.nickrankin.traktapp.dao.movies.model.TmMovie
import com.nickrankin.traktapp.dao.movies.model.TmdbMovieTypeConverter
import com.nickrankin.traktapp.dao.movies.model.WatchedMovie
import com.nickrankin.traktapp.dao.ratings.RatedMoviesDao
import com.nickrankin.traktapp.dao.ratings.model.RatedMovie


@Database(entities = [TmMovie::class, CollectedMovie::class, WatchedMovie::class, RatedMovie::class], version=1, exportSchema = false)
@TypeConverters(TmdbMovieTypeConverter::class)
abstract class MoviesDatabase: RoomDatabase() {
    abstract fun tmMovieDao(): TmMovieDao
    abstract fun collectedMovieDao(): CollectedMoviesDao
    abstract fun watchedMoviesDao(): WatchedMoviesDao
    abstract fun ratedMovieDao(): RatedMoviesDao

    companion object {
        @Volatile
        private var INSTANCE: MoviesDatabase? = null

        fun getDatabase(context: Context): MoviesDatabase {
            return INSTANCE ?: synchronized(this) {
                val newInstance = Room.databaseBuilder(context.applicationContext,
                    MoviesDatabase::class.java,
                    "movies")
                    .build()

                INSTANCE = newInstance

                newInstance
            }
        }
    }
}