package com.nickrankin.traktapp.dao.images

import androidx.room.*
import com.nickrankin.traktapp.dao.images.model.MoviePosterImage

@Dao
interface MoviePosterImagesDao {
    @Transaction
    @Query("SELECT * FROM movie_posters WHERE trakt_id = :traktId")
    fun getPoster(traktId: Int): MoviePosterImage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(moviePosterImage: MoviePosterImage)

    @Update
    fun update(moviePosterImage: MoviePosterImage)

    @Delete
    fun delete(moviePosterImage: MoviePosterImage)

    @Transaction
    @Query("DELETE FROM movie_posters")
    fun deleteAllPosters()
}