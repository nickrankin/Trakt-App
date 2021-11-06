package com.nickrankin.traktapp.adapter.shows

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.databinding.EpisodeLayoutItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import org.apache.commons.lang3.time.DateFormatUtils

class EpisodesAdapter(private val sharedPreferences: SharedPreferences, private val glide: RequestManager, private val callback: (selectedEpisode: TmEpisode) -> Unit): RecyclerView.Adapter<EpisodesAdapter.EpisodesViewHolder>() {
    private var episodes: List<TmEpisode> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodesViewHolder {
        return EpisodesViewHolder(EpisodeLayoutItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: EpisodesViewHolder, position: Int) {
        val currentItem = episodes[position]

        holder.bindings.apply {
            episodeitemStillImageview.setImageResource(R.drawable.ic_baseline_live_tv_24)
            episodeitemName.text = currentItem.name

            if(currentItem.air_date != null) {
                episodeitemAirDate.text = "First aired: " + DateFormatUtils.format(currentItem.air_date, sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_TIME_FORMAT))
            }

            episodeitemNumber.text = "Episode Number: " + currentItem.episode_number

            if(currentItem.still_path?.isNotEmpty() == true) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + currentItem.still_path)
                    .into(episodeitemStillImageview)
            }

            episodeitemOverview.text = currentItem.overview

            root.setOnClickListener {
                callback(currentItem)
            }
        }
    }

    override fun getItemCount(): Int {
        return episodes.size
    }

    fun updateData(episodes: List<TmEpisode>) {
        this.episodes = episodes
        notifyDataSetChanged()
    }

    inner class EpisodesViewHolder(val bindings: EpisodeLayoutItemBinding): RecyclerView.ViewHolder(bindings.root)
}