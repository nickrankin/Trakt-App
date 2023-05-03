package com.nickrankin.traktapp.adapter

import android.graphics.drawable.Drawable
import androidx.annotation.MenuRes

class AdaptorActionControls<T>(val buttonIconResource: Drawable?, val buttonText: String?, val showMenuOnCardView: Boolean, @MenuRes val menuResource: Int?, val entrySelectedCallback: (selectedItem: T) -> Unit, val buttonClickedCallback: (selectedItem: T) -> Unit, val menuItemSelectedCallback: (selectedItem: T, menuSelected: Int) -> Unit)