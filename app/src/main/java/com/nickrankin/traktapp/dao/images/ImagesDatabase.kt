package com.nickrankin.traktapp.dao.images

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nickrankin.traktapp.dao.images.model.ImagesTypeConverter
import com.nickrankin.traktapp.dao.images.model.MoviePosterImage
import com.nickrankin.traktapp.dao.images.model.ShowPosterImage

@Database(entities = [ShowPosterImage::class, MoviePosterImage::class], version=1, exportSchema = false)
@TypeConverters(ImagesTypeConverter::class)
abstract class ImagesDatabase: RoomDatabase() {
    abstract fun showPosterImagesDao(): ShowPosterImagesDao
    abstract fun moviePosterImagesDao(): MoviePosterImagesDao


    companion object {
        @Volatile
        private var INSTANCE: ImagesDatabase? = null

        fun getDatabase(context: Context): ImagesDatabase {
            return INSTANCE ?: synchronized(this) {
                val newInstance = Room.databaseBuilder(context.applicationContext,
                    ImagesDatabase::class.java,
                    "images")
                    .build()

                INSTANCE = newInstance

                newInstance
            }
        }
    }
}