package com.nickrankin.traktapp.adapter.shows

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.databinding.ShowLayoutItemCondensedBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.uwetrottmann.trakt5.entities.SearchResult
import org.threeten.bp.format.DateTimeFormatter

class SearchResultShowsPickerAdapter(private val sharedPreferences: SharedPreferences, private val callback: (collectedShow: SearchResult?) -> Unit): PagingDataAdapter<SearchResult, SearchResultShowsPickerAdapter.CollectedShowVH>(COMPARATOR) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectedShowVH {
        return CollectedShowVH(ShowLayoutItemCondensedBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: CollectedShowVH, position: Int) {
        val selectedShow = getItem(position)

        holder.bindings.apply {
            collectedshowlayoutTitle.text = selectedShow?.show?.title
            collectedshowlayoutCollectedAy.text = "First Aired: " + selectedShow?.show?.first_aired?.format(
                DateTimeFormatter.ofPattern(sharedPreferences.getString(AppConstants.DATE_FORMAT, AppConstants.DEFAULT_DATE_TIME_FORMAT)))

            root.setOnClickListener {
               callback(selectedShow)

            }
        }
    }



    inner class CollectedShowVH(val bindings: ShowLayoutItemCondensedBinding): RecyclerView.ViewHolder(bindings.root)

    companion object {
        val COMPARATOR = object: DiffUtil.ItemCallback<SearchResult>() {
            override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: SearchResult,
                newItem: SearchResult
            ): Boolean {
                return oldItem.show?.ids?.trakt == newItem.show?.ids?.trakt
            }
        }
    }
}