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
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.dao.calendars.model.ShowCalendarEntry
import com.nickrankin.traktapp.databinding.ShowCalendarEntryListItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.PosterImageLoader
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "ShowCalendarEntriesAdap"
class ShowCalendarEntriesAdapter @Inject constructor(private val sharedPreferences: SharedPreferences, private val posterImageLoader: PosterImageLoader, private val glide: RequestManager, private val callback: (selectedShow: ShowCalendarEntry, action: Int) -> Unit): ListAdapter<ShowCalendarEntry, ShowCalendarEntriesAdapter.CalendarEntryViewHolder>(
    COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarEntryViewHolder {
        return CalendarEntryViewHolder(ShowCalendarEntryListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: CalendarEntryViewHolder, position: Int) {
        //holder.setIsRecyclable(false)

        val currentItem = getItem(position)

        holder.bindings.apply {
            showentryitemTitle.text = currentItem.episode_title
            showentryitemShowTitle.text = currentItem.show_title
            showentryitemSeasonEpisodeNumber.text = "Season ${currentItem.episode_season} Episode ${currentItem.episode_number}"
            showentryitemAirDate.text = "Airing: " + currentItem.first_aired?.format(DateTimeFormatter.ofPattern(sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_TIME_FORMAT)))
            showentryitemOverview.text = currentItem.episode_overview

            posterImageLoader.loadShowPosterImage(currentItem.show_trakt_id, currentItem.show_tmdb_id, currentItem?.language, currentItem?.show_title ?: "", currentItem.first_aired?.year, true, callback = { posterImage ->
                if(posterImage.poster_path != null) {
                    glide
                        .load(AppConstants.TMDB_POSTER_URL + posterImage.poster_path)
                        .into(showentryitemPoster)
                }
            })

            root.setOnClickListener { callback(currentItem, ACTION_NAVIGATE_EPISODE) }

            showentryitemMenu.setOnClickListener {
                showPopupMenu(holder.itemView, currentItem)
            }
        }
    }

    private fun showPopupMenu(view: View, selectedItem: ShowCalendarEntry) {
        val context = view.context
        val popup = PopupMenu(context, view)

        popup.menuInflater.inflate(R.menu.upcoming_shows_popup_menu, popup.menu)

        val hideMenuItem = popup.menu.findItem(R.id.upcomingshowspopup_remove_show)

        if(selectedItem.hidden) {
            hideMenuItem.title = "Unhide Show"
        }

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.upcomingshowspopup_nav_show -> {
                    callback(selectedItem, ACTION_NAVIGATE_SHOW)
                }
                R.id.upcomingshowspopup_nav_episode -> {
                    callback(selectedItem, ACTION_NAVIGATE_EPISODE)
                }
                R.id.upcomingshowspopup_remove_show -> {
                    callback(selectedItem, ACTION_REMOVE_COLLECTION)
                }
            }
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

    class CalendarEntryViewHolder(val bindings: ShowCalendarEntryListItemBinding): RecyclerView.ViewHolder(bindings.root)


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