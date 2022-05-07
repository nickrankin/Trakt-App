package com.nickrankin.traktapp.adapter.shows

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import at.blogc.android.views.ExpandableTextView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.databinding.WatchedEpisodeEntryListItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

private const val TAG = "WatchedEpisodesPagingAd"
class WatchedEpisodesPagingAdapter(private val sharedPreferences: SharedPreferences, private val tmdbImageLoader: TmdbImageLoader, private val callback: (selectedShow: WatchedEpisode?, action: Int) -> Unit): PagingDataAdapter<WatchedEpisode, WatchedEpisodesPagingAdapter.WatchedEpisodeViewHolder>(COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchedEpisodeViewHolder {
        return WatchedEpisodeViewHolder(WatchedEpisodeEntryListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: WatchedEpisodeViewHolder, position: Int) {
        val currentItem = getItem(position)

        holder.bindings.apply {
            // clear poster value to prevent flickering overlapping
            watchedentryitemPoster.setImageDrawable(null)
            watchedentryitemBackdrop.setImageDrawable(null)

            watchedentryitemTitle.text = currentItem?.episode_title
            watchedentryitemShowTitle.text = currentItem?.show_title
            watchedentryitemSeasonEpisodeNumber.text = "Season ${currentItem?.episode_season}, Episode ${currentItem?.episode_number}"
            watchedentryitemWatchedDate.text = "Watched: " + currentItem?.watched_at?.atZoneSameInstant(
                ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern(sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_TIME_FORMAT)))

            tmdbImageLoader.loadEpisodeImages(currentItem?.episode_trakt_id ?: 0, currentItem?.show_tmdb_id ?: 0, currentItem?.show_trakt_id ?: 0, currentItem?.episode_season, currentItem?.episode_number, currentItem?.show_title ?: "", currentItem?.language, true, watchedentryitemPoster, watchedentryitemBackdrop)

            watchedentryitemOverview.setOnClickListener { v ->
                val expandableTextView = v as ExpandableTextView

                expandableTextView.toggle()
            }

            root.setOnClickListener { callback(currentItem, ACTION_NAVIGATE_EPISODE) }
        }

    }

    inner class WatchedEpisodeViewHolder(val bindings: WatchedEpisodeEntryListItemBinding): RecyclerView.ViewHolder(bindings.root)

    companion object {
        const val ACTION_NAVIGATE_SHOW = 0
        const val ACTION_REMOVE_HISTORY = 1
        const val ACTION_NAVIGATE_EPISODE = 2


        val COMPARATOR = object: DiffUtil.ItemCallback<WatchedEpisode>() {
            override fun areItemsTheSame(
                oldItem: WatchedEpisode,
                newItem: WatchedEpisode
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: WatchedEpisode,
                newItem: WatchedEpisode
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}