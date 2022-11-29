package com.nickrankin.traktapp.dao.credits

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieCastPeopleDao {
    @Transaction
    @Query("SELECT * FROM movie_cast_person WHERE movie_trakt_id = :traktId ORDER BY ordering ASC")
    fun getMovieCast(traktId: Int): Flow<List<MovieCastPerson>>

    @Transaction
    @Query("SELECT * FROM movie_cast_person WHERE person_trakt_id = :traktId")
    fun getPersonMovies(traktId: Int): Flow<List<MovieCastPerson>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(movieCastPersonData: MovieCastPerson)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(movieCastPersonData: List<MovieCastPerson>)


    @Update
    fun update(movieCastPersonData: MovieCastPerson)

    @Delete
    fun delete(movieCastPersonData: MovieCastPerson)

    @Transaction
    @Query("DELETE FROM movie_cast_person WHERE movie_trakt_id = :traktId")
    fun deleteMovieCast(traktId: Int)
}