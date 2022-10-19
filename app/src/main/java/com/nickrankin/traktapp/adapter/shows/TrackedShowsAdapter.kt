package com.nickrankin.traktapp.adapter.shows

import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.blogc.android.views.ExpandableTextView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.dao.show.model.TrackedEpisode
import com.nickrankin.traktapp.dao.show.model.TrackedShowWithEpisodes
import com.nickrankin.traktapp.databinding.TrackedShowListItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

private const val TAG = "TrackedShowsAdapter"
class TrackedShowsAdapter(private val tmdbImageLoader: TmdbImageLoader, private val callback: (trackedShow: TrackedShowWithEpisodes) -> Unit, private val upcomingEpisodesCallback: (showTitle: String?, episodes: List<TrackedEpisode?>) -> Unit): MediaEntryBaseAdapter<TrackedShowWithEpisodes>(
    null, COMPARATOR) {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val selectedShow = getItem(position)

        when(holder) {
            is MediaEntryBaseAdapter<*>.PosterViewHolder -> {
                holder.bindings.apply {

                    itemTitle.text = "${selectedShow.trackedShow.title} (${selectedShow?.trackedShow?.releaseDate?.year})"

                    itemTimestamp.visibility = View.VISIBLE

                    itemTimestamp.text = "Tracked on: " + selectedShow.trackedShow.tracked_on.atZoneSameInstant(
                        ZoneId.systemDefault())?.format(
                        DateTimeFormatter.ofPattern("dd/MM/YYYY"))


                    tmdbImageLoader.loadImages(selectedShow.trackedShow.trakt_id, ImageItemType.SHOW, selectedShow.trackedShow.tmdb_id, selectedShow.trackedShow.title ?: "", null, selectedShow.trackedShow.language,true, itemPoster, null)

//                    if(selectedShow.episodes.isNotEmpty()) {
//                        trackedshowitemUpcomingEpisodes.visibility = View.VISIBLE
//
//                        trackedshowitemUpcomingEpisodes.text = "Airing Soon (${selectedShow.episodes?.size})"
//
//                        trackedshowitemUpcomingEpisodes.setOnClickListener {
//                            upcomingEpisodesCallback(selectedShow.trackedShow.title, selectedShow.episodes)
//                        }
//                    } else {
//                        trackedshowitemUpcomingEpisodes.visibility = View.GONE
//                    }

                    root.setOnClickListener {
                        callback(selectedShow)
                    }
                }
            }
            is MediaEntryBaseAdapter<*>.CardViewHolder -> {
                holder.bindings.apply {

                    itemTitle.text = "${selectedShow.trackedShow.title} (${selectedShow?.trackedShow?.releaseDate?.year})"

                    itemWatchedDate.text = "Tracked on: " + selectedShow.trackedShow.tracked_on.atZoneSameInstant(
                        ZoneId.systemDefault())?.format(
                        DateTimeFormatter.ofPattern("dd/MM/YYYY"))

                    itemOverview.text = selectedShow.trackedShow.overview

                    tmdbImageLoader.loadImages(selectedShow.trackedShow.trakt_id, ImageItemType.SHOW, selectedShow.trackedShow.tmdb_id, selectedShow.trackedShow.title ?: "", null, selectedShow.trackedShow.language,true, itemPoster, itemBackdropImageview)

//                    if(selectedShow.episodes.isNotEmpty()) {
//                        trackedshowitemUpcomingEpisodes.visibility = View.VISIBLE
//
//                        trackedshowitemUpcomingEpisodes.text = "Airing Soon (${selectedShow.episodes?.size})"
//
//                        trackedshowitemUpcomingEpisodes.setOnClickListener {
//                            upcomingEpisodesCallback(selectedShow.trackedShow.title, selectedShow.episodes)
//                        }
//                    } else {
//                        trackedshowitemUpcomingEpisodes.visibility = View.GONE
//                    }

                    root.setOnClickListener {
                        callback(selectedShow)
                    }
                }
            }
            else -> {
                Log.e(TAG, "onBindViewHolder: Invalid ViewHolder ${holder.javaClass.name}", )
            }
        }



    }

    companion object {
        val COMPARATOR = object: DiffUtil.ItemCallback<TrackedShowWithEpisodes>() {
            override fun areItemsTheSame(
                oldItem: TrackedShowWithEpisodes,
                newItem: TrackedShowWithEpisodes
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: TrackedShowWithEpisodes,
                newItem: TrackedShowWithEpisodes
            ): Boolean {
                return oldItem.trackedShow.trakt_id == newItem.trackedShow.trakt_id
            }
        }
    }
}