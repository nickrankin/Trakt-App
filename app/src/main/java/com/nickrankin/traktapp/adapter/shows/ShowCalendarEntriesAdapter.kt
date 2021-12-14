package com.nickrankin.traktapp.adapter.shows

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.dao.calendars.model.ShowCalendarEntry
import com.nickrankin.traktapp.databinding.ShowCalendarEntryListItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.PosterImageLoader
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

class ShowCalendarEntriesAdapter @Inject constructor(private val sharedPreferences: SharedPreferences, private val posterImageLoader: PosterImageLoader, private val glide: RequestManager, private val callback: (selectedShow: ShowCalendarEntry) -> Unit): ListAdapter<ShowCalendarEntry, ShowCalendarEntriesAdapter.CalendarEntryViewHolder>(
    COMPARATOR) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarEntryViewHolder {
        return CalendarEntryViewHolder(ShowCalendarEntryListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: CalendarEntryViewHolder, position: Int) {
        holder.setIsRecyclable(false)

        val currentItem = getItem(position)

        holder.bindings.apply {
            showentryitemTitle.text = currentItem.episode_title
            showentryitemShowTitle.text = currentItem.show_title
            showentryitemSeasonEpisodeNumber.text = "Season ${currentItem.episode_season} Episode ${currentItem.episode_number}"
            showentryitemAirDate.text = "Airing: " + currentItem.first_aired?.format(DateTimeFormatter.ofPattern(sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_TIME_FORMAT)))
            showentryitemOverview.text = currentItem.episode_overview

            posterImageLoader.loadImage(currentItem.show_tmdb_id, currentItem.language, callback = { posterPath ->
                if(posterPath.isNotEmpty()) {
                    glide
                        .load(AppConstants.TMDB_POSTER_URL + posterPath)
                        .into(showentryitemPoster)
                }
            })

            root.setOnClickListener { callback(currentItem) }
        }
    }

    class CalendarEntryViewHolder(val bindings: ShowCalendarEntryListItemBinding): RecyclerView.ViewHolder(bindings.root)

    companion object {
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