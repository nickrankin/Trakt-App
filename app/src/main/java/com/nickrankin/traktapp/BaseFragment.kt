package com.nickrankin.traktapp

import android.util.Log
import androidx.fragment.app.Fragment
import com.nickrankin.traktapp.helper.OnTitleChangeListener
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "BaseFragment"
@AndroidEntryPoint
abstract class BaseFragment: Fragment() {
    fun updateTitle(newTitle: String) {
        try {
            val onTitleChangeListener = activity as OnTitleChangeListener
            onTitleChangeListener.onTitleChanged(newTitle)
        } catch (cce: ClassCastException) {
            Log.e(TAG, "onViewCreated: Activity class ${activity?.javaClass?.name} must implement OnTitleChangeListener!!")
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }
}