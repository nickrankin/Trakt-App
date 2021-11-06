package com.nickrankin.traktapp.adapter.shows

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.dao.show.model.TmSeason
import com.nickrankin.traktapp.databinding.SeasonListEntryBinding
import com.nickrankin.traktapp.helper.AppConstants

class SeasonsAdapter(private val glide: RequestManager, val callback: (season: TmSeason) -> Unit): RecyclerView.Adapter<SeasonsAdapter.SeasonsViewHolder>() {
    private var seasons: List<TmSeason> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeasonsViewHolder {
        return SeasonsViewHolder(SeasonListEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: SeasonsViewHolder, position: Int) {
        val currentItem = seasons[position]

        holder.bindings.apply {
            seasonitemPoster.setImageResource(R.drawable.ic_trakt_svgrepo_com)

            if(currentItem.poster_path?.isNotEmpty() == true) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + currentItem.poster_path)
                    .into(seasonitemPoster)
            }

            var seasonName = currentItem.name

            // Add Season {number} to the season name for seasons without season id in title
            if(!seasonName.uppercase().contains("SEASON ${currentItem.season_number}")) {
                seasonName = "Season ${currentItem.season_number} - $seasonName"
            }

            seasonitemTitle.text = seasonName
            seasonitemOverview.text = currentItem.overview

            root.setOnClickListener {
                callback(currentItem)
            }
        }
    }

    override fun getItemCount(): Int {
        return seasons.size
    }
    fun updateSeasons(seasons: List<TmSeason>) {
        this.seasons = seasons
        notifyDataSetChanged()
    }

    inner class SeasonsViewHolder(val bindings: SeasonListEntryBinding): RecyclerView.ViewHolder(bindings.root)
}