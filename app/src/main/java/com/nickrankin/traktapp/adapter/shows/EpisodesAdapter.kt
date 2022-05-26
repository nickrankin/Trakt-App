package com.nickrankin.traktapp.adapter.shows

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.dao.show.model.TmEpisodeAndStats
import com.nickrankin.traktapp.databinding.EpisodeLayoutItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.getFormattedDate
import org.apache.commons.lang3.time.DateFormatUtils

class EpisodesAdapter(
    private val sharedPreferences: SharedPreferences,
    private val glide: RequestManager,
    private val callback: (selectedEpisode: TmEpisodeAndStats) -> Unit
) : ListAdapter<TmEpisodeAndStats, EpisodesAdapter.EpisodesViewHolder>(
    COMPARATOR
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodesViewHolder {
        return EpisodesViewHolder(
            EpisodeLayoutItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: EpisodesViewHolder, position: Int) {
        val currentItem = getItem(position)

        val episode = currentItem.episode

        holder.bindings.apply {
            val overviewExpandableTextView = episodeitemOverview
            overviewExpandableTextView.collapse()

            episodeitemStillImageview.setImageResource(R.drawable.ic_baseline_live_tv_24)
            episodeitemName.text = episode.name

            if (episode.air_date != null) {
                episodeitemAirDate.text = "First aired: " + getFormattedDate(
                    episode.air_date,
                    sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_FORMAT)!!,
                    sharedPreferences.getString("timeFormat", AppConstants.DEFAULT_TIME_FORMAT)!!
                )
            }

            if (currentItem.watchedEpisodeStats.isNotEmpty()) {
                val currentWatchedEpisode =
                    currentItem.watchedEpisodeStats.find { it?.season == currentItem.episode.season_number && it?.episode == currentItem.episode.episode_number }

                resetWatchedText(this)

                if (currentWatchedEpisode != null) {
                    // Episode was watched
                    DrawableCompat.setTint(
                        DrawableCompat.wrap(episodeitemWatchedStatusIcon.drawable),
                        ContextCompat.getColor(root.context, R.color.dark_green)
                    )

                    episodeitemWatchedStatusText.setTextColor(
                        ContextCompat.getColor(
                            root.context,
                            R.color.dark_green
                        )
                    )

                    if (currentWatchedEpisode.last_watched_at != null) {
                        episodeitemWatchedStatusText.text = "Watched - Last Watched ${
                            getFormattedDate(
                                currentWatchedEpisode.last_watched_at!!,
                                sharedPreferences.getString(
                                    "date_format",
                                    AppConstants.DEFAULT_DATE_FORMAT
                                )!!,
                                sharedPreferences.getString(
                                    "time_format",
                                    AppConstants.DEFAULT_TIME_FORMAT
                                )!!
                            )
                        }"

                    } else {
                        episodeitemWatchedStatusText.text = "Watched"

                    }

                    if(currentItem.ratingEpisodeStats != null) {
                        val currentRating = currentItem.ratingEpisodeStats.find { it.season == currentItem.episode.season_number && it.episode == currentItem.episode.episode_number }

                        if(currentRating != null) {
                            episodeitemRating.visibility = View.VISIBLE
                            episodeitemRating.text = "Your rating: ${currentRating.rating}"
                        } else {
                            episodeitemRating.visibility = View.GONE
                        }
                    }
                }

            } else {

                DrawableCompat.setTint(
                    DrawableCompat.wrap(episodeitemWatchedStatusIcon.drawable),
                    ContextCompat.getColor(root.context, R.color.red)
                )

                episodeitemWatchedStatusText.setTextColor(
                    ContextCompat.getColor(
                        root.context,
                        R.color.red
                    )
                )
                episodeitemWatchedStatusText.text = "Unwatched"
            }

            episodeitemNumber.text = "Episode Number: " + episode.episode_number

            if (episode.still_path?.isNotEmpty() == true) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + episode.still_path)
                    .into(episodeitemStillImageview)
            }

            episodeitemOverview.text = episode.overview

            overviewExpandableTextView.setOnClickListener {
                overviewExpandableTextView.toggle()
            }

            root.setOnClickListener {
                callback(currentItem)
            }
        }
    }

    private fun resetWatchedText(bindings: EpisodeLayoutItemBinding) {
        bindings.apply {
            DrawableCompat.setTint(
                DrawableCompat.wrap(episodeitemWatchedStatusIcon.drawable),
                ContextCompat.getColor(root.context, R.color.red)
            )
            episodeitemWatchedStatusText.setTextColor(
                ContextCompat.getColor(
                    root.context,
                    R.color.red
                )
            )

            episodeitemWatchedStatusText.text = "Unwatched"
        }
    }

    inner class EpisodesViewHolder(val bindings: EpisodeLayoutItemBinding) :
        RecyclerView.ViewHolder(bindings.root)

    companion object {
        val COMPARATOR = object : DiffUtil.ItemCallback<TmEpisodeAndStats>() {
            override fun areItemsTheSame(
                oldItem: TmEpisodeAndStats,
                newItem: TmEpisodeAndStats
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: TmEpisodeAndStats,
                newItem: TmEpisodeAndStats
            ): Boolean {
                return oldItem.episode.episode_trakt_id == newItem.episode.episode_trakt_id
            }
        }
    }
}