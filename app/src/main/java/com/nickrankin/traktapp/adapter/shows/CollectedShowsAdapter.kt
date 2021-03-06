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
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.databinding.CollectedShowEntryListItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

private const val TAG = "CollectedShowsAdapter"
const val ICON_MARGIN = 2

class CollectedShowsAdapter(
    private val sharedPreferences: SharedPreferences,
    private val glide: RequestManager,
    private val tmdbImageLoader: TmdbImageLoader,
    private val callback: (selectedShow: CollectedShow, action: Int, position: Int) -> Unit
) : ListAdapter<CollectedShow, CollectedShowsAdapter.CollectedShowsViewHolder>(
    COMPARATOR
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectedShowsViewHolder {
        return CollectedShowsViewHolder(
            CollectedShowEntryListItemBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: CollectedShowsViewHolder, position: Int) {
        val currentItem = getItem(position)

        holder.bindings.apply {
            val expandableTextView = collectedentryitemOverview
            expandableTextView.collapse()

            // Empty out the imageviews
            collectedentryitemPoster.setImageResource(R.drawable.ic_trakt_svgrepo_com)
            collectedentryitemBackdrop.setImageResource(R.drawable.ic_baseline_live_tv_24)

            collectedentryitemPoster.setImageResource(R.drawable.ic_trakt_svgrepo_com)

            collectedentryitemTitle.text = currentItem.show_title
            collectedentryitemCollectedDate.text = "Collected: " + currentItem.collected_at?.atZoneSameInstant(
                ZoneId.systemDefault())?.format(
                DateTimeFormatter.ofPattern(
                    sharedPreferences.getString(
                        "date_format",
                        AppConstants.DEFAULT_DATE_TIME_FORMAT
                    )
                )
            )
            collectedentryitemOverview.text = currentItem.show_overview

            tmdbImageLoader.loadImages(
                currentItem.show_trakt_id,
                ImageItemType.SHOW,
                currentItem.show_tmdb_id,
                currentItem.show_title,
                null,
                currentItem.language,
                true,
                collectedentryitemPoster,
            collectedentryitemBackdrop)

            root.setOnClickListener {
                callback(currentItem, ACTION_NAVIGATE_SHOW, position)
            }

            collectedentryitemOverview.setOnClickListener {
                expandableTextView.toggle()
            }

            collectedentryitemRemoveButton.setOnClickListener {
                callback(currentItem, ACTION_REMOVE_COLLECTION, position)

            }


        }
    }

    inner class CollectedShowsViewHolder(val bindings: CollectedShowEntryListItemBinding) :
        RecyclerView.ViewHolder(bindings.root)

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