package com.nickrankin.traktapp.dao.show

import androidx.room.*
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import kotlinx.coroutines.flow.Flow

@Dao
interface TmEpisodesDao {
    @Transaction
    @Query("SELECT * FROM episodes WHERE show_tmdb_id = :showTmdbId AND season_number = :seasonNumber")
    fun getEpisodes(showTmdbId: Int, seasonNumber: Int): Flow<List<TmEpisode>>

    @Transaction
    @Query("SELECT * FROM episodes WHERE show_tmdb_id = :showTmdbId AND season_number = :seasonNumber AND episode_number = :episodeNumber")
    fun getEpisode(showTmdbId: Int, seasonNumber: Int, episodeNumber: Int): Flow<TmEpisode?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(episodes: List<TmEpisode>)

    @Update
    fun update(tmEpisode: TmEpisode)

    @Delete
    fun delete(tmEpisode: TmEpisode)

    @Query("DELETE FROM episodes WHERE show_tmdb_id = :showTmdbId AND season_number = :seasonNumber")
    fun deleteEpisodes(showTmdbId: Int, seasonNumber: Int)
}