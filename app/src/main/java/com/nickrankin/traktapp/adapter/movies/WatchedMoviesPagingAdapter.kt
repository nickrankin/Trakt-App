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
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.databinding.WatchedMovieEntryListItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.PosterImageLoader
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.time.ZoneOffset

private const val TAG = "WatchedEpisodesPagingAd"
class WatchedMoviesPagingAdapter(private val sharedPreferences: SharedPreferences, private val imageLoader: PosterImageLoader, private val glide: RequestManager, private val callback: (selectedMovie: WatchedMovie?, action: Int) -> Unit): PagingDataAdapter<WatchedMovie, WatchedMoviesPagingAdapter.WatchedMovieViewHolder>(COMPARATOR) {

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

            imageLoader.loadMoviePosterImage(currentItem?.trakt_id ?: 0, currentItem?.tmdb_id ?: 0, currentItem?.language, true, callback = { posterImage ->
                if(posterImage.poster_path != null && posterImage.trakt_id == currentItem?.trakt_id) {
                    glide
                        .load(AppConstants.TMDB_POSTER_URL + posterImage.poster_path)
                        .into(watchedentryitemPoster)
                }
            })

            watchedentryitemOverview.setOnClickListener {
                (it as ExpandableTextView).apply {
                    toggle()
                }
            }

            watchedentryitemMenu.setOnClickListener {
                showPopupMenu(holder.itemView, currentItem)

            }

            root.setOnClickListener { callback(currentItem, ACTION_NAVIGATE_MOVIE) }
        }

    }

    private fun showPopupMenu(view: View, selectedItem: WatchedMovie?) {
        val context = view.context
        val popup = PopupMenu(context, view)

        popup.menuInflater.inflate(R.menu.watched_movies_popup_menu, popup.menu)

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.watchedmoviepopupmenu_nav_movie -> {
                    callback(selectedItem, ACTION_NAVIGATE_MOVIE)
                }
                R.id.watchedmoviepopupmenu_remove_show -> {
                    callback(selectedItem, ACTION_REMOVE_HISTORY)
                }
            }
            Log.e(TAG, "showPopupMenu: Clicked ${item.title} for ${selectedItem?.title}")
            true
        }

        // Workaround to add menu icons to dropdown list
        // https://www.material.io/components/menus/android#dropdown-menus
        @SuppressLint("RestrictedApi")
        if (popup.menu is MenuBuilder) {
            val menuBuilder = popup.menu as MenuBuilder
            menuBuilder.setOptionalIconsVisible(true)
            for (item in menuBuilder.visibleItems) {
                val iconMarginPx =
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        ICON_MARGIN.toFloat(),
                        context.resources.displayMetrics
                    ).toInt()
                if (item.icon != null) {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                        item.icon = InsetDrawable(item.icon, iconMarginPx, 0, iconMarginPx, 0)
                    } else {
                        item.icon =
                            object : InsetDrawable(item.icon, iconMarginPx, 0, iconMarginPx, 0) {
                                override fun getIntrinsicWidth(): Int {
                                    return intrinsicHeight + iconMarginPx + iconMarginPx
                                }
                            }
                    }
                }
            }
        }

        popup.show()
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