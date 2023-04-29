package com.nickrankin.traktapp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentManager
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.OnTitleChangeListener
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : SplitViewActivity(), OnTitleChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navigateToFragment(MAIN_ACTIVITY_TAG)

        onTitleChanged(AppConstants.APP_TITLE)
    }

    override fun onTitleChanged(newTitle: String) {
        supportActionBar?.title = newTitle
    }

    companion object {
        const val MAIN_ACTIVITY_TAG = "main_act"
    }
}