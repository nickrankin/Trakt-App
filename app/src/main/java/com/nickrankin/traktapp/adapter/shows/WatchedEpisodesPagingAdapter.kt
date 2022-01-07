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
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.databinding.WatchedEpisodeEntryListItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.PosterImageLoader
import org.threeten.bp.format.DateTimeFormatter

private const val TAG = "WatchedEpisodesPagingAd"
class WatchedEpisodesPagingAdapter(private val sharedPreferences: SharedPreferences, private val imageLoader: PosterImageLoader, private val glide: RequestManager, private val callback: (selectedShow: WatchedEpisode?, action: Int) -> Unit): PagingDataAdapter<WatchedEpisode, WatchedEpisodesPagingAdapter.WatchedEpisodeViewHolder>(COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchedEpisodeViewHolder {
        return WatchedEpisodeViewHolder(WatchedEpisodeEntryListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: WatchedEpisodeViewHolder, position: Int) {
        val currentItem = getItem(position)

        holder.bindings.apply {
            // clear poster value to prevent flickering overlapping
            watchedentryitemPoster.setImageDrawable(null)

            watchedentryitemTitle.text = currentItem?.episode_title
            watchedentryitemShowTitle.text = currentItem?.show_title
            watchedentryitemSeasonEpisodeNumber.text = "Season ${currentItem?.episode_season} Episode ${currentItem?.episode_number}"
            watchedentryitemWatchedDate.text = "Watched: " + currentItem?.watched_at?.format(DateTimeFormatter.ofPattern(sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_TIME_FORMAT)))

            imageLoader.loadImage(currentItem?.show_tmdb_id ?: 0, currentItem?.language, true, callback = {posterPath ->
                if(posterPath.isNotEmpty()) {
                    glide
                        .load(AppConstants.TMDB_POSTER_URL + posterPath)
                        .into(watchedentryitemPoster)
                }
            })

            watchedentryitemMenu.setOnClickListener {
                showPopupMenu(holder.itemView, currentItem)

            }

            root.setOnClickListener { callback(currentItem, ACTION_NAVIGATE_EPISODE) }
        }

    }

    private fun showPopupMenu(view: View, selectedItem: WatchedEpisode?) {
        val context = view.context
        val popup = PopupMenu(context, view)

        popup.menuInflater.inflate(R.menu.watched_shows_popup_menu, popup.menu)

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.watchedshowspopup_nav_show -> {
                    callback(selectedItem, ACTION_NAVIGATE_SHOW)
                }
                R.id.watchedshowspopup_nav_episode -> {
                    callback(selectedItem, ACTION_NAVIGATE_EPISODE)
                }
                R.id.watchedshowspopup_remove_show -> {
                    callback(selectedItem, ACTION_REMOVE_HISTORY)
                }
            }
            Log.e(TAG, "showPopupMenu: Clicked ${item.title} for ${selectedItem?.show_title}")
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