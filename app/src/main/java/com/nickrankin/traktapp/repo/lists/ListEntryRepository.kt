package com.nickrankin.traktapp.repo.lists

import android.content.SharedPreferences
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.lists.TraktListsDatabase
import com.nickrankin.traktapp.dao.lists.model.*
import com.nickrankin.traktapp.dao.lists.model.ListEntry
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.Type
import javax.inject.Inject

class ListEntryRepository @Inject constructor(private val traktApi: TraktApi, private val sharedPreferences: SharedPreferences, private val listsDatabase: TraktListsDatabase) {
    private val traktListEntryDao = listsDatabase.listEntryDao()

    fun getListEntries(listId: Int, shouldRefresh: Boolean) = networkBoundResource(
        query = {
            traktListEntryDao.getListEntries(listId)
        }, fetch = {
            traktApi.tmUsers().listItems(UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "")), listId.toString(), Extended.FULL)
        },
        shouldFetch = { listEntries ->
            shouldRefresh || listEntries.isEmpty()
        },
        saveFetchResult = { listEntries ->
            listsDatabase.withTransaction {
                traktListEntryDao.getListEntries(listId)
                traktListEntryDao.insertListEntries(getListEntries(listId, listEntries))
            }
        }
    )

    private suspend fun getListEntries(listId: Int, listEntries: List<com.uwetrottmann.trakt5.entities.ListEntry>): List<ListEntry> {
        val listEntriesConverted: MutableList<ListEntry> = mutableListOf()

        listEntries.map { listEntry ->
            var traktId = 0
            var showTraktId: Int? = null

            when(listEntry.type) {
                "movie" -> {
                    traktId = listEntry.movie?.ids?.trakt ?: -1
                    saveMovie(listEntry.movie!!)
                }
                "show" -> {
                    traktId = listEntry.show?.ids?.trakt ?: -1
                    saveShow(listEntry.show!!)

                }
                "episode" -> {
                    traktId = listEntry.episode?.ids?.trakt ?: -1
                    showTraktId = listEntry.show?.ids?.trakt
                    saveShow(listEntry.show!!)
                    saveEpisode(listEntry.episode!!)
                }
                "person" -> {
                    traktId = listEntry.person?.ids?.trakt ?: -1
                    savePerson(listEntry.person!!)

                }
            }

            listEntriesConverted.add(
            ListEntry(
                listEntry.id,
                listId,
                showTraktId,
                traktId,
                listEntry.listed_at,
                listEntry.rank,
                listEntry.type
            ))
        }

        return listEntriesConverted
    }

    suspend fun removeEntry(listTraktId: Int, listEntryTraktId: Int, type: Type): Resource<SyncResponse?> {
        return try {
            val userSlug = UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "null"))
            val syncItems = SyncItems()

            when(type) {
                Type.MOVIE -> {
                    syncItems.apply {
                        movies = listOf(
                            SyncMovie().id(MovieIds.trakt(listEntryTraktId))
                        )
                    }
                }
                Type.SHOW -> {
                    syncItems.apply {
                        shows = listOf(
                            SyncShow().id(ShowIds.trakt(listEntryTraktId))
                        )
                    }
                }
                Type.EPISODE -> {
                    syncItems.apply {
                        episodes = listOf(
                            SyncEpisode().id(EpisodeIds.trakt(listEntryTraktId))
                        )
                    }
                }
                Type.PERSON -> {
                    syncItems.apply {
                        people = listOf(
                            SyncPerson().id(PersonIds.trakt(listEntryTraktId))
                        )
                    }
                }
            }

            val response = traktApi.tmUsers().deleteListItems(userSlug, listTraktId.toString(), syncItems)

            listsDatabase.withTransaction {
                traktListEntryDao.deleteListEntry(listTraktId, listEntryTraktId)
            }

            Resource.Success(response)

        } catch(t: Throwable) {
            Resource.Error(t, null)
        }
    }


    private suspend fun saveMovie(movie: Movie) {
        listsDatabase.withTransaction {
            traktListEntryDao.insertMovie(
                MovieEntry(
                    movie.ids?.trakt ?: -1,
                    movie.ids?.tmdb,
                    movie.title,
                    movie.overview,
                    movie.released,
                    movie.runtime,
                    movie.tagline
                )
            )
        }
    }

    private suspend fun saveShow(show: Show) {
        listsDatabase.withTransaction {
            traktListEntryDao.insertShow(
                ShowEntry(
                    show.ids?.trakt ?: -1,
                    show.ids?.tmdb,
                    show.title,
                    show.overview,
                    show.first_aired,
                    show.runtime
                )
            )
        }

    }

    private suspend fun saveEpisode(episode: Episode) {
        listsDatabase.withTransaction {
            traktListEntryDao.insertEpisode(
                EpisodeEntry(
                    episode.ids?.trakt ?: -1,
                    episode.ids?.tmdb,
                    episode.title,
                    episode.overview,
                    episode.first_aired,
                    episode.runtime,
                    episode.season,
                    episode.number
                )
            )
        }

    }

    private suspend fun savePerson(person: Person) {
        listsDatabase.withTransaction {
            traktListEntryDao.insertPerson(
                PersonEntry(
                    person.ids?.trakt ?: -1,
                    person.ids?.tmdb,
                    person.name,
                    person.biography,
                    person.birthday,
                    person.death
                )
            )
        }
    }
}