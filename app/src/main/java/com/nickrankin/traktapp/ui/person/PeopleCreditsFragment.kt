package com.nickrankin.traktapp.ui.person

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.nickrankin.traktapp.adapter.credits.CharacterPosterAdapter
import com.nickrankin.traktapp.databinding.FragmentPeopleCreditsBinding
import com.nickrankin.traktapp.helper.IHandleError
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.person.PeopleCreditsViewModel
import com.nickrankin.traktapp.ui.movies.moviedetails.MovieDetailsActivity
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "PeopleCreditsFragment"

@AndroidEntryPoint
class PeopleCreditsFragment : Fragment() {

    private lateinit var bindings: FragmentPeopleCreditsBinding

    private val viewModel: PeopleCreditsViewModel by activityViewModels()

    private lateinit var creditsRecyclerView: RecyclerView
    private lateinit var adapter: CharacterPosterAdapter

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = FragmentPeopleCreditsBinding.inflate(inflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initRecycler()

        val showType = arguments?.getString(CREDIT_TYPE_KEY) ?: CREDIT_MOVIES_KEY

        when (showType) {
            CREDIT_MOVIES_KEY -> {
                getMovies()
            }
            SHOW_CREDITS_KEY -> {
                getShows()
            }
            else -> {
                Log.e(TAG, "onViewCreated: Invalid type: $showType!")
            }
        }
    }

    private fun getMovies() {
        lifecycleScope.launchWhenStarted {
            viewModel.movies.collectLatest { moviesCharacterResource ->

                when (moviesCharacterResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getMovies: Loading movies")
                    }
                    is Resource.Success -> {
                        Log.d(
                            TAG,
                            "getMovies: Adding ${moviesCharacterResource.data?.size} movies to adapter"
                        )

                        adapter.submitList(moviesCharacterResource.data)
                    }
                    is Resource.Error -> {
                        if(moviesCharacterResource.data != null) {
                            adapter.submitList(moviesCharacterResource.data)
                        }

                        (activity as IHandleError).showErrorSnackbarRetryButton(moviesCharacterResource.error, bindings.personactivitySwipeLayout) {
                            viewModel.onRefresh()
                        }
                    }
                }

            }
        }
    }

    private fun getShows() {
        lifecycleScope.launchWhenStarted {
            viewModel.shows.collectLatest { showsCharacterResource ->

                when (showsCharacterResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getShows: Loading movies")
                    }
                    is Resource.Success -> {
                        Log.d(
                            TAG,
                            "getShows: Adding ${showsCharacterResource.data?.size} shows to adapter"
                        )

                        adapter.submitList(showsCharacterResource.data)
                    }
                    is Resource.Error -> {
                        if(showsCharacterResource.data != null) {
                            adapter.submitList(showsCharacterResource.data)
                        }

                        (activity as IHandleError).showErrorSnackbarRetryButton(showsCharacterResource.error, bindings.personactivitySwipeLayout) {
                            viewModel.onRefresh()
                        }
                    }
                }
            }
        }
    }

    private fun initRecycler() {
        creditsRecyclerView = bindings.personactivityCreditsRecyclerview

        val lm = FlexboxLayoutManager(requireContext())
        lm.flexDirection = FlexDirection.ROW
        lm.flexWrap = FlexWrap.WRAP

        adapter = CharacterPosterAdapter(glide, tmdbImageLoader) { selectedCredit ->
            when (selectedCredit.type) {
                Type.MOVIE -> {
                    val movieIntent = Intent(requireContext(), MovieDetailsActivity::class.java)
                    movieIntent.putExtra(
                        MovieDetailsActivity.MOVIE_DATA_KEY,
                        MovieDataModel(
                            selectedCredit.trakt_id,
                            selectedCredit.tmdb_id,
                            selectedCredit.title,
                            selectedCredit.year
                        )
                    )

                    startActivity(movieIntent)
                }
                Type.SHOW -> {
                    val showIntent = Intent(requireContext(), ShowDetailsActivity::class.java)
                    showIntent.putExtra(
                        ShowDetailsActivity.SHOW_DATA_KEY,
                        ShowDataModel(
                            selectedCredit.trakt_id,
                            selectedCredit.tmdb_id,
                            selectedCredit.title
                        )
                    )

                    startActivity(showIntent)
                }
                else -> {
                    Log.e(TAG, "initRecycler: Incompatible type ${selectedCredit.type.name}")
                }
            }

        }

        creditsRecyclerView.layoutManager = lm
        creditsRecyclerView.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    companion object {

        const val CREDIT_TYPE_KEY = "credit_type_key"
        const val CREDIT_MOVIES_KEY = "movies_credis_key"
        const val SHOW_CREDITS_KEY = "show_credits_key"

        @JvmStatic
        fun newInstance() = PeopleCreditsFragment()
    }
}