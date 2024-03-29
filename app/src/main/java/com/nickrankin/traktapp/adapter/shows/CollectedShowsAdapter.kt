package com.nickrankin.traktapp.adapter.shows

import android.content.SharedPreferences
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.AdaptorActionControls
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.getFormattedDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

private const val TAG = "CollectedShowsAdapter"
const val ICON_MARGIN = 2

class CollectedShowsAdapter(controls: AdaptorActionControls<CollectedShow>,
    private val sharedPreferences: SharedPreferences,
    private val glide: RequestManager,
    private val tmdbImageLoader: TmdbImageLoader) : MediaEntryBaseAdapter<CollectedShow>(
    controls, COMPARATOR) {
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, holder.absoluteAdapterPosition)

        val currentItem = getItem(position)


        when(holder) {
            is MediaEntryBaseAdapter<*>.PosterViewHolder -> {
                holder.bindings.apply {
                    itemTitle.text = currentItem.show_title

                    if(currentItem.collected_at != null) {
                        itemTimestamp.visibility = View.VISIBLE
                        itemTimestamp.text = "Last collected: ${getFormattedDateTime(currentItem.collected_at, sharedPreferences.getString(AppConstants.DATE_FORMAT, AppConstants.DEFAULT_DATE_FORMAT), sharedPreferences.getString(AppConstants.TIME_FORMAT, AppConstants.DEFAULT_TIME_FORMAT))}"
                    }


                    tmdbImageLoader.loadImages(
                        currentItem.show_trakt_id,
                        ImageItemType.SHOW,
                        currentItem.show_tmdb_id,
                        currentItem.show_title,
                        currentItem.language,
                        true,
                        itemPoster,
                        null,
                    false)
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
                                AppConstants.DATE_FORMAT,
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
                        currentItem.language,
                        true,
                        itemPoster,
                        itemBackdropImageview,
                    false)
                }



            }
            else -> {
                throw  RuntimeException("Invalid ViewHolder ${holder.javaClass.name}")
            }
        }
    }

    override fun reloadImages(
        selectedItem: CollectedShow,
        posterImageView: ImageView,
        backdropImageView: ImageView?
    ) {
        tmdbImageLoader.loadImages(
            selectedItem.show_trakt_id,
            ImageItemType.SHOW,
            selectedItem.show_tmdb_id,
            selectedItem.show_title,
            selectedItem.language,
            true,
            posterImageView,
            backdropImageView,
            true)
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