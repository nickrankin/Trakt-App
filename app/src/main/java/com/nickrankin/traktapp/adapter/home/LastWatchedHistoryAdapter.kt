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
class LastWatchedHistoryAdapter<T>(
    private val comparator: DiffUtil.ItemCallback<T>,
    private val sharedPreferences: SharedPreferences,
    private val tmdbImageLoader: TmdbImageLoader,
    private val callback: (item: T, action: Int, position: Int) -> Unit
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


            when (currentItem) {
                is WatchedMoviesStats -> {
                    itemTimestamp.text = "Watched: " + currentItem.last_watched_at?.atZoneSameInstant(
                        ZoneId.systemDefault())?.format(
                        DateTimeFormatter.ofPattern(AppConstants.DEFAULT_DATE_TIME_FORMAT))

                    itemTitle.text = currentItem.title
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

                is WatchedEpisodeStats -> {
                    itemTitle.text = "Season ${currentItem.season} Episode ${currentItem.episode}"

                    itemTimestamp.visibility = View.VISIBLE

                    itemTimestamp.text = "Watched: " + currentItem.last_watched_at?.atZoneSameInstant(
                        ZoneId.systemDefault())?.format(
                        DateTimeFormatter.ofPattern(AppConstants.DEFAULT_DATE_TIME_FORMAT))

                    tmdbImageLoader.loadImages(
                        currentItem.show_trakt_id ?: 0,
                        ImageItemType.SHOW,
                        currentItem.show_tmdb_id,
                        currentItem.show_title,
                        null,
                        true,
                        itemPoster,
                        null,
                        false
                    )

                }
                else -> {}
            }
            root.setOnClickListener {
                callback(currentItem, 0, position)
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