package com.nickrankin.traktapp.adapter.shows

import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.blogc.android.views.ExpandableTextView
import com.bumptech.glide.RequestManager
import com.google.android.material.button.MaterialButton
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.AdaptorActionControls
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
class TrackedShowsAdapter(val controls: AdaptorActionControls<TrackedShowWithEpisodes>, private val tmdbImageLoader: TmdbImageLoader): MediaEntryBaseAdapter<TrackedShowWithEpisodes>(
    controls, COMPARATOR) {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, holder.absoluteAdapterPosition)
        val selectedShow = getItem(position)

        when(holder) {
            is MediaEntryBaseAdapter<*>.PosterViewHolder -> {
                holder.bindings.apply {

                    itemTitle.text = "${selectedShow.trackedShow.title} (${selectedShow?.trackedShow?.releaseDate?.year})"

                    itemTimestamp.visibility = View.VISIBLE

                    itemTimestamp.text = "Tracked on: " + selectedShow.trackedShow.tracked_on.atZoneSameInstant(
                        ZoneId.systemDefault())?.format(
                        DateTimeFormatter.ofPattern("dd/MM/YYYY"))


                    tmdbImageLoader.loadImages(selectedShow.trackedShow.trakt_id, ImageItemType.SHOW, selectedShow.trackedShow.tmdb_id, selectedShow.trackedShow.title ?: "", selectedShow.trackedShow.language,true, itemPoster, null, false)

                }
            }
            is MediaEntryBaseAdapter<*>.CardViewHolder -> {
                holder.bindings.apply {

                    itemTitle.text = "${selectedShow.trackedShow.title} (${selectedShow?.trackedShow?.releaseDate?.year})"

                    itemWatchedDate.text = "Tracked on: " + selectedShow.trackedShow.tracked_on.atZoneSameInstant(
                        ZoneId.systemDefault())?.format(
                        DateTimeFormatter.ofPattern("dd/MM/YYYY"))

                    itemOverview.text = selectedShow.trackedShow.overview

                    tmdbImageLoader.loadImages(selectedShow.trackedShow.trakt_id, ImageItemType.SHOW, selectedShow.trackedShow.tmdb_id, selectedShow.trackedShow.title ?: "", selectedShow.trackedShow.language,true, itemPoster, itemBackdropImageview, false)

                    updateUpcomingUpisodesCount(selectedShow, buttonControl)


                }
            }
            else -> {
                Log.e(TAG, "onBindViewHolder: Invalid ViewHolder ${holder.javaClass.name}")
            }
        }
    }

    private fun updateUpcomingUpisodesCount(trackedShowWithEpisodes: TrackedShowWithEpisodes, upcomingEpisodesButton: MaterialButton) {
        Log.d(TAG, "updateUpcomingUpisodesCount: Show ${trackedShowWithEpisodes.trackedShow.title} has ${trackedShowWithEpisodes.episodes.size} upcoming episodes")
        val totalUpcoming = trackedShowWithEpisodes.episodes.size

        if(totalUpcoming > 0) {
            upcomingEpisodesButton.visibility = View.VISIBLE
            upcomingEpisodesButton.text = "${upcomingEpisodesButton.text} ($totalUpcoming)"

        } else {
            upcomingEpisodesButton.visibility = View.GONE
        }
    }

    override fun reloadImages(
        selectedItem: TrackedShowWithEpisodes,
        posterImageView: ImageView,
        backdropImageView: ImageView?
    ) {
        tmdbImageLoader.loadImages(selectedItem.trackedShow.trakt_id, ImageItemType.SHOW, selectedItem.trackedShow.tmdb_id, selectedItem.trackedShow.title ?: "", selectedItem.trackedShow.language,true, posterImageView, backdropImageView, true)
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