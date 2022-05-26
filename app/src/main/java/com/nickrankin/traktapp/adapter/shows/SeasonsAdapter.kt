package com.nickrankin.traktapp.adapter.shows

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.dao.show.TmSeasonAndStats
import com.nickrankin.traktapp.dao.show.model.TmSeason
import com.nickrankin.traktapp.databinding.SeasonListEntryBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.calculateProgress

class SeasonsAdapter(private val glide: RequestManager, val callback: (season: TmSeasonAndStats) -> Unit): ListAdapter<TmSeasonAndStats, SeasonsAdapter.SeasonsViewHolder>(
    COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeasonsViewHolder {
        return SeasonsViewHolder(SeasonListEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: SeasonsViewHolder, position: Int) {
        val currentItem = getItem(position)

        val season = currentItem.season
        val seasonStats = currentItem.watchedSeasonStats.find {
            it?.season == currentItem.season.season_number
        }

        holder.bindings.apply {
            val overviewExpandableTextView = seasonitemOverview

            overviewExpandableTextView.collapse()

            seasonitemPoster.setImageResource(R.drawable.ic_trakt_svgrepo_com)

            if(season.poster_path?.isNotEmpty() == true) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + season.poster_path)
                    .into(seasonitemPoster)
            }

            var seasonName = season.name

            // Add Season {number} to the season name for seasons without season id in title
            if(!seasonName.uppercase().contains("SEASON ${season.season_number}")) {
                seasonName = "Season ${season.season_number} - $seasonName"
            }

            seasonitemTitle.text = seasonName
            seasonitemOverview.text = season.overview

            if(seasonStats != null) {
                seasonitemProgressTitle.visibility = View.VISIBLE
                seasonitemProgrssbar.visibility = View.VISIBLE

                val progress = calculateProgress(seasonStats.completed.toDouble(), seasonStats.aired.toDouble())
                seasonitemProgressTitle.text = "Season Progress ($progress% - ${seasonStats.completed}/${seasonStats.aired} watched)"
                seasonitemProgrssbar.progress = progress
            } else {
                seasonitemProgressTitle.visibility = View.GONE
                seasonitemProgrssbar.visibility = View.GONE
            }



            overviewExpandableTextView.setOnClickListener {
                overviewExpandableTextView.toggle()
            }

            root.setOnClickListener {
                callback(currentItem)
            }
        }
    }
    inner class SeasonsViewHolder(val bindings: SeasonListEntryBinding): RecyclerView.ViewHolder(bindings.root)

    companion object {
        val COMPARATOR = object: DiffUtil.ItemCallback<TmSeasonAndStats>() {
            override fun areItemsTheSame(oldItem: TmSeasonAndStats, newItem: TmSeasonAndStats): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: TmSeasonAndStats, newItem: TmSeasonAndStats): Boolean {
                return oldItem.season.trakt_id == newItem.season.trakt_id
            }
        }
    }
}