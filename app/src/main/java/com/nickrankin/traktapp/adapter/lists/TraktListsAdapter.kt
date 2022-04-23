package com.nickrankin.traktapp.adapter.lists

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.databinding.TraktListItemBinding
import org.threeten.bp.format.DateTimeFormatter

class TraktListsAdapter(private val sharedPreferences: SharedPreferences): ListAdapter<TraktList, TraktListsAdapter.ViewHolder>(COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(TraktListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = getItem(position)

        holder.bindings.apply {
            listitemName.text = currentItem.name
            listitemCreated.text = currentItem.created_at.format(DateTimeFormatter.ofPattern(sharedPreferences.getString("date_format", "dd/MM/yyyy")))
            listitemCount.text = "In list ${currentItem.item_count} items"
        }
    }

    inner class ViewHolder(val bindings: TraktListItemBinding): RecyclerView.ViewHolder(bindings.root)


    companion object {
        private val COMPARATOR = object: DiffUtil.ItemCallback<TraktList>() {
            override fun areItemsTheSame(oldItem: TraktList, newItem: TraktList): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: TraktList, newItem: TraktList): Boolean {
                return oldItem.trakt_id == newItem.trakt_id
            }
        }
    }
}