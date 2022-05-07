package com.nickrankin.traktapp.adapter.movies

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
import com.nickrankin.traktapp.adapter.shows.ICON_MARGIN
import com.nickrankin.traktapp.dao.movies.model.WatchedMovie
import com.nickrankin.traktapp.databinding.WatchedMovieEntryListItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

private const val TAG = "WatchedEpisodesPagingAd"
class WatchedMoviesPagingAdapter(private val sharedPreferences: SharedPreferences, private val tmdbImageLoader: TmdbImageLoader, private val callback: (selectedMovie: WatchedMovie?, action: Int) -> Unit): PagingDataAdapter<WatchedMovie, WatchedMoviesPagingAdapter.WatchedMovieViewHolder>(COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchedMovieViewHolder {
        return WatchedMovieViewHolder(WatchedMovieEntryListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: WatchedMovieViewHolder, position: Int) {
        val currentItem = getItem(position)

        holder.bindings.apply {
            // clear poster value to prevent flickering overlapping
            watchedentryitemPoster.setImageDrawable(null)

            // The ExpandingTextView should be collapsed always unless explicitly toggled. This is needed due to the recycling of the Views
            watchedentryitemOverview.collapse()

            watchedentryitemTitle.text = currentItem?.title
            watchedentryitemOverview.text = currentItem?.overview
            watchedentryitemWatchedDate.text = "Watched: " + currentItem?.watched_at?.atZoneSameInstant(
                ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern(sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_TIME_FORMAT)))

            tmdbImageLoader.loadImages(currentItem?.trakt_id ?: 0, ImageItemType.MOVIE,currentItem?.tmdb_id ?: 0,  currentItem?.title, null, currentItem?.language,true, watchedentryitemPoster, watchedentryitemBackdropImageview)

            watchedentryitemOverview.setOnClickListener {
                (it as ExpandableTextView).apply {
                    toggle()
                }
            }

            root.setOnClickListener { callback(currentItem, ACTION_NAVIGATE_MOVIE) }

            watchedentryitemRemovePlayBtn.setOnClickListener { callback(currentItem, ACTION_REMOVE_HISTORY) }
        }

    }

    inner class WatchedMovieViewHolder(val bindings: WatchedMovieEntryListItemBinding): RecyclerView.ViewHolder(bindings.root)

    companion object {
        const val ACTION_NAVIGATE_MOVIE = 0
        const val ACTION_REMOVE_HISTORY = 1


        val COMPARATOR = object: DiffUtil.ItemCallback<WatchedMovie>() {
            override fun areItemsTheSame(
                oldItem: WatchedMovie,
                newItem: WatchedMovie
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: WatchedMovie,
                newItem: WatchedMovie
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}