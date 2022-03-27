package com.nickrankin.traktapp.adapter.shows

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.blogc.android.views.ExpandableTextView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.dao.show.model.TrackedEpisode
import com.nickrankin.traktapp.dao.show.model.TrackedShowWithEpisodes
import com.nickrankin.traktapp.databinding.TrackedShowListItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.PosterImageLoader
import org.threeten.bp.format.DateTimeFormatter

private const val TAG = "TrackedShowsAdapter"
class TrackedShowsAdapter(private val glide: RequestManager, private val imageLoader: PosterImageLoader, private val callback: (trackedShow: TrackedShowWithEpisodes) -> Unit, private val upcomingEpisodesCallback: (showTitle: String?, episodes: List<TrackedEpisode?>) -> Unit): ListAdapter<TrackedShowWithEpisodes, TrackedShowsAdapter.TrackedShowsViewHolder>(
    COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackedShowsViewHolder {
        return TrackedShowsViewHolder(TrackedShowListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: TrackedShowsViewHolder, position: Int) {
        val selectedShow = getItem(position)

        holder.binding.apply {
            trackedshowitemShowTitle.text = "${selectedShow.trackedShow.title} (${selectedShow?.trackedShow?.releaseDate?.year})"
            trackedshowitemShowCollectedAt.text = "Tracked on: " + selectedShow.trackedShow.tracked_on.format(
                DateTimeFormatter.ofPattern("dd/MM/YYYY"))

            trackedshowitemShowOverview.text = selectedShow.trackedShow.overview

            imageLoader.loadImage(selectedShow.trackedShow.trakt_id, selectedShow.trackedShow.tmdb_id, null, selectedShow.trackedShow.title ?: "", null, true) { posterRes ->
                if(posterRes.poster_path != null) {
                    glide
                        .load(AppConstants.TMDB_POSTER_URL + posterRes.poster_path)
                        .into(trackedshowitemShowPoster)
                } else {
                    trackedshowitemShowPoster.setImageResource(R.drawable.ic_trakt_svgrepo_com)
                }

            }

            trackedshowitemShowOverview.setOnClickListener {
                val expandableTextView = it as ExpandableTextView

                expandableTextView.toggle()
            }

            if(selectedShow.episodes.isNotEmpty()) {
                trackedshowitemUpcomingEpisodes.visibility = View.VISIBLE

                trackedshowitemUpcomingEpisodes.text = "Airing Soon (${selectedShow.episodes?.size})"

                trackedshowitemUpcomingEpisodes.setOnClickListener {
                    upcomingEpisodesCallback(selectedShow.trackedShow.title, selectedShow.episodes)
                }
            } else {
                trackedshowitemUpcomingEpisodes.visibility = View.GONE
            }

            root.setOnClickListener {
                callback(selectedShow)
            }



        }

    }

    inner class TrackedShowsViewHolder(val binding: TrackedShowListItemBinding): RecyclerView.ViewHolder(binding.root)

    companion object {
        val COMPARATOR = object: DiffUtil.ItemCallback<TrackedShowWithEpisodes>() {
            override fun areItemsTheSame(
                oldItem: TrackedShowWithEpisodes,
                newItem: TrackedShowWithEpisodes
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: TrackedShowWithEpisodes,
                newItem: TrackedShowWithEpisodes
            ): Boolean {
                return oldItem.trackedShow.trakt_id == newItem.trackedShow.trakt_id
            }
        }
    }
}