package com.nickrankin.traktapp.ui.shows

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.databinding.TabLayoutShowsBinding
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "ShowTabsFragment"
@AndroidEntryPoint
class ShowTabsFragment: BaseFragment(), TabLayout.OnTabSelectedListener {
    private var _bindings: TabLayoutShowsBinding? = null
    private val bindings get() = _bindings!!

    private lateinit var navTabs: TabLayout

    private val onTabPosChanged: MutableLiveData<Int> = MutableLiveData()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _bindings = TabLayoutShowsBinding.inflate(inflater)
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navTabs = bindings.showsmainactivityNavigationTabs
        navTabs.addOnTabSelectedListener(this)

        onTabChanged()
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        val showsMainActivity = activity as ShowsMainActivity

        when (tab?.position) {
            0 -> {
                showsMainActivity.navigateToFragment(ShowsMainActivity.PROGRESS_SHOWS_TAG, true)
            }
            1 -> {
                showsMainActivity.navigateToFragment(ShowsMainActivity.UPCOMING_SHOWS_TAG, true)
            }
            2 -> {

                showsMainActivity.navigateToFragment(ShowsMainActivity.WATCHED_SHOWS_TAG, true)
            }
            3 -> {
                showsMainActivity.navigateToFragment(ShowsMainActivity.TRACKING_SHOWS_TAG, true)
            }
            4 -> {

                showsMainActivity.navigateToFragment(ShowsMainActivity.COLLECTED_SHOWS_TAG, true)
            }
            5 -> {

                showsMainActivity.navigateToFragment(ShowsMainActivity.SUGGESTED_SHOWS_TAG, true)

            }
            else -> {
                Log.e(TAG, "onTabSelected: ELSE")
            }
        }
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {
//        try {
//            Log.d(TAG, "onTabReselected: Current Fragment Tag $currentFragmentTag")
//            (supportFragmentManager.findFragmentByTag(currentFragmentTag) as SwipeRefreshLayout.OnRefreshListener).let { refreshFragment ->
//                Log.d(TAG, "onTabReselected: Refreshing Fragment $currentFragmentTag")
//                refreshFragment.onRefresh()
//            }
//        } catch (e: ClassCastException) {
//            Log.e(
//                TAG,
//                "onTabReselected: Cannot Cast ${
//                    supportFragmentManager.findFragmentByTag(
//                        currentFragmentTag
//                    )?.javaClass?.name
//                } as SwipeRefreshLayout.OnRefreshListener",
//            )
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
        when (tab?.position) {
            0 -> {
            }
            1 -> {
            }
            2 -> {
            }
            3 -> {
            }
            4 -> {
            }
            else -> {
            }
        }
    }

    private fun onTabChanged() {
        onTabPosChanged.observe(viewLifecycleOwner) { newPos ->
            navTabs.selectTab(navTabs.getTabAt(newPos))

        }
    }

    fun selectTab(tabPos: Int) {
        onTabPosChanged.value = tabPos

    }

    companion object {
        @JvmStatic
        fun newInstance() = ShowTabsFragment()
    }
}