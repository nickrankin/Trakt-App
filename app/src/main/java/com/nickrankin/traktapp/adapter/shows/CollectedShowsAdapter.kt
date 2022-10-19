package com.nickrankin.traktapp.adapter.shows

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.util.Log
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
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.databinding.CollectedShowEntryListItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.getFormattedDate
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

private const val TAG = "CollectedShowsAdapter"
const val ICON_MARGIN = 2

class CollectedShowsAdapter(
    private val sharedPreferences: SharedPreferences,
    private val glide: RequestManager,
    private val tmdbImageLoader: TmdbImageLoader,
    private val callback: (selectedShow: CollectedShow, action: Int, position: Int) -> Unit
) : MediaEntryBaseAdapter<CollectedShow>(
    null, COMPARATOR) {
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = getItem(position)


        when(holder) {
            is MediaEntryBaseAdapter<*>.PosterViewHolder -> {
                holder.bindings.apply {
                    itemTitle.text = currentItem.show_title

                    if(currentItem.collected_at != null) {
                        itemTimestamp.visibility = View.VISIBLE
                        itemTimestamp.text = "Last collected: ${getFormattedDate(currentItem.collected_at, sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_FORMAT), sharedPreferences.getString("time_format", AppConstants.DEFAULT_TIME_FORMAT))}"
                    }


                    tmdbImageLoader.loadImages(
                        currentItem.show_trakt_id,
                        ImageItemType.SHOW,
                        currentItem.show_tmdb_id,
                        currentItem.show_title,
                        null,
                        currentItem.language,
                        true,
                        itemPoster,
                        null)

                    root.setOnClickListener {
                        callback(currentItem, ACTION_NAVIGATE_SHOW, position)
                    }
                }



            }
            is MediaEntryBaseAdapter<*>.CardViewHolder -> {
                holder.bindings.apply {
                    // Empty out the imageviews
                    itemPoster.setImageResource(R.drawable.ic_trakt_svgrepo_com)
                    itemBackdropImageview.setImageResource(R.drawable.ic_baseline_live_tv_24)

                    itemPoster.setImageResource(R.drawable.ic_trakt_svgrepo_com)

                    itemTitle.text = currentItem.show_title
                    itemWatchedDate.text = "Collected: " + currentItem.collected_at?.atZoneSameInstant(
                        ZoneId.systemDefault())?.format(
                        DateTimeFormatter.ofPattern(
                            sharedPreferences.getString(
                                "date_format",
                                AppConstants.DEFAULT_DATE_TIME_FORMAT
                            )
                        )
                    )

                    itemOverview.text = currentItem.show_overview

                    tmdbImageLoader.loadImages(
                        currentItem.show_trakt_id,
                        ImageItemType.SHOW,
                        currentItem.show_tmdb_id,
                        currentItem.show_title,
                        null,
                        currentItem.language,
                        true,
                        itemPoster,
                        itemBackdropImageview)

                    root.setOnClickListener {
                        callback(currentItem, ACTION_NAVIGATE_SHOW, position)
                    }
                }



            }
            else -> {
                throw  RuntimeException("Invalid ViewHolder ${holder.javaClass.name}")
            }
        }


    }

    companion object {
        const val ACTION_NAVIGATE_SHOW = 0
        const val ACTION_REMOVE_COLLECTION = 1

        val COMPARATOR = object : DiffUtil.ItemCallback<CollectedShow>() {
            override fun areItemsTheSame(oldItem: CollectedShow, newItem: CollectedShow): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: CollectedShow,
                newItem: CollectedShow
            ): Boolean {
                return oldItem.show_trakt_id == newItem.show_trakt_id
            }
        }
    }
}