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
import com.nickrankin.traktapp.dao.calendars.model.ShowBaseCalendarEntry
import com.nickrankin.traktapp.databinding.ViewPosterItemBinding
import com.nickrankin.traktapp.databinding.ViewUpcomingItemBinding
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.convertToHumanReadableTime

private const val TAG = "CollectedShowsAdapter"
class UpcomingEpisodesAdapter(
    private val sharedPreferences: SharedPreferences,
    private val tmdbImageLoader: TmdbImageLoader,
    private val callback: (ShowCalendarEntry: ShowBaseCalendarEntry, action: Int, position: Int) -> Unit
) : ListAdapter<ShowBaseCalendarEntry, UpcomingEpisodesAdapter.WatchedHistoryVH>(
    COMPARATOR
) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UpcomingEpisodesAdapter.WatchedHistoryVH {
        return WatchedHistoryVH(
            ViewUpcomingItemBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: WatchedHistoryVH, position: Int) {
        val currentItem = getItem(position)

        holder.bindings.apply {

            upcomingitemviewAiring.visibility = View.VISIBLE
            upcomingitemviewAiring.text = "Airing: ${convertToHumanReadableTime(currentItem.first_aired)}"

                    upcomingitemviewSeasonEpisodeTitle.text = "${currentItem?.episode_title} (${currentItem.show_title})"
            upcomingitemviewShowTitle.text = currentItem?.show_title

                    tmdbImageLoader.loadImages(
                        currentItem.show_trakt_id,
                        ImageItemType.SHOW,
                        currentItem.show_tmdb_id,
                        currentItem?.show_title,
                        currentItem.language,
                        true,
                        upcomingitemviewPoster,
                        null,
                        false
                    )


            root.setOnClickListener {
                callback(currentItem, 0, position)
            }
        }
    }

    inner class WatchedHistoryVH(val bindings: ViewUpcomingItemBinding) :
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
        val COMPARATOR = object : DiffUtil.ItemCallback<ShowBaseCalendarEntry>() {
            override fun areItemsTheSame(oldItem: ShowBaseCalendarEntry, newItem: ShowBaseCalendarEntry): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: ShowBaseCalendarEntry,
                newItem: ShowBaseCalendarEntry
            ): Boolean {
                return oldItem.episode_trakt_id == newItem.episode_trakt_id
            }
        }
    }
}