package com.nickrankin.traktapp.adapter.shows

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.databinding.CollectedShowEntryListItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.PosterImageLoader
import org.threeten.bp.format.DateTimeFormatter

class CollectedShowsAdapter(private val sharedPreferences: SharedPreferences, private val glide: RequestManager, private val imageLoader: PosterImageLoader, private val callback: (selectedShow: CollectedShow) -> Unit): ListAdapter<CollectedShow, CollectedShowsAdapter.CollectedShowsViewHolder>(
    COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectedShowsViewHolder {
        return CollectedShowsViewHolder(CollectedShowEntryListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: CollectedShowsViewHolder, position: Int) {
        holder.setIsRecyclable(false)

        val currentItem = getItem(position)

        holder.bindings.apply {
            collectedentryitemTitle.text = currentItem.show_title
            collectedentryitemCollectedDate.text = "Collected: " + currentItem.collected_at?.format(
                DateTimeFormatter.ofPattern(sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_TIME_FORMAT)))
            collectedentryitemOverview.text = currentItem.show_overview

            imageLoader.loadImage(currentItem.show_tmdb_id, "en,null", callback = {posterPath ->
                if(posterPath.isNotEmpty()) {
                    glide
                        .load(AppConstants.TMDB_POSTER_URL + posterPath)
                        .into(collectedentryitemPoster)
                }
            })

            root.setOnClickListener {
                callback(currentItem)
            }
        }
    }

    inner class CollectedShowsViewHolder(val bindings: CollectedShowEntryListItemBinding): RecyclerView.ViewHolder(bindings.root)

    companion object {
        val COMPARATOR = object: DiffUtil.ItemCallback<CollectedShow>() {
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