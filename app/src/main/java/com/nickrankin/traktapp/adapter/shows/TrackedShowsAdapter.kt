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
import com.nickrankin.traktapp.databinding.LayoutTrackedShowBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.getFormattedDate
import com.nickrankin.traktapp.helper.getFormattedDateTime
import org.apache.commons.lang3.StringUtils
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

private const val TAG = "TrackedShowsAdapter"
class TrackedShowsAdapter(val callback: (option: Int, trackedShowsWithEpisode: TrackedShowWithEpisodes) -> Unit, private val tmdbImageLoader: TmdbImageLoader, private val sharedPreferences: SharedPreferences): ListAdapter<TrackedShowWithEpisodes, TrackedShowsAdapter.TrackedItemViewHolder>(
    COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackedItemViewHolder {
        return TrackedItemViewHolder(
            LayoutTrackedShowBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: TrackedItemViewHolder, position: Int) {
        val selectedShow = getItem(position)

        holder.bindings.apply {
            root.setOnClickListener {
                callback(OPT_VIEW, selectedShow)
            }

            trackedshowTitle.text = selectedShow.trackedShow.title
            trackedshowTrackedAt.text = "Tracked at: ${getFormattedDateTime(selectedShow.trackedShow.tracked_on, sharedPreferences.getString(AppConstants.DATE_FORMAT, AppConstants.DEFAULT_DATE_FORMAT), sharedPreferences.getString(AppConstants.TIME_FORMAT, AppConstants.DEFAULT_TIME_FORMAT))}"
            trackedshowOverview.text = selectedShow.trackedShow.overview

            trackedshowStatus.text = "Status: ${StringUtils.capitalize(selectedShow.trackedShow.status?.name?.lowercase())}"

            if(selectedShow.episodes.isNullOrEmpty()) {
                trackedshowChipUpcoming.visibility = View.GONE
            } else {
                trackedshowChipUpcoming.visibility = View.VISIBLE
                trackedshowChipUpcoming.text = "Airing Soon (${selectedShow.episodes.size})"

                trackedshowChipUpcoming.setOnClickListener {
                    callback(OPT_LIST_EPISODES, selectedShow)
                }

            }

            trackedshowChipStopTracking.setOnClickListener {
                callback(OPT_STOP_TRACKING, selectedShow)
            }

            tmdbImageLoader.loadImages(selectedShow.trackedShow.trakt_id, ImageItemType.SHOW, selectedShow.trackedShow.tmdb_id, selectedShow.trackedShow.title, selectedShow.trackedShow.language, true, trackedshowPoster, null, false)
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

    fun reloadImages(
        selectedItem: TrackedShowWithEpisodes,
        posterImageView: ImageView,
        backdropImageView: ImageView?
    ) {
        tmdbImageLoader.loadImages(selectedItem.trackedShow.trakt_id, ImageItemType.SHOW, selectedItem.trackedShow.tmdb_id, selectedItem.trackedShow.title ?: "", selectedItem.trackedShow.language,true, posterImageView, backdropImageView, true)
    }

    inner class TrackedItemViewHolder(val bindings: LayoutTrackedShowBinding): RecyclerView.ViewHolder(bindings.root)

    companion object {
        const val OPT_VIEW = 1
        const val OPT_LIST_EPISODES = 2
        const val OPT_STOP_TRACKING = 3
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