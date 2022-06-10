package com.nickrankin.traktapp

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.nickrankin.traktapp.helper.OnTitleChangeListener
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.auth.NotLoggedInFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "BaseFragment"
@AndroidEntryPoint
abstract class BaseFragment: Fragment() {

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    protected var isLoggedIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isLoggedIn = sharedPreferences.getBoolean(AuthActivity.IS_LOGGED_IN, false)
    }

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

    protected fun handleLoggedOutState(currentFragmentId: Int) {
        val fm = activity?.supportFragmentManager!!
        fm.beginTransaction()
            .replace(currentFragmentId, NotLoggedInFragment.newInstance())
            .commit()

        fm.popBackStack()
    }
}