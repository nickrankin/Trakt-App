package com.nickrankin.traktapp.ui.shows

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.OnNavigateToEntity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.shows.ShowProgressAdapter
import com.nickrankin.traktapp.databinding.FragmentSplitviewLayoutBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.shows.ShowsProgressViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "ShowsProgressFragment"
@AndroidEntryPoint
class ShowsProgressFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {

    private var _bindings: FragmentSplitviewLayoutBinding? = null
    private val bindings get() = _bindings!!

    private val viewModel: ShowsProgressViewModel by activityViewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var showProgressAdapter: ShowProgressAdapter

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _bindings = FragmentSplitviewLayoutBinding.inflate(inflater)
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateTitle("Show Progress")

        (activity as OnNavigateToEntity).enableOverviewLayout(false)

        initRecycler()
        getShowProgress()
    }

    private fun getShowProgress() {
        lifecycleScope.launchWhenStarted {
            viewModel.showSeasonProgress.collectLatest { showProgressResource ->
                when(showProgressResource) {
                    is Resource.Loading -> {
                        bindings.splitviewlayoutProgressbar.visibility = View.VISIBLE
                        toggleMessageBanner(bindings, null, false)
                    }
                    is Resource.Success -> {
                        bindings.splitviewlayoutProgressbar.visibility = View.GONE
                        toggleMessageBanner(bindings, null, false)

                        val stats = showProgressResource.data

                        if(stats?.isNotEmpty() == true) {
                            toggleMessageBanner(bindings, null, false)
                            showProgressAdapter.submitList(stats)
                        } else {
                            toggleMessageBanner(bindings, getString(R.string.none_watched), true)
                        }
                    }
                    is Resource.Error -> {
                        bindings.splitviewlayoutProgressbar.visibility = View.GONE

                        val stats = showProgressResource.data

                        if(stats?.isNotEmpty() == true) {
                            toggleMessageBanner(bindings, null, false)
                            showProgressAdapter.submitList(stats)
                        } else {
                            toggleMessageBanner(bindings, getString(R.string.none_watched), true)
                        }

                        showErrorSnackbarRetryButton(showProgressResource.error, bindings.root) {
                            onRefresh()
                        }
                    }

                }
            }
        }
    }

    private fun initRecycler() {
        recyclerView = bindings.splitviewlayoutRecyclerview
        val lm = LinearLayoutManager(requireContext())

        showProgressAdapter = ShowProgressAdapter(tmdbImageLoader, sharedPreferences) { showAndSasonProgress ->
            (activity as OnNavigateToEntity).navigateToShow(
                ShowDataModel(
                    showAndSasonProgress.showProgress.show_trakt_id,
                    showAndSasonProgress.showProgress.show_tmdb_id,
                    showAndSasonProgress.showProgress.title
                )
            )
        }

        recyclerView.layoutManager = lm
        recyclerView.adapter = showProgressAdapter
    }

    override fun onStart() {
        super.onStart()

        viewModel.onStart()
    }

    override fun onRefresh() {
        viewModel.onRefresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        recyclerView.adapter = null


        _bindings = null
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            ShowsProgressFragment()
    }
}