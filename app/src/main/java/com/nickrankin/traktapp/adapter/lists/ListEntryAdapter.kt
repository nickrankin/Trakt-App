package com.nickrankin.traktapp.adapter.lists

import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.blogc.android.views.ExpandableTextView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.dao.lists.model.TraktListEntry
import com.nickrankin.traktapp.databinding.ListEntryEpisodeItemBinding
import com.nickrankin.traktapp.databinding.ListEntryMovieItemBinding
import com.nickrankin.traktapp.databinding.ListEntryPersonItemBinding
import com.nickrankin.traktapp.databinding.ListEntryShowItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.uwetrottmann.trakt5.enums.Type
import org.threeten.bp.format.DateTimeFormatter

private const val TAG = "ListEntryAdapter"

class ListEntryAdapter constructor(
    private val glide: RequestManager,
    private val tmdbImageLoader: TmdbImageLoader,
    private val sharedPreferences: SharedPreferences,
    private val callback: (traktId: Int, action: String, type: Type, listEntry: TraktListEntry) -> Unit
) : ListAdapter<TraktListEntry, RecyclerView.ViewHolder>(COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_SHOW -> {
                ShowViewHolder(
                    ListEntryShowItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            TYPE_MOVIE -> {
                MovieViewHolder(
                    ListEntryMovieItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            TYPE_EPISODE -> {
                EpisodeViewHolder(
                    ListEntryEpisodeItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            TYPE_PERSON -> {
                PersonViewHolder(
                    ListEntryPersonItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            else -> {
                MovieViewHolder(
                    ListEntryMovieItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = getItem(position)

        when (currentItem.entryData.type) {
            "movie" -> {
                val movieViewHolder = holder as MovieViewHolder

                movieViewHolder.bindings.apply {
                    movieentryTitle.text = currentItem.movie?.title
                    movieentryOverview.text = currentItem.movie?.overview
                    movieentryAdded.text = "Added ${
                        currentItem.entryData.listed_at.format(
                            DateTimeFormatter.ofPattern(
                                sharedPreferences.getString(
                                    "date_format",
                                    "dd/MM/yyyy"
                                )
                            )
                        )
                    }"

                    movieentryOverview.setOnClickListener {
                        (it as ExpandableTextView).apply {
                            toggle()
                        }
                    }

                    tmdbImageLoader.loadImages(
                        currentItem.movie?.trakt_id ?: -1,
                        ImageItemType.MOVIE,
                        currentItem.movie?.tmdb_id,
                        currentItem.movie?.title,
                        currentItem.movie?.language,
                        true,
                        movieentryPoster,
                    movieentryBackdrop,
                    false)

                    root.setOnClickListener {
                        callback(
                            currentItem.movie?.trakt_id!!,
                            ACTION_VIEW,
                            Type.MOVIE,
                            currentItem
                        )
                    }

                    movieentryDeleteButton.setOnClickListener {
                        callback(
                            currentItem.movie?.trakt_id!!,
                            ACTION_REMOVE,
                            Type.MOVIE,
                            currentItem
                        )
                    }
                }
            }
            "show" -> {
                val showViewHolder = holder as ShowViewHolder

                showViewHolder.bindings.apply {
                    showentryTitle.text = currentItem.show?.title
                    showentryOverview.text = currentItem.show?.overview
                    showentryAdded.text = "Added ${
                        currentItem.entryData.listed_at.format(
                            DateTimeFormatter.ofPattern(
                                sharedPreferences.getString(
                                    "date_format",
                                    "dd/MM/yyyy"
                                )
                            )
                        )
                    }"

                    showentryOverview.setOnClickListener {
                        (it as ExpandableTextView).apply {
                            toggle()
                        }
                    }



                    tmdbImageLoader.loadImages(
                        currentItem.show?.trakt_id ?: -1,
                        ImageItemType.SHOW,
                        currentItem.show?.tmdb_id,
                        currentItem.show?.title ?: "",
                        currentItem.show?.language,
                        true,
                        showentryPoster,
                    showentryBackdrop,
                    false)


                    root.setOnClickListener {
                        callback(
                            currentItem.show?.trakt_id!!,
                            ACTION_VIEW,
                            Type.SHOW,
                            currentItem
                        )
                    }

                    showentryDeleteButton.setOnClickListener {
                        callback(
                            currentItem.show?.trakt_id!!,
                            ACTION_REMOVE,
                            Type.SHOW,
                            currentItem
                        )
                    }
                }

            }
            "episode" -> {
                val episodeViewHolder = holder as EpisodeViewHolder

                episodeViewHolder.bindings.apply {
                    episodeentryTitle.text =
                        currentItem.episode?.title + " (${currentItem.episodeShow?.title})"
                    episodeentryOverview.text = currentItem.episode?.overview
                    episodeentrySeasonEpisode.text =
                        "S${currentItem.episode?.season}E${currentItem.episode?.episode}"
                    episodeentryAdded.text = "Added ${
                        currentItem.entryData.listed_at.format(
                            DateTimeFormatter.ofPattern(
                                sharedPreferences.getString(
                                    "date_format",
                                    "dd/MM/yyyy"
                                )
                            )
                        )
                    }"

                    tmdbImageLoader.loadEpisodeImages(
                        currentItem.episodeShow?.trakt_id ?: -1,
                        currentItem.episodeShow?.tmdb_id,
                        currentItem.episodeShow?.trakt_id ?: 0,
                        currentItem.episode?.season,
                        currentItem.episode?.episode,
                        currentItem.episodeShow?.title ?: "",
                        currentItem.episode?.language,
                        true,
                        episodeentryPoster,
                    episodeentryBackdrop,
                    false)


                    episodeentryOverview.setOnClickListener {
                        (it as ExpandableTextView).apply {
                            toggle()
                        }
                    }

                    root.setOnClickListener {
                        callback(
                            // Show Trakt ID, not episodes!
                            currentItem.episodeShow?.trakt_id!!,
                            ACTION_VIEW,
                            Type.EPISODE,
                            currentItem
                        )
                    }

                    episodeentryDeleteButton.setOnClickListener {
                        callback(
                            // Show Trakt ID, not episodes!
                            currentItem.episodeShow?.trakt_id!!,
                            ACTION_REMOVE,
                            Type.EPISODE,
                            currentItem
                        )
                    }
                }
            }
            "person" -> {
                val personViewHolder = holder as PersonViewHolder

                personViewHolder.bindings.apply {
                    personentryName.text = currentItem.person?.name
                    personentryOverview.text = currentItem.person?.bio

                    if (currentItem.person?.birthday != null) {
                        personentryDob.text = "Born ${
                            currentItem.person?.birthday?.format(
                                DateTimeFormatter.ofPattern("dd/MM/yyyy")
                            )
                        }"
                    }

                    personentryAdded.text = "Added ${
                        currentItem.entryData.listed_at.format(
                            DateTimeFormatter.ofPattern(
                                sharedPreferences.getString(
                                    "date_format",
                                    "dd/MM/yyyy"
                                )
                            )
                        )
                    }"

                    personentryOverview.setOnClickListener {
                        (it as ExpandableTextView).apply {
                            toggle()
                        }
                    }

                    root.setOnClickListener {
                        callback(
                            currentItem.movie?.trakt_id!!,
                            ACTION_VIEW,
                            Type.PERSON,
                            currentItem
                        )
                    }
                }
            }
            else -> {
                Log.e(TAG, "onBindViewHolder: Unsupported ViewHolder type:- ${currentItem.entryData.type}!", )
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).entryData.type) {
            "movie" -> {
                TYPE_MOVIE
            }
            "show" -> {
                TYPE_SHOW
            }
            "episode" -> {
                TYPE_EPISODE
            }
            "person" -> {
                TYPE_PERSON
            }
            else -> {
                TYPE_MOVIE
            }
        }
    }

    inner class MovieViewHolder(val bindings: ListEntryMovieItemBinding) :
        RecyclerView.ViewHolder(bindings.root)

    inner class ShowViewHolder(val bindings: ListEntryShowItemBinding) :
        RecyclerView.ViewHolder(bindings.root)

    inner class EpisodeViewHolder(val bindings: ListEntryEpisodeItemBinding) :
        RecyclerView.ViewHolder(bindings.root)

    inner class PersonViewHolder(val bindings: ListEntryPersonItemBinding) :
        RecyclerView.ViewHolder(bindings.root)

    companion object {
        const val TYPE_SHOW = 0
        const val TYPE_MOVIE = 1
        const val TYPE_EPISODE = 2
        const val TYPE_PERSON = 3

        const val ACTION_VIEW = "action_view"
        const val ACTION_REMOVE = "action_remove"

        private val COMPARATOR = object : DiffUtil.ItemCallback<TraktListEntry>() {
            override fun areItemsTheSame(
                oldItem: TraktListEntry,
                newItem: TraktListEntry
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: TraktListEntry,
                newItem: TraktListEntry
            ): Boolean {
                return oldItem.entryData.id == newItem.entryData.id
            }
        }
    }
}