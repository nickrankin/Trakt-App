package com.nickrankin.traktapp.adapter.movies

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.blogc.android.views.ExpandableTextView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.adapter.AdaptorActionControls
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.uwetrottmann.trakt5.entities.Movie

private const val TAG = "ReccomendedMoviesAdapto"
class ReccomendedMoviesAdaptor(private val tmdbImageLoader: TmdbImageLoader, adapterControls: AdaptorActionControls<Movie>): MediaEntryBaseAdapter<Movie>(
    adapterControls, COMPARATOR) {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        val currentItem = getItem(position)

        when(holder) {
            is MediaEntryBaseAdapter<*>.CardViewHolder -> {
                holder.bindings.apply {
                    itemTitle.text = currentItem?.title
                    itemWatchedDate.visibility = View.GONE
                    itemOverview.text = currentItem?.overview

                    tmdbImageLoader.loadImages(currentItem?.ids?.trakt ?: 0, ImageItemType.MOVIE,currentItem?.ids?.tmdb ?: 0,  currentItem.title, currentItem.language, true, itemPoster, itemBackdropImageview, false)
                }
            }
            is MediaEntryBaseAdapter<*>.PosterViewHolder -> {
                holder.bindings.apply {
                    itemTitle.text = currentItem?.title
                    itemTimestamp.visibility = View.GONE

                    tmdbImageLoader.loadImages(currentItem?.ids?.trakt ?: 0, ImageItemType.MOVIE,currentItem?.ids?.tmdb ?: 0,  currentItem.title, currentItem.language, true, itemPoster, null, false)

                }
            }
            else -> {
                Log.e(TAG, "onBindViewHolder: Invalid ViewHolder ${holder.javaClass.name}")
            }
        }

    }

    override fun reloadImages(
        selectedItem: Movie,
        posterImageView: ImageView,
        backdropImageView: ImageView?
    ) {
        tmdbImageLoader.loadImages(selectedItem.ids?.trakt ?: 0, ImageItemType.MOVIE,selectedItem.ids?.tmdb ?: 0,  selectedItem.title, selectedItem.language, true, posterImageView, backdropImageView, true)

    }

    companion object {
        const val ACTION_VIEW = 0
        const val ACTION_REMOVE = 1
        val COMPARATOR = object: DiffUtil.ItemCallback<Movie>() {
            override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean {
                return  oldItem.ids?.trakt ?: 0 == newItem.ids?.trakt ?: 0
            }
        }
    }
}