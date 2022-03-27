package com.nickrankin.traktapp.ui.shows.episodedetails

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.credits.ShowCastCreditsAdapter
import com.nickrankin.traktapp.adapter.history.EpisodeWatchedHistoryItemAdapter
import com.nickrankin.traktapp.dao.credits.ShowCastPerson
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.databinding.ActivityEpisodeDetailsBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.ItemDecorator
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.shows.episodedetails.EpisodeDetailsViewModel
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.services.helper.TrackedEpisodeAlarmScheduler
import com.nickrankin.traktapp.services.helper.TrackedEpisodeNotificationsBuilder
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.shows.OnNavigateToShow
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import com.uwetrottmann.tmdb2.entities.CastMember
import com.uwetrottmann.trakt5.entities.SyncItems
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import org.apache.commons.lang3.time.DateFormatUtils
import org.threeten.bp.format.DateTimeFormatter
import retrofit2.HttpException
import javax.inject.Inject

private const val TAG = "EpisodeDetailsActivity"

private const val FRAGMENT_ACTION_BUTTONS ="action_buttons_fragment"
@AndroidEntryPoint
class EpisodeDetailsActivity : AppCompatActivity(), OnNavigateToShow, SwipeRefreshLayout.OnRefreshListener {
    private lateinit var bindings: ActivityEpisodeDetailsBinding
    private val viewModel: EpisodeDetailsViewModel by viewModels()

    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var castRecyclerView: RecyclerView
    private lateinit var showCastAdapter: ShowCastCreditsAdapter

    private lateinit var watchedEpisodesRecyclerView: RecyclerView
    private lateinit var watchedEpisodesAdapter: EpisodeWatchedHistoryItemAdapter

    private var isLoggedIn = false

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var trackedEpisodesAlarmScheduler: TrackedEpisodeAlarmScheduler

    @Inject
    lateinit var glide: RequestManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = ActivityEpisodeDetailsBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        setSupportActionBar(bindings.episodedetailsactivityToolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if(savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .add(bindings.episodedetailsactivityInner.episodedetailsactivityActionButtonsFragmentContainer.id, EpisodeDetailsActionButtonsFragment.newInstance(), FRAGMENT_ACTION_BUTTONS)
                    .commit()
        }

        progressBar = bindings.episodedetailsactivityInner.episodedetailsactivityProgressbar
        swipeRefreshLayout = bindings.episodedetailsactivitySwipeLayout
        swipeRefreshLayout.setOnRefreshListener(this)

        isLoggedIn = sharedPreferences.getBoolean(AuthActivity.IS_LOGGED_IN, false)

        if(intent.hasExtra(TrackedEpisodeNotificationsBuilder.FROM_NOTIFICATION_TAP) && intent.extras?.getBoolean(TrackedEpisodeNotificationsBuilder.FROM_NOTIFICATION_TAP, false) == true) {
            dismissEipsodeNotifications()
        }

        initCastRecycler()
        initDeleteWatchedEpisodeRecycler()

        getShow()
        getEpisode()
        getCast()
        getEvents()

        if(isLoggedIn) {
            getWatchedEpisodes()
        }

    }

    private fun dismissEipsodeNotifications() {
        val episodeTraktId = intent.extras?.getInt(TrackedEpisodeNotificationsBuilder.EPISODE_TRAKT_ID)
        Log.d(TAG, "dismissEipsodeNotifications: Dismissing notifications for episode ${episodeTraktId}", )

        lifecycleScope.launchWhenStarted {
            trackedEpisodesAlarmScheduler.dismissNotification(episodeTraktId ?: 0, true)
        }
    }

    private fun getShow() {
        lifecycleScope.launchWhenStarted {
            viewModel.show.collectLatest { showResource ->
                val show = showResource.data
                when (showResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "collectShow: Loading show..")
                    }
                    is Resource.Success -> {

                        Log.d(TAG, "collectShow: Got show! ${showResource.data}")

                        displayShow(show)
                    }
                    is Resource.Error -> {

                        // Try display cached show if available
                        if(show != null) {
                            displayShow(show)
                        }

                        Log.e(
                            TAG,
                            "collectShow: Error getting show. ${showResource.error?.localizedMessage}"
                        )
                        showResource.error?.printStackTrace()
                    }
                }
            }
        }

    }

    private fun getEpisode() {
        lifecycleScope.launchWhenStarted {
            // Contains error text and retry button
            val errorWidgets = bindings.episodedetailsactivityInner.episodedetailsactivityErrorWidgets
            // Contains all UI elements for the Episode Details View
            val mainGroup = bindings.episodedetailsactivityInner.episodedetailsactivityMainGroup
            mainGroup.visibility = View.VISIBLE

            viewModel.episode.collectLatest { episodeResource ->
                val episode = episodeResource.data

                when (episodeResource) {
                    is Resource.Loading -> {
                        toggleProgressBar(true)

                        errorWidgets.visibility = View.GONE
                        mainGroup.visibility = View.GONE
                        Log.d(TAG, "collectEpisode: Loading Episode...")
                    }
                    is Resource.Success -> {
                        toggleProgressBar(false)

                        mainGroup.visibility = View.VISIBLE
                        errorWidgets.visibility = View.GONE

                        bindings.wpisodedetailsactivityCollapsingToolbarLayout.title = episode?.name ?: "Unknown"

                        displayEpisode(episode)

                        setupActionBarFragment(episode!!)
                    }
                    is Resource.Error -> {
                        toggleProgressBar(false)

                        displayMessageToast("Error getting episode. ${episodeResource.error?.localizedMessage}", Toast.LENGTH_LONG)

                        // Try to display cached episode if available
                        if(episodeResource.data != null) {
                            errorWidgets.visibility = View.GONE
                            mainGroup.visibility = View.VISIBLE

                            displayEpisode(episode)

                            setupActionBarFragment(episode!!)

                        } else {
                            // No cached episode, need to show error message
                            errorWidgets.visibility = View.VISIBLE
                            mainGroup.visibility = View.GONE

                            handleError(episodeResource.error)
                        }

                        episodeResource.error?.printStackTrace()
                    }
                }
            }
        }
    }

    private fun handleError(t: Throwable?) {
        val errorText = bindings.episodedetailsactivityInner.episodedetailsactivityError
        val retryButton = bindings.episodedetailsactivityInner.episodedetailsactivityRetryButton

        if(t != null) {
            if(t is HttpException) {
                errorText.text = "There was an error loading the Episode details. Please check your internet connection. Error code: (HTTP Status: ${t.code()}) "
            } else {
                errorText.text = "There was an error loading the Episode details. Please check your internet connection. Error: (${t.localizedMessage}) "
            }
        }

        retryButton.setOnClickListener {
            viewModel.onStart()
        }
    }

    private fun toggleProgressBar(isVisible: Boolean) {
            if(swipeRefreshLayout.isRefreshing) {
                swipeRefreshLayout.isRefreshing = false
            }

            if(isVisible) progressBar.visibility = View.VISIBLE else progressBar.visibility = View.GONE
    }

    private fun getWatchedEpisodes() {
        lifecycleScope.launchWhenStarted {
            viewModel.watchedEpisodes.collectLatest { watchedEpisodes ->
                        displayWatchedEpisodes(watchedEpisodes ?: emptyList())
            }
        }
    }

    private fun getEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->
                when(event) {
                    is EpisodeDetailsViewModel.Event.DeleteWatchedHistoryItem -> {
                        val eventResource = event.syncResponse

                        if(eventResource is Resource.Success) {
                            val syncResponse = eventResource.data

                            if(syncResponse?.deleted?.episodes ?: 0 > 0) {
                                displayMessageToast("Successfully removed play", Toast.LENGTH_SHORT)
                            } else {
                                displayMessageToast("Didn't remove play", Toast.LENGTH_SHORT)
                            }

                        } else if (eventResource is Resource.Error) {
                            displayMessageToast("Error deleting history item! ${eventResource.error?.localizedMessage}", Toast.LENGTH_LONG)
                            eventResource.error?.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun getCast() {
       setupCastSwitcher()

        lifecycleScope.launchWhenStarted {
            viewModel.cast.collectLatest { castResource ->
                when (castResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getCast: Loading Cast People")
                    }
                    is Resource.Success -> {
                        displayCast(castResource.data ?: emptyList())
                    }
                    is Resource.Error -> {
                        bindings.episodedetailsactivityInner.showdetailsactivityCastGroup.visibility = View.GONE
                        Log.e(
                            TAG,
                            "getCast: Error getting Cast People. ${castResource.error?.localizedMessage}",
                        )
                        castResource.error?.printStackTrace()
                    }
                }
            }
        }
    }

    private fun initCastRecycler() {
        castRecyclerView = bindings.episodedetailsactivityInner.episodedetailsactivityCastRecycler

        val layoutManager = FlexboxLayoutManager(this)
        layoutManager.flexDirection = FlexDirection.ROW
        layoutManager.flexWrap = FlexWrap.NOWRAP

        showCastAdapter = ShowCastCreditsAdapter(glide)

        castRecyclerView.layoutManager = layoutManager
        castRecyclerView.adapter = showCastAdapter
    }

    private fun setupCastSwitcher() {

        bindings.episodedetailsactivityInner.apply {
            val regularCastButton = episodedetailsactivityCastRegularButton
            val guestStarsButton = episodedetailsactivityCastGuestButton

            regularCastButton.text = "Season Regulars"
            guestStarsButton.text = "Guest Stars"

            regularCastButton.setOnClickListener {
                regularCastButton.setTypeface(null, Typeface.BOLD)
                guestStarsButton.setTypeface(null, Typeface.NORMAL)

                viewModel.filterCast(false)
            }

            guestStarsButton.setOnClickListener {
                regularCastButton.setTypeface(null, Typeface.NORMAL)
                guestStarsButton.setTypeface(null, Typeface.BOLD)

                viewModel.filterCast(true)
            }
        }
    }

    private fun displayCast(castPersons: List<ShowCastPerson>) {

        if (castPersons.isNotEmpty()) {
            bindings.episodedetailsactivityInner.showdetailsactivityCastGroup.visibility = View.VISIBLE

            showCastAdapter.updateCredits(castPersons)
        } else {
            showCastAdapter.updateCredits(emptyList())
        }
    }

    private fun displayWatchedEpisodes(episodes: List<WatchedEpisode>) {
        if(episodes.isNotEmpty()) {
            bindings.episodedetailsactivityInner.showdetailsactivityWatchedEpisodesGroup.visibility = View.VISIBLE

            bindings.episodedetailsactivityInner.episodedetailsactivityTotalPlays.text = "Total plays: ${episodes.size} (Last play: ${
                    episodes.first().watched_at?.format(DateTimeFormatter.ofPattern(sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_TIME_FORMAT)))
            })"

            watchedEpisodesAdapter.submitList(episodes)
        } else {
            bindings.episodedetailsactivityInner.showdetailsactivityWatchedEpisodesGroup.visibility = View.GONE
        }
    }

    private fun setupActionBarFragment(episode: TmEpisode) {
        try {
            val fragment = supportFragmentManager.findFragmentByTag(
                FRAGMENT_ACTION_BUTTONS) as OnEpisodeChangeListener

            fragment.bindEpisode(episode!!)
        } catch(e: ClassCastException) {
            Log.e(TAG, "getShow: Cannot cast ${supportFragmentManager.findFragmentByTag(
                FRAGMENT_ACTION_BUTTONS)} as OnEpisodeIdChangeListener", )
            e.printStackTrace()
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    private fun displayShow(show: TmShow?) {
        bindings.episodedetailsactivityInner.apply {
            episodedetailsactivityShowTitle.text = show?.name ?: "Unknown"

            if(show?.poster_path?.isNotEmpty() == true) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + show.poster_path)
                    .into(episodedetailsactivityShowPoster)
            } else {
                episodedetailsactivityShowPoster.visibility = View.GONE
            }

            episodedetailsactivityShowTitle.setOnClickListener {
                navigateToShow(show?.trakt_id ?: 0, show?.tmdb_id ?: 0, show?.name,show?.languages?.first() ?: "en")
            }

            episodedetailsactivityShowPoster.setOnClickListener {
                navigateToShow(show?.trakt_id ?: 0, show?.tmdb_id ?: 0, show?.name,show?.languages?.first() ?: "en")
            }
        }
    }

    private fun displayEpisode(episode: TmEpisode?) {
        if (episode?.still_path?.isNotEmpty() == true) {
            glide
                .load(AppConstants.TMDB_POSTER_URL + episode.still_path)
                .into(bindings.episodedetailsactivityBackdrop)
        }

        bindings.episodedetailsactivityInner.apply {
            episodedetailsactivityTitle.text = episode?.name
            episodedetailsactivityOverview.text = episode?.overview

            if (episode?.air_date != null) {
                bindings.episodedetailsactivityInner.episodedetailsactivityFirstAired.visibility = View.VISIBLE

                episodedetailsactivityFirstAired.text = "Aired: " + DateFormatUtils.format(
                    episode.air_date,
                    sharedPreferences.getString(
                        "date_format",
                        AppConstants.DEFAULT_DATE_TIME_FORMAT
                    )
                )
            }
        }
    }

    override fun navigateToShow(traktId: Int, tmdbId: Int, showTitle: String?, language: String?) {
        if(tmdbId == 0) {
            Toast.makeText(this, "Trakt does not have this show's TMDB", Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(this, ShowDetailsActivity::class.java)
        intent.putExtra(ShowDetailsRepository.SHOW_TRAKT_ID_KEY, traktId)
        intent.putExtra(ShowDetailsRepository.SHOW_TMDB_ID_KEY, tmdbId)
        intent.putExtra(ShowDetailsRepository.SHOW_TITLE_KEY, showTitle)
        intent.putExtra(ShowDetailsRepository.SHOW_LANGUAGE_KEY, language)

        startActivity(intent)
    }

    private fun handleWatchedEpisodeDelete(watchedEpisode: WatchedEpisode, showConfirmation: Boolean) {
        val syncItem = SyncItems().ids(watchedEpisode.id)

        if(!showConfirmation) {
            viewModel.removeWatchedHistoryItem(syncItem)
            return
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Delete ${watchedEpisode.episode_title} from your watched history?")
            .setMessage("Are you sure you want to remove ${watchedEpisode.episode_title} from your Trakt History?")
            .setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->
                viewModel.removeWatchedHistoryItem(syncItem)
            })
            .setNegativeButton("No", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            .create()

        dialog.show()
    }

    private fun setupViewSwipeBehaviour(context: Context) {

        var itemTouchHelper: ItemTouchHelper? = null

        itemTouchHelper = ItemTouchHelper(
            object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
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
                    val colorAlert = ContextCompat.getColor(context, R.color.red)
                    val teal200 = ContextCompat.getColor(context, R.color.teal_200)
                    val defaultWhiteColor = ContextCompat.getColor(context, R.color.white)

                    ItemDecorator.Builder(c, recyclerView, viewHolder, dX, actionState).set(
                        iconHorizontalMargin = 23f,
                        backgroundColorFromStartToEnd = teal200,
                        backgroundColorFromEndToStart = colorAlert,
                        textFromStartToEnd = "",
                        textFromEndToStart = "Remove from History",
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
                    val episodesList: MutableList<WatchedEpisode> = mutableListOf()
                    episodesList.addAll(watchedEpisodesAdapter.currentList)

                    val episodePosition = viewHolder.layoutPosition
                    val episode = episodesList[episodePosition]

                    when (direction) {
                        ItemTouchHelper.LEFT -> {
                            val updatedList: MutableList<WatchedEpisode> = mutableListOf()
                            updatedList.addAll(episodesList)
                            updatedList.remove(episode)

                            watchedEpisodesAdapter.submitList(updatedList)

                            val timer = getTimer() {
                                Log.e(TAG, "onFinish: Timer ended for remove show ${episode.episode_title}!")

                                handleWatchedEpisodeDelete(episode, false)

                            }.start()

                            getSnackbar(
                                bindings.episodedetailsactivityInner.episodedetailsactivityWatchedRecyclerview,
                                "You have removed history item: ${episode.episode_title}"
                            ) {
                                timer.cancel()
                                watchedEpisodesAdapter.submitList(episodesList) {
                                    // For first and last element, always scroll to the position to bring the element to focus
                                    if (episodePosition == 0) {
                                        watchedEpisodesRecyclerView.scrollToPosition(0)
                                    } else if (episodePosition == episodesList.size - 1) {
                                        watchedEpisodesRecyclerView.scrollToPosition(episodesList.size - 1)
                                    }
                                }
                            }.show()
                        }
                    }
                }
            }
        )

        itemTouchHelper.attachToRecyclerView(watchedEpisodesRecyclerView)
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

    private fun displayMessageToast(message: String, length: Int) {
        Toast.makeText(this, message, length).show()
    }


    private fun initDeleteWatchedEpisodeRecycler() {
        val watchedEpisodesLayoutManager = LinearLayoutManager(this)

        watchedEpisodesAdapter = EpisodeWatchedHistoryItemAdapter(callback = { selectedEpisode ->
            handleWatchedEpisodeDelete(selectedEpisode, true)
        })

        watchedEpisodesRecyclerView = bindings.episodedetailsactivityInner.episodedetailsactivityWatchedRecyclerview

        watchedEpisodesRecyclerView.layoutManager = watchedEpisodesLayoutManager
        watchedEpisodesRecyclerView.adapter = watchedEpisodesAdapter

        setupViewSwipeBehaviour(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {
        viewModel.onRefresh()
    }
}