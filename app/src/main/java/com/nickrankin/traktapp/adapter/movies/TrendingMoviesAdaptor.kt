package com.nickrankin.traktapp.adapter.movies

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.blogc.android.views.ExpandableTextView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.databinding.TrendingMovieEntryListItemBinding
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.uwetrottmann.trakt5.entities.TrendingMovie

private const val TAG = "TrendingMoviesAdaptor"
class TrendingMoviesAdaptor(private val tmdbImageLoader: TmdbImageLoader, private val callback: (results: TrendingMovie?) -> Unit): MediaEntryBaseAdapter<TrendingMovie>(
    null, COMPARATOR) {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = getItem(position)

        when(holder) {
            is MediaEntryBaseAdapter<*>.CardViewHolder -> {
                holder.bindings.apply {

                    itemTitle.text = currentItem.movie?.title
                    itemWatchedDate.text = "${currentItem?.watchers ?: 0} watching this right now"
                    itemOverview.text = currentItem.movie?.overview

                    tmdbImageLoader.loadImages(currentItem.movie?.ids?.trakt ?: 0, ImageItemType.MOVIE, currentItem.movie?.ids?.tmdb ?: 0,  currentItem.movie?.title, null, currentItem?.movie?.language, true, itemPoster, itemBackdropImageview)

                    root.setOnClickListener {
                        callback(currentItem)
                    }
                }
            }
            is MediaEntryBaseAdapter<*>.PosterViewHolder -> {
                holder.bindings.apply {
                    itemTitle.text = currentItem.movie?.title

                    itemTimestamp.visibility = View.VISIBLE
                    itemIcon.visibility = View.VISIBLE
                    itemIcon.setImageResource(R.drawable.ic_baseline_people_24)

                    itemTimestamp.text = "${currentItem?.watchers ?: 0} watching this now"
                    tmdbImageLoader.loadImages(currentItem.movie?.ids?.trakt ?: 0, ImageItemType.MOVIE, currentItem.movie?.ids?.tmdb ?: 0,  currentItem.movie?.title, null, currentItem?.movie?.language, true, itemPoster, null)

                    root.setOnClickListener {
                        callback(currentItem)
                    }
                }
            }
        }


    }

    companion object {
        val COMPARATOR = object: DiffUtil.ItemCallback<TrendingMovie>() {
            override fun areItemsTheSame(oldItem: TrendingMovie, newItem: TrendingMovie): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: TrendingMovie, newItem: TrendingMovie): Boolean {
                return  oldItem.movie?.ids?.trakt ?: 0 == newItem.movie?.ids?.trakt ?: 0
            }
        }
    }
}