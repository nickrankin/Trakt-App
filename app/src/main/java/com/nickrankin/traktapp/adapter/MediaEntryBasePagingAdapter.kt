package com.nickrankin.traktapp.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nickrankin.traktapp.dao.movies.model.WatchedMovieAndStats
import com.nickrankin.traktapp.databinding.ViewCardItemBinding
import com.nickrankin.traktapp.databinding.ViewPosterItemBinding

private const val TAG = "MediaEntryBasePagingAda"
open class MediaEntryBasePagingAdapter<T: Any> constructor(protected val controls: AdaptorActionControls<T>?, diffCallback: DiffUtil.ItemCallback<T>): PagingDataAdapter<T, RecyclerView.ViewHolder>(diffCallback) {

    private var currentViewType = VIEW_TYPE_POSTER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        when (viewType) {
            VIEW_TYPE_POSTER -> {
                return PosterViewHolder(
                    ViewPosterItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            VIEW_TYPE_CARD -> {
                return CardViewHolder(
                    ViewCardItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            else -> {
                return PosterViewHolder(
                    ViewPosterItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(holder.absoluteAdapterPosition)

        if (item != null) {
            when (holder) {
                is PosterViewHolder -> {
                    holder.bindings.apply {

                        if (controls != null) {

                            holder.itemView.setOnClickListener {
                                controls.entrySelectedCallback(item)
                            }

                            if (controls.menuResource != null) {
                                itemMenu.visibility = View.VISIBLE

                                itemMenu.setOnClickListener {
                                    showMenu(item, itemMenu)
                                }
                            }
                        }


                    }
                }
                is CardViewHolder -> {
                    holder.bindings.apply {

                        if (controls != null) {
                            holder.itemView.setOnClickListener {
                                controls.entrySelectedCallback(item)
                            }

                            if (controls.showMenuOnCardView) {
                                itemMenu.setOnClickListener {
                                    showMenu(item, itemMenu)

                                }
                            } else {
                                itemMenu.visibility = View.GONE
                            }

                            if (controls.buttonText != null) {
                                buttonControl.visibility = View.VISIBLE
                                buttonControl.icon = controls.buttonIconResource
                                buttonControl.text = controls.buttonText

                                buttonControl.setOnClickListener {
                                    controls.buttonClickedCallback(item)

                                }

                            }
                        }
                    }
                }
                else -> {
                    Log.e(TAG, "onBindViewHolder: Invalid ViewHolder ${holder.javaClass.name}",)
                }
            }
        }

    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        Log.e(TAG, "onViewRecycled: Recycled $holder", )
    }

    private fun showMenu(selectedItem: T, v: View) {
        if(controls?.menuResource == null) {
            return
        }

        val popup = PopupMenu(v.context!!, v)
        popup.menuInflater.inflate(controls.menuResource, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            // Respond to menu item click.
            controls.menuItemSelectedCallback(selectedItem, menuItem.itemId)

            true
        }

        popup.setOnDismissListener { popupMenu ->
            popupMenu.dismiss()
            // Respond to popup being dismissed.
        }
        // Show the popup menu.
        popup.show()
    }

    override fun getItemViewType(position: Int): Int {
        super.getItemViewType(currentViewType)
        return currentViewType
    }

    fun switchView(viewType: Int) {
        currentViewType = when(viewType) {
            VIEW_TYPE_CARD -> {
                VIEW_TYPE_CARD
            }
            VIEW_TYPE_POSTER -> {
                VIEW_TYPE_POSTER
            }
            else -> {
                throw RuntimeException("Invalid viewtype specified, ($viewType)")
            }
        }
    }



    class PosterViewHolder(val bindings: ViewPosterItemBinding) :
        RecyclerView.ViewHolder(bindings.root)

    class CardViewHolder(val bindings: ViewCardItemBinding) :
        RecyclerView.ViewHolder(bindings.root)

    companion object {
        const val VIEW_TYPE_POSTER = 0
        const val VIEW_TYPE_CARD = 1
        const val ACTION_NAVIGATE_SHOW = 0
    }
}