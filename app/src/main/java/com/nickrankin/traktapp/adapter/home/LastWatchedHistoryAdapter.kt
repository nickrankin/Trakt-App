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
import com.nickrankin.traktapp.databinding.HistoryPosterItemBinding
import com.nickrankin.traktapp.databinding.MoviePosterItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.uwetrottmann.trakt5.entities.HistoryEntry
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

private const val TAG = "CollectedShowsAdapter"
class LastWatchedHistoryAdapter(
    private val sharedPreferences: SharedPreferences,
    private val tmdbImageLoader: TmdbImageLoader,
    private val callback: (historyEntry: HistoryEntry, action: Int, position: Int) -> Unit
) : ListAdapter<HistoryEntry, LastWatchedHistoryAdapter.WatchedHistoryVH>(
    COMPARATOR
) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LastWatchedHistoryAdapter.WatchedHistoryVH {
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
            watcheditemTimestamp.text = "Watched: " + currentItem.watched_at?.atZoneSameInstant(
                ZoneId.systemDefault())?.format(
                DateTimeFormatter.ofPattern(AppConstants.DEFAULT_DATE_TIME_FORMAT))

            when (currentItem.type) {
                "movie" -> {
                    movieitemTitle.text = currentItem.movie?.title
                    tmdbImageLoader.loadImages(
                        currentItem.movie?.ids?.trakt ?: 0,
                        ImageItemType.MOVIE,
                        currentItem.movie?.ids?.tmdb ?: 0,
                        currentItem.movie?.title,
                        null,
                        currentItem.movie?.language,
                        true,
                        movieitemPoster,
                        null
                    )
                }

                "episode" -> {
                    movieitemTitle.text = currentItem.episode?.title

                    tmdbImageLoader.loadImages(
                        currentItem.show?.ids?.trakt ?: 0,
                        ImageItemType.SHOW,
                        currentItem.show?.ids?.tmdb ?: 0,
                        currentItem.show?.title,
                        null,
                        currentItem.show?.language,
                        true,
                        movieitemPoster,
                        null
                    )

                }
                else -> {}
            }
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
        val COMPARATOR = object : DiffUtil.ItemCallback<HistoryEntry>() {
            override fun areItemsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: HistoryEntry,
                newItem: HistoryEntry
            ): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }
}