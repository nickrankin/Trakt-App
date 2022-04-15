package com.nickrankin.traktapp.ui.movies

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.google.android.material.snackbar.Snackbar
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.movies.ReccomendedMoviesAdaptor
import com.nickrankin.traktapp.databinding.FragmentRecommendedMoviesBinding
import com.nickrankin.traktapp.helper.ItemDecorator
import com.nickrankin.traktapp.helper.PosterImageLoader
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.movies.RecommendedMoviesViewModel
import com.nickrankin.traktapp.repo.movies.MovieDetailsRepository
import com.uwetrottmann.trakt5.entities.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import retrofit2.HttpException
import javax.inject.Inject

private const val TAG = "RecommendedMoviesFragme"
@AndroidEntryPoint
class RecommendedMoviesFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var bindings: FragmentRecommendedMoviesBinding

    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReccomendedMoviesAdaptor

    private val viewModel: RecommendedMoviesViewModel by activityViewModels()

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var posterLoader: PosterImageLoader

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = FragmentRecommendedMoviesBinding.inflate(inflater)
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeLayout = bindings.recommendedmoviesfragmentSwipeLayout
        swipeLayout.setOnRefreshListener(this)

        progressBar = bindings.recommendedmoviesfragmentProgressbar

        initRecyclerView()

        getRecommendedMovies()

        getEvents()
    }
    
    private fun getRecommendedMovies() {
        lifecycleScope.launchWhenStarted { 
            viewModel.recommendedMovies.collectLatest { recommendedMoviesResource ->
                when(recommendedMoviesResource) {
                    is Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE

                        bindings.recommendedmoviesfragmentErrorText.visibility = View.GONE
                        bindings.recommendedmoviesfragmentRetryButton.visibility = View.GONE

                        recyclerView.visibility = View.VISIBLE


                        Log.d(TAG, "getRecommendedMovies: Loading recommendations")
                    }
                    
                    is Resource.Success -> {
                        progressBar.visibility = View.GONE

                        if(swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }
                        Log.d(TAG, "getRecommendedMovies: Got recommendations successfully")

                        adapter.submitList(recommendedMoviesResource.data)
                    }
                    
                    is Resource.Error -> {
                        progressBar.visibility = View.GONE

                        if(swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }

                        bindings.recommendedmoviesfragmentErrorText.visibility = View.VISIBLE
                        bindings.recommendedmoviesfragmentRetryButton.visibility = View.VISIBLE

                        recyclerView.visibility = View.GONE

                        bindings.recommendedmoviesfragmentErrorText.text = "Error loading recommended Movies. ${recommendedMoviesResource.error?.message}"

                        bindings.recommendedmoviesfragmentRetryButton.setOnClickListener { onRefresh() }

                        Log.e(TAG, "getRecommendedMovies: Error loading recommendations. ${recommendedMoviesResource.error?.message}", )
                    }
                }
            }
        }
    }

    private fun getEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.event.collectLatest { event ->
                when(event) {
                    is RecommendedMoviesViewModel.Event.RemoveRecommendationEvent -> {
                        val response = event.response

                        when(response) {
                            is Resource.Loading -> {

                            }
                            is Resource.Success -> {
                                displayToast("Successfully removed recommended movie", Toast.LENGTH_SHORT)
                            }
                            is Resource.Error -> {
                                val throwable = response.error

                                throwable?.printStackTrace()

                                if(throwable is HttpException) {
                                    displayToast("HTTP Error occurred removing recommendation (HTTP Code (${throwable.code()})", Toast.LENGTH_LONG)
                                } else {
                                    displayToast("An exception was raised trying to remove recommendation, ${throwable?.message}", Toast.LENGTH_LONG)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initRecyclerView() {
        recyclerView = bindings.recommendedmoviesfragmentRecyclerview

        val lm = LinearLayoutManager(requireContext())

        adapter = ReccomendedMoviesAdaptor(glide, posterLoader, callback = {movie ->
            val intent = Intent(requireContext(), MovieDetailsActivity::class.java)
            intent.putExtra(MovieDetailsRepository.MOVIE_TRAKT_ID_KEY, movie?.ids?.trakt)
            intent.putExtra(MovieDetailsRepository.MOVIE_TITLE_KEY, movie?.title)

            startActivity(intent)
        })

        recyclerView.layoutManager = lm
        recyclerView.adapter = adapter

        setupViewSwipeBehaviour()

    }

    private fun setupViewSwipeBehaviour() {

        var itemTouchHelper: ItemTouchHelper? = null

        itemTouchHelper = ItemTouchHelper(
            object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    viewHolder.itemView.background = null

                    return true
                }

                override fun onChildDraw(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    val colorAlert = ContextCompat.getColor(requireContext(), R.color.red)
                    val teal200 = ContextCompat.getColor(requireContext(), R.color.teal_200)
                    val defaultWhiteColor = ContextCompat.getColor(requireContext(), R.color.white)

                    ItemDecorator.Builder(c, recyclerView, viewHolder, dX, actionState).set(
                        iconHorizontalMargin = 23f,
                        backgroundColorFromStartToEnd = teal200,
                        backgroundColorFromEndToStart = colorAlert,
                        textFromStartToEnd = "Add to Collection",
                        textFromEndToStart = "Remove from Suggested",
                        textColorFromStartToEnd = defaultWhiteColor,
                        textColorFromEndToStart = defaultWhiteColor,
                        iconTintColorFromStartToEnd = defaultWhiteColor,
                        iconTintColorFromEndToStart = defaultWhiteColor,
                        textSizeFromStartToEnd = 16f,
                        textSizeFromEndToStart = 16f,
                        typeFaceFromStartToEnd = Typeface.DEFAULT_BOLD,
                        typeFaceFromEndToStart = Typeface.SANS_SERIF,
                        iconResIdFromStartToEnd = R.drawable.ic_baseline_delete_forever_24,
                        iconResIdFromEndToStart = R.drawable.ic_trakt_svgrepo_com
                    )

                    super.onChildDraw(
                        c,
                        recyclerView,
                        viewHolder,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )

                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val movieList: MutableList<Movie> = mutableListOf()
                    movieList.addAll(adapter.currentList)

                    val showPosition = viewHolder.layoutPosition
                    val movie = movieList[showPosition]

                    when (direction) {
                        ItemTouchHelper.LEFT -> {
                            val updatedList: MutableList<Movie> = mutableListOf()
                            updatedList.addAll(movieList)
                            updatedList.remove(movie)

                            adapter.submitList(updatedList)

                            val timer = getTimer() {
                                Log.e(TAG, "onFinish: Timer ended for remove show ${movie.title}!")
                                viewModel.removeRecommendedMovie(movie.ids?.trakt ?: -1)

                            }.start()

                            getSnackbar(
                                recyclerView,
                                "You have removed suggestion: ${movie.title}"
                            ) {
                                timer.cancel()
                                adapter.submitList(movieList) {
                                    // For first and last element, always scroll to the position to bring the element to focus
                                    if (showPosition == 0) {
                                        recyclerView.scrollToPosition(0)
                                    } else if (showPosition == movieList.size - 1) {
                                        recyclerView.scrollToPosition(movieList.size - 1)
                                    }
                                }
                            }.show()
                        }

                        ItemTouchHelper.RIGHT -> {
                            val timer = getTimer() {
                                Log.e(TAG, "onFinish: Timer ended for Collect Show ${movie.title}!")
                                val syncItems = SyncItems()
                                syncItems.apply {
                                    shows = listOf(
                                        SyncShow()
                                            .id(ShowIds.trakt(movie.ids?.trakt ?: 0))
                                    )
                                }
                                //viewModel.addToCollection(syncItems)
                            }.start()

                            getSnackbar(
                                recyclerView,
                                "You have added ${movie.title} to your collection."
                            ) {
                                timer.cancel()
                            }.show()

                            // Force the current show to "bounce back" into view
                            adapter.notifyItemChanged(showPosition)
                        }

                    }
                }
            }
        )

        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun getTimer(doAction: () -> Unit): CountDownTimer {
        return object : CountDownTimer(5000, 1000) {
            override fun onTick(p0: Long) {
            }

            override fun onFinish() {
                doAction()
            }
        }
    }

    private fun getSnackbar(v: View, message: String, listener: View.OnClickListener): Snackbar {
        return Snackbar.make(
            v,
            message,
            Snackbar.LENGTH_LONG
        )
            .setAction("Cancel", listener)
    }


    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {
        viewModel.onRefresh()
    }

    private fun displayToast(message: String, length: Int) {
        Toast.makeText(requireContext(), message, length).show()
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            RecommendedMoviesFragment()
    }
}