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
import com.nickrankin.traktapp.dao.history.model.EpisodeWatchedHistoryEntry
import com.nickrankin.traktapp.dao.history.model.MovieWatchedHistoryEntry
import com.nickrankin.traktapp.dao.movies.model.WatchedMovie
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.dao.stats.model.WatchedEpisodeStats
import com.nickrankin.traktapp.dao.stats.model.WatchedMoviesStats
import com.nickrankin.traktapp.databinding.ViewPosterItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.uwetrottmann.trakt5.entities.HistoryEntry
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

private const val TAG = "LastWatchedHistoryAdapter"
class LastWatchedHistoryAdapter<T: com.nickrankin.traktapp.dao.history.model.HistoryEntry>(
    private val comparator: DiffUtil.ItemCallback<T>,
    private val sharedPreferences: SharedPreferences,
    private val tmdbImageLoader: TmdbImageLoader,
    private val callback: (item: T) -> Unit
) : ListAdapter<T, LastWatchedHistoryAdapter<T>.WatchedHistoryVH>(
    comparator
) {



    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LastWatchedHistoryAdapter<T>.WatchedHistoryVH {
        return WatchedHistoryVH(
            ViewPosterItemBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: WatchedHistoryVH, position: Int) {
        val currentItem = getItem(position)

        holder.bindings.apply {

            itemTimestamp.text = "Watched: " + currentItem.watched_date?.atZoneSameInstant(
                ZoneId.systemDefault())?.format(
                DateTimeFormatter.ofPattern(AppConstants.DEFAULT_DATE_TIME_FORMAT))

            itemTitle.text = currentItem.title

            when(currentItem) {
                is MovieWatchedHistoryEntry -> {
                    tmdbImageLoader.loadImages(
                        currentItem.trakt_id,
                        ImageItemType.MOVIE,
                        currentItem.tmdb_id,
                        currentItem.title,
                        null,
                        true,
                        itemPoster,
                        null,
                        false
                    )
                }
                is EpisodeWatchedHistoryEntry -> {
                    tmdbImageLoader.loadImages(
                        currentItem.trakt_id,
                        ImageItemType.SHOW,
                        currentItem.show_tmdb_id,
                        currentItem.title,
                        null,
                        true,
                        itemPoster,
                        null,
                        false
                    )
                }
            }

            root.setOnClickListener {
                callback(currentItem)
            }

        }
    }

    inner class WatchedHistoryVH(val bindings: ViewPosterItemBinding) :
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
}