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
import com.nickrankin.traktapp.dao.credits.model.CreditPerson
import com.nickrankin.traktapp.databinding.FragmentPeopleCreditsBinding
import com.nickrankin.traktapp.helper.IHandleError
import com.nickrankin.traktapp.helper.OnTitleChangeListener
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.person.PeopleCreditsViewModel
import com.nickrankin.traktapp.model.person.PersonOverviewViewModel
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

    private lateinit var creditsRecyclerView: RecyclerView
    private lateinit var adapter: CharacterPosterAdapter

    private val viewModel: PersonOverviewViewModel by activityViewModels()

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

        getTypeFilter()
    }

    private fun getTypeFilter() {
        lifecycleScope.launchWhenStarted {
            viewModel.filter.collectLatest { filterValue ->
                when(filterValue) {
                    CREDIT_MOVIES_KEY -> {
                        updateTitle("Movies")
                        getCredits(filterValue)
                    }
                    SHOW_CREDITS_KEY -> {
                        updateTitle("Shows")
                        getCredits(filterValue)

                    }
                    CREDIT_DIRECTED_KEY -> {
                        updateTitle("Directed/Written")
                        getCredits(filterValue)
                    }
                    else -> {
                        "Unknown"
                    }
                }

            }
        }
    }

    private fun updateTitle(toolbarTitle: String) {

        lifecycleScope.launchWhenStarted {
            viewModel.person.collectLatest { personResource ->
                when(personResource) {
                    is Resource.Loading -> {

                    }
                    is Resource.Success -> {
                        (activity as OnTitleChangeListener).onTitleChanged("$toolbarTitle - ${personResource.data?.name}")
                    }
                    is Resource.Error -> {
                        (activity as OnTitleChangeListener).onTitleChanged("$toolbarTitle - Unknown")

                        Log.e(TAG, "displayCredits: Error getting person ${personResource.error?.localizedMessage}", )
                    }
                }
            }
        }
    }

    private fun getCredits(type: String) {
        when(type) {
            CREDIT_MOVIES_KEY, SHOW_CREDITS_KEY -> {
                lifecycleScope.launchWhenStarted {
                    viewModel.personCast.collectLatest { castResource ->
                        when(castResource) {
                            is Resource.Loading -> {

                            }
                            is Resource.Success -> {
                                if(type == CREDIT_MOVIES_KEY) {
                                    adapter.submitList(castResource.data?.filter { it.type == Type.MOVIE })
                                } else if (type == SHOW_CREDITS_KEY) {
                                    adapter.submitList(castResource.data?.filter { it.type == Type.SHOW })
                                }
                            }
                            is Resource.Error -> {
                                Log.e(TAG, "getCredits: Error getting cast credits ${castResource.error?.message}", )
                                if(!castResource.data.isNullOrEmpty()) {
                                    if(type == CREDIT_MOVIES_KEY) {
                                        adapter.submitList(castResource.data?.filter { it.type == Type.MOVIE })
                                    } else if (type == SHOW_CREDITS_KEY) {
                                        adapter.submitList(castResource.data?.filter { it.type == Type.SHOW })
                                    }
                                }
                            }

                        }
                    }
                }
            }
            CREDIT_DIRECTED_KEY -> {
                lifecycleScope.launchWhenStarted {
                    viewModel.personCrew.collectLatest { crewResource ->
                        when(crewResource) {
                            is Resource.Loading -> {

                            }
                            is Resource.Success -> {
                                adapter.submitList(crewResource.data?.filter {
                                    it.job.uppercase() == "director".uppercase() || it.job.uppercase() == "writer"
                                })
                            }
                            is Resource.Error -> {
                                Log.e(TAG, "getCredits: Error getting crew credits ${crewResource.error?.message}", )

                                if(!crewResource.data.isNullOrEmpty()) {
                                    adapter.submitList(crewResource.data?.filter {
                                        it.job.uppercase() == "director".uppercase() || it.job.uppercase() == "writer"
                                    })
                                }
                            }

                        }
                    }
                }
            }
            else -> {
                Log.e(TAG, "getCredits: Unknown Credits type: $type", )
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

    companion object {
        const val CREDIT_MOVIES_KEY = "movies_credits_key"
        const val SHOW_CREDITS_KEY = "show_credits_key"
        const val CREDIT_DIRECTED_KEY = "directed_credits_key"

        @JvmStatic
        fun newInstance() = PeopleCreditsFragment()
    }
}