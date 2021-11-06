package com.nickrankin.traktapp.dao.images

import androidx.room.*
import com.nickrankin.traktapp.dao.images.model.ShowPosterImage
import kotlinx.coroutines.flow.Flow

@Dao
interface ShowPosterImagesDao {
    @Transaction
    @Query("SELECT * FROM show_posters WHERE tmdb_id = :tmdbId")
    fun getPoster(tmdbId: Int): ShowPosterImage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(showPosterImage: ShowPosterImage)

    @Update
    fun update(showPosterImage: ShowPosterImage)

    @Delete
    fun delete(showPosterImage: ShowPosterImage)

    @Transaction
    @Query("DELETE FROM show_posters")
    fun deleteAllPosters()
}