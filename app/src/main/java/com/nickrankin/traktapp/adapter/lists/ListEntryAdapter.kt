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
    private val callback: (traktId: Int?, action: String, type: Type, listEntry: TraktListEntry) -> Unit
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
            Type.MOVIE -> {
                val movieViewHolder = holder as MovieViewHolder

                movieViewHolder.bindings.apply {
                    movieentryTitle.text = currentItem.movie?.title
//                    movieentryOverview.text = currentItem.movie?.overview
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


                    tmdbImageLoader.loadImages(
                        currentItem.movie?.trakt_id ?: -1,
                        ImageItemType.MOVIE,
                        currentItem.movie?.tmdb_id,
                        currentItem.movie?.title,
                        currentItem.movie?.language,
                        true,
                        movieentryPoster,
                        null,
                        false
                    )

                    root.setOnClickListener {
                        callback(
                            currentItem.movie?.trakt_id,
                            ACTION_VIEW,
                            Type.MOVIE,
                            currentItem
                        )
                    }

//                    movieentryDeleteButton.setOnClickListener {
//                        callback(
//                            currentItem.movie?.trakt_id,
//                            ACTION_REMOVE,
//                            Type.MOVIE,
//                            currentItem
//                        )
//                    }
                }
            }
            Type.SHOW -> {
                val showViewHolder = holder as ShowViewHolder

                showViewHolder.bindings.apply {

                    showentryPoster.setImageDrawable(null)

                    showentryTitle.text = currentItem.show?.title
//                    showentryOverview.text = currentItem.show?.overview
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


                    tmdbImageLoader.loadImages(
                        currentItem.show?.trakt_id ?: -1,
                        ImageItemType.SHOW,
                        currentItem.show?.tmdb_id,
                        currentItem.show?.title ?: "",
                        currentItem.show?.language,
                        true,
                        showentryPoster,
                        null,
                        false
                    )


                    root.setOnClickListener {
                        callback(
                            currentItem.show?.trakt_id,
                            ACTION_VIEW,
                            Type.SHOW,
                            currentItem
                        )
                    }

//                    showentryDeleteButton.setOnClickListener {
//                        callback(
//                            currentItem.show?.trakt_id,
//                            ACTION_REMOVE,
//                            Type.SHOW,
//                            currentItem
//                        )
//                    }
                }

            }
            Type.EPISODE -> {
                val episodeViewHolder = holder as EpisodeViewHolder

                episodeViewHolder.bindings.apply {

                    episodeentryPoster.setImageDrawable(null)
                    episodeentryTitle.text =
                        "${currentItem.episode?.title} S${currentItem.episode?.season_number}E${currentItem.episode?.episode_number} (${currentItem.episodeShow?.title})"

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
                        currentItem.episode?.season_number,
                        currentItem.episode?.episode_number,
                        currentItem.episodeShow?.title ?: "",
                        currentItem.episode?.language,
                        true,
                        episodeentryPoster,
                        null,
                        false
                    )

                    root.setOnClickListener {
                        callback(
                            // Show Trakt ID, not episodes!
                            currentItem.episodeShow?.trakt_id,
                            ACTION_VIEW,
                            Type.EPISODE,
                            currentItem
                        )
                    }

//                    episodeentryDeleteButton.setOnClickListener {
//                        callback(
//                            // Show Trakt ID, not episodes!
//                            currentItem.episodeShow?.trakt_id,
//                            ACTION_REMOVE,
//                            Type.EPISODE,
//                            currentItem
//                        )
//                    }
                }
            }
            Type.PERSON -> {
                val personViewHolder = holder as PersonViewHolder

                personViewHolder.bindings.apply {
                    personentryTitle.text = currentItem.person?.title
//                    personentryOverview.text = currentItem.person?

//                    if (currentItem.person?.birthday != null) {
//                        personentryDob.text = "Born ${
//                            currentItem.person?.birthday?.format(
//                                DateTimeFormatter.ofPattern("dd/MM/yyyy")
//                            )
//                        }"
//                    }

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
                Log.e(
                    TAG,
                    "onBindViewHolder: Unsupported ViewHolder type:- ${currentItem.entryData.type}!",
                )
            }
        }
    }


    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).entryData.type) {
            Type.MOVIE -> {
                TYPE_MOVIE
            }
            Type.SHOW -> {
                TYPE_SHOW
            }
            Type.EPISODE -> {
                TYPE_EPISODE
            }
            Type.PERSON -> {
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