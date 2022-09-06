package com.nickrankin.traktapp.repo.ratings

import androidx.room.PrimaryKey
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.stats.model.RatingsEpisodesStats
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.uwetrottmann.trakt5.entities.EpisodeIds
import com.uwetrottmann.trakt5.entities.SyncEpisode
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.enums.Rating
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

class EpisodeRatingsRepository @Inject constructor(private val traktApi: TraktApi, private val showsDatabase: ShowsDatabase) {
    private val episodeRatingsDao = showsDatabase.ratedEpisodesStatsDao()
    val episodeRatings = episodeRatingsDao.getRatingsStats()

    suspend fun addRatings(newRating: Int, episodeDataModel: EpisodeDataModel?, episodeTraktId: Int): Resource<Pair<SyncResponse, Int>> {
        if(episodeDataModel == null) {
            return Resource.Error(RuntimeException("EpisodeDataModel cannot be null"), null)
        }

        try {
            val syncItems = SyncItems().apply {
                episodes = listOf(
                    SyncEpisode().rating(
                        Rating.fromValue(newRating)
                    )
                        .id(EpisodeIds.trakt(episodeTraktId))
                )
            }

            val response = traktApi.tmSync().addRatings(syncItems)

            showsDatabase.withTransaction {
                episodeRatingsDao.insert(
                    RatingsEpisodesStats(
                        episodeTraktId,
                        episodeDataModel.showTraktId,
                        episodeDataModel.seasonNumber,
                        episodeDataModel.episodeNumber,
                        newRating,
                        OffsetDateTime.now()
                    )
                )
            }

            return Resource.Success(Pair(response, newRating))

        } catch (t: Throwable) {
            return Resource.Error(t, null)
        }
    }

    suspend fun resetRating(episodeTraktId: Int): Resource<SyncResponse> {
        return try {
            val syncItems = SyncItems().apply {
                episodes = listOf(
                    SyncEpisode().id(EpisodeIds.trakt(episodeTraktId))
                )
            }

            val response = traktApi.tmSync().deleteRatings(syncItems)

            showsDatabase.withTransaction {
                episodeRatingsDao.deleteRatingsStatByEpisodeId(episodeTraktId)
            }

            Resource.Success(response)
        } catch(t: Throwable) {

            Resource.Error(t, null)
        }
    }
}