package com.nickrankin.traktapp.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.MenuRes
import androidx.recyclerview.widget.*
import com.nickrankin.traktapp.databinding.ViewCardItemBinding
import com.nickrankin.traktapp.databinding.ViewPosterItemBinding

private const val TAG = "MediaEntryBaseAdapter"
abstract class MediaEntryBaseAdapter<T> constructor(protected val controls: AdaptorActionControls<T>?, diffCallback: DiffUtil.ItemCallback<T>): ListAdapter<T, RecyclerView.ViewHolder>(diffCallback) {

    private var currentViewType = VIEW_TYPE_POSTER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_POSTER -> {
                PosterViewHolder(
                    ViewPosterItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            VIEW_TYPE_CARD -> {
                CardViewHolder(
                    ViewCardItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            else -> {
                PosterViewHolder(
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
        when(holder) {
            is MediaEntryBaseAdapter<*>.PosterViewHolder -> {
                holder.bindings.apply {
                    if(controls != null) {

                        holder.itemView.setOnClickListener {
                            controls.entrySelectedCallback(getItem(holder.absoluteAdapterPosition))
                        }

                        if (controls.menuResource != null) {
                            itemMenu.visibility = View.VISIBLE

                            itemMenu.setOnClickListener {
                                showMenu(getItem(holder.absoluteAdapterPosition), itemMenu)
                            }
                        }
                    }


                }
            }
            is MediaEntryBaseAdapter<*>.CardViewHolder -> {
                holder.bindings.apply {

                    if(controls != null) {
                        holder.itemView.setOnClickListener {
                            controls.entrySelectedCallback(getItem(holder.absoluteAdapterPosition))
                        }

                        if(controls.showMenuOnCardView) {
                            itemMenu.setOnClickListener {
                                showMenu(getItem(holder.absoluteAdapterPosition), itemMenu)

                            }
                        } else {
                            itemMenu.visibility = View.GONE
                        }

                        if(controls.buttonText != null) {
                            buttonControl.visibility = View.VISIBLE
                            buttonControl.icon = controls.buttonIconResource
                            buttonControl.text = controls.buttonText

                            buttonControl.setOnClickListener {
                                controls.buttonClickedCallback(getItem(holder.absoluteAdapterPosition))

                            }

                        }
                    }


                }
            }
            else -> {
                Log.e(TAG, "onBindViewHolder: Invalid ViewHolder ${holder.javaClass.name}", )
            }
        }
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

    inner class PosterViewHolder(val bindings: ViewPosterItemBinding) :
        RecyclerView.ViewHolder(bindings.root)

    inner class CardViewHolder(val bindings: ViewCardItemBinding) :
        RecyclerView.ViewHolder(bindings.root)

    companion object {
        const val VIEW_TYPE_POSTER = 0
        const val VIEW_TYPE_CARD = 1
        const val ACTION_NAVIGATE_SHOW = 0
    }
}