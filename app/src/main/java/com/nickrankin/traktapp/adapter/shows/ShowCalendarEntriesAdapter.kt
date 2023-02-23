package com.nickrankin.traktapp.adapter.shows

import android.content.SharedPreferences
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.dao.calendars.model.ShowCalendarEntry
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.getFormattedDateTime
import javax.inject.Inject

private const val TAG = "ShowCalendarEntriesAdap"
class ShowCalendarEntriesAdapter @Inject constructor(private val sharedPreferences: SharedPreferences, private val tmdbImageLoader: TmdbImageLoader,private val callback: (selectedShow: ShowCalendarEntry, action: Int) -> Unit): MediaEntryBaseAdapter<ShowCalendarEntry>(
    null, COMPARATOR) {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        //holder.setIsRecyclable(false)

        val currentItem = getItem(position)

        when(holder) {
            is MediaEntryBaseAdapter<*>.CardViewHolder -> {
                holder.bindings.apply {
                    itemTitle.text = "${currentItem.episode_title} (${currentItem.show_title})"
                    itemSubTitle.visibility = View.VISIBLE
                    itemSubTitle.text = "Season ${currentItem.episode_season} Episode ${currentItem.episode_number}"

                    if(currentItem.first_aired != null) {
                        itemWatchedDate.visibility = View.VISIBLE
                        itemWatchedDate.text = "Airing: ${getFormattedDateTime(currentItem.first_aired, sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_FORMAT), sharedPreferences.getString("time_format", AppConstants.DEFAULT_TIME_FORMAT))}"
                    }

                    itemOverview.text = currentItem.episode_overview

                    tmdbImageLoader.loadEpisodeImages(currentItem.episode_trakt_id, currentItem.show_tmdb_id, currentItem.show_trakt_id, currentItem.episode_season,currentItem.episode_number, currentItem?.show_title ?: "",currentItem.language,true, itemPoster, itemBackdropImageview, false)

                    root.setOnClickListener { callback(currentItem, ACTION_NAVIGATE_EPISODE) }

//                    howentryitemNavShowBtn.setOnClickListener {  callback(currentItem, ACTION_NAVIGATE_SHOW)  }
//                    howentryitemNavEpisodeBtn.setOnClickListener { callback(currentItem, ACTION_NAVIGATE_EPISODE) }
//
//                    if(currentItem.hidden) {
//                        howentryitemHideShowBtn.text = "Unhide Show"
//                    }

//                    howentryitemHideShowBtn.setOnClickListener {  callback(currentItem, ACTION_REMOVE_COLLECTION)  }
                }
            }
            is MediaEntryBaseAdapter<*>.PosterViewHolder -> {
                holder.bindings.apply {
                    itemTitle.text = "S${currentItem.episode_season}E${currentItem.episode_number} (${currentItem.show_title})"

                    if(currentItem.first_aired != null) {
                        itemTimestamp.visibility = View.VISIBLE
                        itemTimestamp.text = "Airing: ${getFormattedDateTime(currentItem.first_aired, sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_FORMAT), sharedPreferences.getString("time_format", AppConstants.DEFAULT_TIME_FORMAT))}"
                    }

                    tmdbImageLoader.loadEpisodeImages(currentItem.episode_trakt_id, currentItem.show_tmdb_id, currentItem.show_trakt_id, currentItem.episode_season,currentItem.episode_number, currentItem?.show_title ?: "",currentItem.language,true, itemPoster, null, false)

                    root.setOnClickListener { callback(currentItem, ACTION_NAVIGATE_EPISODE) }
                }
            }
        }
    }

    override fun reloadImages(
        selectedItem: ShowCalendarEntry,
        posterImageView: ImageView,
        backdropImageView: ImageView?
    ) {
        tmdbImageLoader.loadEpisodeImages(selectedItem.episode_trakt_id, selectedItem.show_tmdb_id, selectedItem.show_trakt_id, selectedItem.episode_season, selectedItem.episode_number, selectedItem?.show_title ?: "", selectedItem.language,true, posterImageView, backdropImageView, true)
    }

    companion object {
        const val ACTION_NAVIGATE_SHOW = 0
        const val ACTION_REMOVE_COLLECTION = 1
        const val ACTION_NAVIGATE_EPISODE = 2

        val COMPARATOR = object: DiffUtil.ItemCallback<ShowCalendarEntry>() {
            override fun areItemsTheSame(
                oldItem: ShowCalendarEntry,
                newItem: ShowCalendarEntry
            ): Boolean {
                return  oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: ShowCalendarEntry,
                newItem: ShowCalendarEntry
            ): Boolean {
                return oldItem.show_trakt_id == newItem.show_trakt_id
            }
        }
    }
}