package com.nickrankin.traktapp.adapter.shows

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.AdaptorActionControls
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.databinding.ReccomendedShowEntryListItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.uwetrottmann.trakt5.entities.Show

private const val TAG = "RecommendedShowsAdapter"
class RecommendedShowsAdapter(private val tmdbImageLoader: TmdbImageLoader, controls: AdaptorActionControls<Show>): MediaEntryBaseAdapter<Show>(
    controls, COMPARATOR) {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        val currentItem = getItem(position)

        when(holder) {
            is MediaEntryBaseAdapter<*>.CardViewHolder -> {
                holder.bindings.apply {
                    itemTitle.text = currentItem?.title
                    itemWatchedDate.visibility = View.GONE
                    itemOverview.text = currentItem?.overview

                    tmdbImageLoader.loadImages(currentItem?.ids?.trakt ?: 0, ImageItemType.SHOW,currentItem?.ids?.tmdb ?: 0,currentItem?.title ?: "", currentItem.language, true, itemPoster, itemBackdropImageview, false)
                }
            }
            is MediaEntryBaseAdapter<*>.PosterViewHolder -> {
                holder.bindings.apply {
                    itemTitle.text = currentItem?.title
                    itemTimestamp.visibility = View.GONE

                    tmdbImageLoader.loadImages(currentItem?.ids?.trakt ?: 0, ImageItemType.SHOW,currentItem?.ids?.tmdb ?: 0,currentItem?.title ?: "", currentItem.language, true, itemPoster, null, false)

                }
            }
            else -> {
                Log.e(TAG, "onBindViewHolder: Error invalid ViewHolder ${holder.javaClass.name}", )
            }
        }
    }

    override fun reloadImages(
        selectedItem: Show,
        posterImageView: ImageView,
        backdropImageView: ImageView?
    ) {
        tmdbImageLoader.loadImages(selectedItem?.ids?.trakt ?: 0, ImageItemType.SHOW,selectedItem?.ids?.tmdb ?: 0,selectedItem?.title ?: "", selectedItem.language, true, posterImageView, backdropImageView, true)

    }

    companion object {
        const val ACTION_VIEW = 0
        const val ACTION_REMOVE = 1

        val COMPARATOR = object: DiffUtil.ItemCallback<Show>() {
            override fun areItemsTheSame(oldItem: Show, newItem: Show): Boolean {
                return oldItem === newItem

            }

            override fun areContentsTheSame(oldItem: Show, newItem: Show): Boolean {
                return oldItem.ids!!.trakt  == newItem.ids!!.trakt

            }

            override fun getChangePayload(oldItem: Show, newItem: Show): Any? {
                Log.e(TAG, "getChangePayload: Change detected ${oldItem.title} // ${newItem.title}", )
                return super.getChangePayload(oldItem, newItem)
            }
        }
    }
}