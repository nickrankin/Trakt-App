package com.nickrankin.traktapp.dao.credits

import androidx.room.*
import com.nickrankin.traktapp.dao.credits.model.MovieCastPersonData
import com.nickrankin.traktapp.dao.credits.model.Person
import com.nickrankin.traktapp.dao.credits.model.ShowCastPersonData
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Transaction
    @Query("SELECT * FROM people WHERE trakt_id = :traktId")
    fun getPerson(traktId: Int): Flow<Person?>

    @Transaction
    @Query("SELECT * FROM movie_cast WHERE person_trakt_id = :traktId")
    fun getPersonMovies(traktId: Int): Flow<List<MovieCastPerson>>

    @Transaction
    @Query("SELECT * FROM show_cast WHERE person_trakt_id = :traktId")
    fun getPersonShows(traktId: Int): Flow<List<ShowCastPerson>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(people: List<Person>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPersonMovies(people: List<MovieCastPersonData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPersonShows(people: List<ShowCastPersonData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(person: Person)

    @Update
    fun update(person: Person)

    @Delete
    fun delete(person: Person)

    @Transaction
    @Query("DELETE FROM people")
    fun deleteCachedPeople()
}