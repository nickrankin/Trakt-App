package com.nickrankin.traktapp.dao.images

import androidx.room.*
import com.nickrankin.traktapp.dao.images.model.Image

@Dao
interface ImagesDao {
    @Transaction
    @Query("SELECT * FROM image WHERE trakt_id = :traktId")
    fun getImage(traktId: Int): Image?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(image: Image)

    @Update
    fun update(image: Image)

    @Delete
    fun delete(image: Image)

    @Transaction
    @Query("DELETE FROM image")
    fun deleteAll()
}