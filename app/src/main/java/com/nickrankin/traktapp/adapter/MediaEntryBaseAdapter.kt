package com.nickrankin.traktapp.adapter

import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.MenuRes
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.*
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.shows.ICON_MARGIN
import com.nickrankin.traktapp.databinding.ViewCardItemBinding
import com.nickrankin.traktapp.databinding.ViewPosterItemBinding
import com.nickrankin.traktapp.helper.TmdbImageLoader

private const val TAG = "MediaEntryBaseAdapter"
abstract class MediaEntryBaseAdapter<T> constructor(private val controls: AdaptorActionControls<T>?, diffCallback: DiffUtil.ItemCallback<T>): ListAdapter<T, RecyclerView.ViewHolder>(diffCallback) {

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
        val item = getItem(holder.absoluteAdapterPosition)
        when(holder) {
            is MediaEntryBaseAdapter<*>.PosterViewHolder -> {
                holder.bindings.apply {
                    if(controls != null) {

                        holder.itemView.setOnClickListener {
                            controls.entrySelectedCallback(item)
                        }

                        if (controls.menuResource != null) {
//                            itemMenu.visibility = View.VISIBLE

                            root.setOnLongClickListener {
                                showMenu(item, root, itemPoster, null)

                                true
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

                            root.setOnLongClickListener {
                                showMenu(item, root, itemPoster, itemBackdropImageview)

                                true
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

    private fun showMenu(selectedItem: T, v: View, posterImageView: ImageView, backdropImageView: ImageView?) {
        if(controls?.menuResource == null) {
            return
        }

        val popup = PopupMenu(v.context!!, v)

        // Inflate both BaseMenu resource items and the provided menu resource
        popup.menuInflater.inflate(controls.menuResource, popup.menu)
        popup.menuInflater.inflate(R.menu.base_popup_menu, popup.menu)

        if(popup.menu is MenuBuilder) {
            val menuBuilder = popup.menu as MenuBuilder
            menuBuilder.setOptionalIconsVisible(true)

            for(item in menuBuilder.visibleItems) {
                val iconMarginPx =
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, ICON_MARGIN.toFloat(), v.context!!.resources.displayMetrics)
                        .toInt()
                if (item.icon != null) {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                        item.icon = InsetDrawable(item.icon, iconMarginPx, 0, iconMarginPx,0)
                    } else {
                        item.icon =
                            object : InsetDrawable(item.icon, iconMarginPx, 0, iconMarginPx, 0) {
                                override fun getIntrinsicWidth(): Int {
                                    return intrinsicHeight + iconMarginPx + iconMarginPx
                                }
                            }
                    }
                }
            }
        }

        popup.setOnMenuItemClickListener { menuItem ->
            
            if(menuItem.itemId == R.id.popupmenu_refresh_image) {
                reloadImages(selectedItem, posterImageView, backdropImageView)
            } else {
                // Respond to menu item click.
                controls.menuItemSelectedCallback(selectedItem, menuItem.itemId)
            }
            true
        }

        popup.setOnDismissListener { popupMenu ->
            popupMenu.dismiss()
            // Respond to popup being dismissed.
        }
        // Show the popup menu.
        popup.show()
    }

    abstract fun reloadImages(selectedItem: T, posterImageView: ImageView, backdropImageView: ImageView?)

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