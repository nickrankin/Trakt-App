package com.nickrankin.traktapp.adapter.shows

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.databinding.ShowLayoutItemCondensedBinding
import com.nickrankin.traktapp.helper.AppConstants
import org.threeten.bp.format.DateTimeFormatter

class CollectedShowsPickerAdapter(private val sharedPreferences: SharedPreferences, private val callback: (collectedShow: CollectedShow) -> Unit): ListAdapter<CollectedShow, CollectedShowsPickerAdapter.CollectedShowVH>(COMPARATOR) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectedShowVH {
        return CollectedShowVH(ShowLayoutItemCondensedBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: CollectedShowVH, position: Int) {
        val selectedShow = getItem(position)

        holder.bindings.apply {
            collectedshowlayoutTitle.text = selectedShow.show_title
            collectedshowlayoutCollectedAy.text = "Collected At: " + selectedShow.collected_at?.format(DateTimeFormatter.ofPattern(sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_TIME_FORMAT)))

            root.setOnClickListener {
               callback(selectedShow)

            }
        }

    }

    inner class CollectedShowVH(val bindings: ShowLayoutItemCondensedBinding): RecyclerView.ViewHolder(bindings.root)

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