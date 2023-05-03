package com.nickrankin.traktapp.adapter.shows

import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nickrankin.traktapp.dao.calendars.model.BaseCalendarEntry
import com.nickrankin.traktapp.dao.calendars.model.ShowBaseCalendarEntry
import com.nickrankin.traktapp.databinding.ViewPosterItemBinding
import com.nickrankin.traktapp.databinding.ViewTitleItemBinding
import com.nickrankin.traktapp.databinding.ViewUpcomingItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.getFormattedDateTime
import javax.inject.Inject

private const val TAG = "ShowCalendarEntriesAdap"
class ShowCalendarEntriesAdapter @Inject constructor(private val sharedPreferences: SharedPreferences, private val tmdbImageLoader: TmdbImageLoader,private val callback: (selectedShow: BaseCalendarEntry, action: Int) -> Unit): ListAdapter<BaseCalendarEntry, RecyclerView.ViewHolder>(
    COMPARATOR) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            0 -> {
                ItemViewHolder(ViewUpcomingItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            1 -> {
                TitleViewHolder(ViewTitleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            else -> {
                ItemViewHolder(ViewUpcomingItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        //holder.setIsRecyclable(false)

        val currentItem = getItem(position)

//        when(holder) {
//            is MediaEntryBaseAdapter<*>.CardViewHolder -> {
//                when(currentItem) {
//                    is ShowBaseCalendarEntry -> {
//                        holder.bindings.apply {
//                            itemTitle.text = "${currentItem.episode_title} (${currentItem.show_title})"
//                            itemSubTitle.visibility = View.VISIBLE
//                            itemSubTitle.text = "Season ${currentItem.episode_season} Episode ${currentItem.episode_number}"
//
//                            if(currentItem.first_aired != null) {
//                                itemWatchedDate.visibility = View.VISIBLE
//                                itemWatchedDate.text = "Airing: ${getFormattedDateTime(currentItem.first_aired!!, sharedPreferences.getString(AppConstants.DATE_FORMAT, AppConstants.DEFAULT_DATE_FORMAT), sharedPreferences.getString(AppConstants.TIME_FORMAT, AppConstants.DEFAULT_TIME_FORMAT))}"
//                            }
//
//                            itemOverview.text = currentItem.episode_overview
//
//                            tmdbImageLoader.loadEpisodeImages(currentItem.episode_trakt_id, currentItem.show_tmdb_id, currentItem.show_trakt_id, currentItem.episode_season,currentItem.episode_number, currentItem?.show_title ?: "",currentItem.language,true, itemPoster, itemBackdropImageview, false)
//
//                            root.setOnClickListener { callback(currentItem, ACTION_NAVIGATE_EPISODE) }
//
////                    howentryitemNavShowBtn.setOnClickListener {  callback(currentItem, ACTION_NAVIGATE_SHOW)  }
////                    howentryitemNavEpisodeBtn.setOnClickListener { callback(currentItem, ACTION_NAVIGATE_EPISODE) }
////
////                    if(currentItem.hidden) {
////                        howentryitemHideShowBtn.text = "Unhide Show"
////                    }
//
////                    howentryitemHideShowBtn.setOnClickListener {  callback(currentItem, ACTION_REMOVE_COLLECTION)  }
//                        }
//                    }
////                    is BaseCalendarEntry -> {
////                        holder.bindings.apply {
////                            itemWatchedDate.visibility = View.VISIBLE
////
////
////                            itemWatchedDate.text = "fhfghfgh"+currentItem.first_aired.toString()
////                        }
////                    }
//
//                }
//
//            }
//            is MediaEntryBaseAdapter<*>.PosterViewHolder -> {
        when(holder) {
            is ItemViewHolder -> {
                when(currentItem) {
                    is ShowBaseCalendarEntry -> {
                        holder.bindings.apply {
                            upcomingitemviewSeasonEpisodeTitle.text = "Season: ${currentItem.episode_season} Episode: ${currentItem.episode_number} (${currentItem.episode_title})"
                            upcomingitemviewShowTitle.text = currentItem.show_title

                            if(currentItem.first_aired != null) {
                                upcomingitemviewAiring.text = "Airing: ${getFormattedDateTime(currentItem.first_aired!!, sharedPreferences.getString(AppConstants.DATE_FORMAT, AppConstants.DEFAULT_DATE_FORMAT), sharedPreferences.getString(AppConstants.TIME_FORMAT, AppConstants.DEFAULT_TIME_FORMAT))}"
                            }

                            tmdbImageLoader.loadEpisodeImages(currentItem.episode_trakt_id, currentItem.show_tmdb_id, currentItem.show_trakt_id, currentItem.episode_season,currentItem.episode_number, currentItem.show_title, currentItem.language,true, upcomingitemviewPoster, null, false)

                            root.setOnClickListener { callback(currentItem, ACTION_NAVIGATE_EPISODE) }
                        }
                    }

                    else -> {
                        Log.e(TAG, "onBindViewHolder: Invalid type")
                    }
                }
            }
            is TitleViewHolder -> {
                when(currentItem) {
                    is BaseCalendarEntry -> {
                        holder.bindings.apply {
                            viewtitleitemHeading.visibility = View.VISIBLE

                            if(currentItem.first_aired != null) {

                                viewtitleitemHeading.text = "Shows airing " + getFormattedDateTime(
                                    currentItem.first_aired!!,
                                    sharedPreferences.getString(
                                        AppConstants.DATE_FORMAT,
                                        AppConstants.DEFAULT_DATE_FORMAT
                                    ),
                                    null
                                )
                            }
                        }
                    }

                    else -> {
                        Log.e(TAG, "onBindViewHolder: Invalid type")
                    }
                }
            }
        }


//            }
//        }
    }

    override fun getItemViewType(position: Int): Int {
        return when(getItem(position)) {
            is ShowBaseCalendarEntry -> {
                0
            }
            is BaseCalendarEntry -> {
                1
            }
            else -> {
                0
            }
        }
    }

    inner class TitleViewHolder(val bindings: ViewTitleItemBinding): RecyclerView.ViewHolder(bindings.root)
    inner class ItemViewHolder(val bindings: ViewUpcomingItemBinding): RecyclerView.ViewHolder(bindings.root)

//    override fun reloadImages(
//        selectedItem: BaseCalendarEntry,
//        posterImageView: ImageView,
//        backdropImageView: ImageView?
//    ) {
//        if(selectedItem is ShowBaseCalendarEntry) {
//            tmdbImageLoader.loadEpisodeImages(selectedItem.episode_trakt_id, selectedItem.show_tmdb_id, selectedItem.show_trakt_id, selectedItem.episode_season, selectedItem.episode_number, selectedItem?.show_title ?: "", selectedItem.language,true, posterImageView, backdropImageView, true)
//
//        }
//    }

    companion object {
        const val ACTION_NAVIGATE_SHOW = 0
        const val ACTION_REMOVE_COLLECTION = 1
        const val ACTION_NAVIGATE_EPISODE = 2

        val COMPARATOR = object: DiffUtil.ItemCallback<BaseCalendarEntry>() {
            override fun areItemsTheSame(
                oldItem: BaseCalendarEntry,
                newItem: BaseCalendarEntry
            ): Boolean {
                return  oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: BaseCalendarEntry,
                newItem: BaseCalendarEntry
            ): Boolean {
                return  oldItem.episode_trakt_id == newItem.episode_trakt_id
            }
        }
    }
}