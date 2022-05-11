package com.nickrankin.traktapp.adapter.home

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexboxLayoutManager
import com.nickrankin.traktapp.dao.calendars.model.ShowCalendarEntry
import com.nickrankin.traktapp.databinding.HistoryPosterItemBinding
import com.nickrankin.traktapp.databinding.MoviePosterItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.convertToHumanReadableTime
import com.uwetrottmann.trakt5.entities.HistoryEntry
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

private const val TAG = "CollectedShowsAdapter"
class UpcomingEpisodesAdapter(
    private val sharedPreferences: SharedPreferences,
    private val tmdbImageLoader: TmdbImageLoader,
    private val callback: (ShowCalendarEntry: ShowCalendarEntry, action: Int, position: Int) -> Unit
) : ListAdapter<ShowCalendarEntry, UpcomingEpisodesAdapter.WatchedHistoryVH>(
    COMPARATOR
) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UpcomingEpisodesAdapter.WatchedHistoryVH {
        return WatchedHistoryVH(
            HistoryPosterItemBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: WatchedHistoryVH, position: Int) {
        val currentItem = getItem(position)

        holder.bindings.apply {
            watcheditemTimestamp.text = "Airing: ${convertToHumanReadableTime(currentItem.first_aired)}"

                    movieitemTitle.text = "${currentItem?.episode_title} (${currentItem.show_title})"

                    tmdbImageLoader.loadImages(
                        currentItem.show_trakt_id,
                        ImageItemType.SHOW,
                        currentItem.show_tmdb_id,
                        currentItem?.show_title,
                        null,
                        currentItem.language,
                        true,
                        movieitemPoster,
                        null
                    )


            root.setOnClickListener {
                callback(currentItem, 0, position)
            }
        }
    }

    inner class WatchedHistoryVH(val bindings: HistoryPosterItemBinding) :
        BaseViewHolder(bindings.root)

    // Workaround to support variable height Cast Person elements in the horizontal RecyclerView.
    // https://stackoverflow.com/questions/64504633/horizontal-recyclerview-with-dynamic-item-s-height
    abstract class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            val lp: ViewGroup.LayoutParams = itemView.layoutParams
            if (lp is FlexboxLayoutManager.LayoutParams) {
                lp.flexShrink = 0.0f
                lp.alignSelf =
                    AlignItems.FLEX_START //this will align each itemView on Top or use AlignItems.FLEX_END to align it at Bottom
            }
        }
    }

    companion object {
        val COMPARATOR = object : DiffUtil.ItemCallback<ShowCalendarEntry>() {
            override fun areItemsTheSame(oldItem: ShowCalendarEntry, newItem: ShowCalendarEntry): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: ShowCalendarEntry,
                newItem: ShowCalendarEntry
            ): Boolean {
                return oldItem.episode_trakt_id == newItem.episode_trakt_id
            }
        }
    }
}