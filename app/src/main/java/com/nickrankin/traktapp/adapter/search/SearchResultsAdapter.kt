package com.nickrankin.traktapp.adapter.search

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.databinding.LayoutSearchResultItemBinding
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.uwetrottmann.trakt5.entities.SearchResult

class SearchResultsAdapter(
    private val tmdbImageLoader: TmdbImageLoader,
    private val callback: (results: SearchResult?) -> Unit
) : PagingDataAdapter<SearchResult, SearchResultsAdapter.ViewHolder>(
    COMPARATOR
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutSearchResultItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentSearchItem = getItem(position) ?: return

        var title = ""
        var overview = ""

        var traktId = 0
        var tmdbId = 0
        var language = ""
        var imageItemType: ImageItemType? = null

        when (currentSearchItem.type) {
            "movie" -> {
                traktId = currentSearchItem.movie?.ids?.trakt ?: 0
                tmdbId = currentSearchItem.movie?.ids?.tmdb ?: 0

                title = currentSearchItem.movie?.title ?: ""
                overview = currentSearchItem.movie?.overview ?: ""
                language = currentSearchItem.movie?.language ?: ""

                imageItemType = ImageItemType.MOVIE
            }
            "show" -> {
                traktId = currentSearchItem.show?.ids?.trakt ?: 0
                tmdbId = currentSearchItem.show?.ids?.tmdb ?: 0

                title = currentSearchItem.show?.title ?: ""
                overview = currentSearchItem.show?.overview ?: ""
                language = currentSearchItem.show?.language ?: ""

                imageItemType = ImageItemType.SHOW
            }
        }

        holder.bindings.apply {

            searchresultlayoutPoster.setImageDrawable(null)

            searchresultlayoutPoster.setImageResource(R.drawable.ic_trakt_svgrepo_com)

            searchresultlayoutTitle.text = title
            searchresultlayoutOverview.text = overview

            setBadgeTextView(imageItemType!!, searchresultlayoutBadge)

            tmdbImageLoader.loadImages(
                traktId,
                imageItemType,
                tmdbId,
                title,
                language,
                true,
                searchresultlayoutPoster,
                null,
                false
            )

            root.setOnClickListener {
                callback(currentSearchItem)
            }

        }

    }

    private fun setBadgeTextView(imageItemType: ImageItemType, textView: TextView) {
        return when(imageItemType) {
            ImageItemType.MOVIE -> {
                textView.text = imageItemType.name

                textView.setBackgroundColor(getColour(textView.context, R.color.red))
            }
            ImageItemType.SHOW -> {
                textView.text = imageItemType.name

                textView.setBackgroundColor(getColour(textView.context, R.color.green))
            }
            ImageItemType.EPISODE -> TODO()
            ImageItemType.PERSON -> TODO()
        }
    }

    private fun getColour(context: Context, @ColorRes colorResource: Int): Int {
        return ContextCompat.getColor(context, colorResource)
    }

    inner class ViewHolder(val bindings: LayoutSearchResultItemBinding) :
        RecyclerView.ViewHolder(bindings.root)

    companion object {
        val COMPARATOR = object : DiffUtil.ItemCallback<SearchResult>() {
            override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
                return oldItem.show?.ids?.trakt ?: 0 == newItem.show?.ids?.trakt ?: 0
            }
        }
    }
}