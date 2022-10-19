package com.nickrankin.traktapp.adapter.shows

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.blogc.android.views.ExpandableTextView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.dao.calendars.model.ShowCalendarEntry
import com.nickrankin.traktapp.databinding.ShowCalendarEntryListItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.getFormattedDate
import com.uwetrottmann.trakt5.entities.CalendarShowEntry
import org.threeten.bp.format.DateTimeFormatter
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
                        itemWatchedDate.text = "Airing: ${getFormattedDate(currentItem.first_aired, sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_FORMAT), sharedPreferences.getString("time_format", AppConstants.DEFAULT_TIME_FORMAT))}"
                    }

                    itemOverview.text = currentItem.episode_overview

                    tmdbImageLoader.loadEpisodeImages(currentItem.episode_trakt_id, currentItem.show_tmdb_id, currentItem.show_trakt_id, currentItem.episode_season,currentItem.episode_number, currentItem?.show_title ?: "",currentItem.language,true, itemPoster, itemBackdropImageview)

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
                        itemTimestamp.text = "Airing: ${getFormattedDate(currentItem.first_aired, sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_FORMAT), sharedPreferences.getString("time_format", AppConstants.DEFAULT_TIME_FORMAT))}"
                    }

                    tmdbImageLoader.loadEpisodeImages(currentItem.episode_trakt_id, currentItem.show_tmdb_id, currentItem.show_trakt_id, currentItem.episode_season,currentItem.episode_number, currentItem?.show_title ?: "",currentItem.language,true, itemPoster, null)

                    root.setOnClickListener { callback(currentItem, ACTION_NAVIGATE_EPISODE) }
                }
            }
        }
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