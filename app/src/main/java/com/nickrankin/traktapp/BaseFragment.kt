package com.nickrankin.traktapp

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.nickrankin.traktapp.databinding.FragmentSplitviewLayoutBinding
import com.nickrankin.traktapp.helper.IHandleError
import com.nickrankin.traktapp.helper.OnTitleChangeListener
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.auth.NotLoggedInFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "BaseFragment"
@AndroidEntryPoint
abstract class BaseFragment: Fragment(), IHandleError {

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    val isLoggedIn get() = getIsLoggedIn()

    private fun getIsLoggedIn(): Boolean {
        return try {
            (activity as BaseActivity).isLoggedIn
        } catch(e: Exception) {
            Log.e(TAG, "getIsLoggedIn: Activity ${activity?.javaClass?.name} should Extend BaseActivity", )
            false
        }
    }

    open fun updateTitle(newTitle: String) {
        try {
            val onTitleChangeListener = activity as OnTitleChangeListener
            onTitleChangeListener.onTitleChanged(newTitle)
        } catch (cce: ClassCastException) {
            Log.e(TAG, "onViewCreated: Activity class ${activity?.javaClass?.name} must implement OnTitleChangeListener!!")
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    override fun showErrorSnackbarRetryButton(
        throwable: Throwable?,
        view: View,
        retryCallback: () -> Unit
    ) {
        try {
            (activity as IHandleError).showErrorSnackbarRetryButton(throwable, view, retryCallback)
        }
        catch(classCastException: ClassCastException) {
            classCastException.printStackTrace()
        }
        catch(e: Exception) {
            e.printStackTrace()
        }
    }

    override fun handleError(throwable: Throwable?, customMessage: String?) {
        try {
            (activity as IHandleError).handleError(throwable, customMessage)
        }
        catch(classCastException: ClassCastException) {
            classCastException.printStackTrace()
        }
        catch(e: Exception) {
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

    protected fun toggleMessageBanner(bindings: FragmentSplitviewLayoutBinding, message: String?, isEnabled: Boolean) {
        if(isEnabled) {
            bindings.splitviewlayoutMessageContainer.visibility = View.VISIBLE
            bindings.splitviewlayoutRecyclerview.visibility = View.GONE
            bindings.splitviewlayoutMessageContainer.text = message
        } else {
            bindings.splitviewlayoutMessageContainer.visibility = View.GONE
            bindings.splitviewlayoutRecyclerview.visibility = View.VISIBLE
        }
    }
}