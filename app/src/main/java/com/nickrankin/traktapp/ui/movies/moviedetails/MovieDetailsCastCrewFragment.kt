package com.nickrankin.traktapp.ui.movies.moviedetails

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.adapter.credits.MovieCastCreditsAdapter
import com.nickrankin.traktapp.dao.movies.model.TmMovie
import com.nickrankin.traktapp.databinding.FragmentMovieCastcrewOverviewBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.movies.MovieDetailsViewModel
import com.nickrankin.traktapp.ui.person.PersonActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "MovieDetailsOverviewFra"

@AndroidEntryPoint
class MovieDetailsCastCrewFragment : BaseFragment() {

    private lateinit var bindings: FragmentMovieCastcrewOverviewBinding
    private val viewModel: MovieDetailsViewModel by activityViewModels()

    private lateinit var castRecyclerView: RecyclerView
    private lateinit var castAdapter: MovieCastCreditsAdapter

    @Inject
    lateinit var glide: RequestManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = FragmentMovieCastcrewOverviewBinding.inflate(inflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        getCredits()
    }


    private fun getCredits() {
        lifecycleScope.launchWhenStarted {
            viewModel.credits.collectLatest { creditsResource ->
                when(creditsResource) {
                    is Resource.Loading -> {
                        bindings.moviedetailsoverviewProgressbar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        bindings.moviedetailsoverviewProgressbar.visibility = View.GONE

                        val credits = creditsResource.data

                        castAdapter.submitList(credits?.sortedBy { it.ordering })
                    }
                    is Resource.Error -> {
                        handleError(creditsResource.error, null)

                        val credits = creditsResource.data

                        if(credits != null && credits.isNotEmpty()) {
                            castAdapter.submitList(credits?.sortedBy { it.ordering })
                        }


                    }

                }
            }
        }
    }

    private fun initRecyclerView() {
        Log.d(TAG, "initRecyclerView: HERE")
        castRecyclerView = bindings.moviedetailsoverviewCastRecycler
        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL

        castAdapter = MovieCastCreditsAdapter(glide) { selectedCredit ->
            val creditIntent = Intent(requireContext(), PersonActivity::class.java)
            creditIntent.putExtra(PersonActivity.PERSON_ID_KEY, selectedCredit.person_trakt_id)

            startActivity(creditIntent)
        }

        castRecyclerView.layoutManager = layoutManager
        castRecyclerView.adapter = castAdapter
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            MovieDetailsCastCrewFragment()

        const val TMDB_ID_KEY = "tmdb_key"
        const val OVERVIEW_KEY = "overview_key"
    }


}