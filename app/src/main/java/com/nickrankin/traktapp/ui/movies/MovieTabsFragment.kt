package com.nickrankin.traktapp.ui.movies

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.databinding.TabLayoutMoviesBinding
import com.nickrankin.traktapp.databinding.TabLayoutShowsBinding
import com.nickrankin.traktapp.ui.movies.collected.CollectedMoviesFragment
import com.nickrankin.traktapp.ui.movies.watched.WatchedMoviesFragment
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "MovieTabsFragment"
@AndroidEntryPoint
class MovieTabsFragment: BaseFragment(), TabLayout.OnTabSelectedListener {
    private var _bindings: TabLayoutMoviesBinding? = null
    private val bindings get() = _bindings!!

    private lateinit var navTabs: TabLayout

    private val onTabPosChanged: MutableLiveData<Int> = MutableLiveData()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _bindings = TabLayoutMoviesBinding.inflate(inflater)
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navTabs = bindings.splitviewmoviesactivityNavigationTabs
        navTabs.addOnTabSelectedListener(this)

        onTabChanged()
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        val moviesMainActivity = activity as MoviesMainActivity

        when (tab?.position) {
            0 -> {
                moviesMainActivity.navigateToFragment(MoviesMainActivity.TAG_COLLECTED_MOVIES)
            }
            1 -> {
                moviesMainActivity.navigateToFragment(MoviesMainActivity.TAG_WATCHED_MOVIES)
            }
            2 -> {

                moviesMainActivity.navigateToFragment(MoviesMainActivity.TAG_SUGGESTED_MOVIES)
            }
            3 -> {
                moviesMainActivity.navigateToFragment(MoviesMainActivity.TAG_TRENDING_MOVIES)
            }
            else -> {
                Log.e(TAG, "onTabSelected: Tab position invalid: ${tab?.position}")
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
        fun newInstance() = MovieTabsFragment()
    }
}