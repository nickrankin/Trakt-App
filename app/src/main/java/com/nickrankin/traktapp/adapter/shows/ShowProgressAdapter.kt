package com.nickrankin.traktapp.adapter.shows

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nickrankin.traktapp.dao.show.model.ShowAndSeasonProgress
import com.nickrankin.traktapp.databinding.LayoutShowProgressItemBinding
import com.nickrankin.traktapp.helper.*

class ShowProgressAdapter(private val tmdbImageLoader: TmdbImageLoader, private val sharedPreferences: SharedPreferences, private val callback: (showAndSasonProgress: ShowAndSeasonProgress) -> Unit): ListAdapter<ShowAndSeasonProgress, ShowProgressAdapter.ShowProgressViewHolder>(COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShowProgressViewHolder {
        return ShowProgressViewHolder(
            LayoutShowProgressItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ShowProgressViewHolder, position: Int) {
        val currentItem = getItem(position)

        holder.bindings.apply {
            layoutshowprogressitemTitle.text = currentItem.showProgress.title
            layoutshowprogressitemLastWatched.text = getFormattedDateTime(currentItem.showProgress.last_watched_at,
                sharedPreferences.getString(AppConstants.DATE_FORMAT, AppConstants.DEFAULT_DATE_FORMAT),
                sharedPreferences.getString(AppConstants.TIME_FORMAT, AppConstants.DEFAULT_TIME_FORMAT)
            )
            layoutshowprogressitemOverview.text = currentItem.showProgress.overview.toString()

            var showProgress = 0

            currentItem.showSeasonProgress.map {
                // don't count specials
                if(it?.season_number != 0) {
                    showProgress += it?.played_episodes ?: 0

                }
            }


            if(showProgress > 100) {
                // TODO Find a better solution to this?
                // If for some reason Trakt Show data is not up to date, limit percentage to 100
                showProgress = 100
            }
            showProgress = calculateProgress(showProgress.toDouble(), currentItem.showProgress.total_aired.toDouble())



            layoutshowprogresitemProgressHeader.text = "Progress ($showProgress%)"
            layoutshowprogresitemProgressbar.progress = showProgress

            root.setOnClickListener {
                callback(currentItem)
            }
        }

        tmdbImageLoader.loadImages(currentItem.showProgress.show_trakt_id, ImageItemType.SHOW, currentItem.showProgress.show_tmdb_id, currentItem.showProgress.title, null, true, holder.bindings.layoutshowprogressitemPoster, null, false)
    }

    inner class ShowProgressViewHolder(val bindings: LayoutShowProgressItemBinding): RecyclerView.ViewHolder(bindings.root)

companion object {
    private val COMPARATOR = object: DiffUtil.ItemCallback<ShowAndSeasonProgress>() {
        override fun areItemsTheSame(
            oldItem: ShowAndSeasonProgress,
            newItem: ShowAndSeasonProgress
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: ShowAndSeasonProgress,
            newItem: ShowAndSeasonProgress
        ): Boolean {
            return oldItem.showProgress.show_trakt_id == newItem.showProgress.show_trakt_id
        }
    }
}
}