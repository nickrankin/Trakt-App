package com.nickrankin.traktapp.ui.person

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.credits.CharacterPosterAdapter
import com.nickrankin.traktapp.dao.credits.model.Person
import com.nickrankin.traktapp.databinding.FragmentPersonOverviewBinding
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.person.PersonOverviewViewModel
import com.nickrankin.traktapp.ui.movies.moviedetails.MovieDetailsActivity
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "PersonOverviewFragment"
@AndroidEntryPoint
class PersonOverviewFragment : Fragment() {

    private lateinit var bindings: FragmentPersonOverviewBinding

    private lateinit var progressBar: ProgressBar

    private lateinit var personMoviesRecyclerView: RecyclerView
    private lateinit var personMoviesAdapter: CharacterPosterAdapter

    private lateinit var personShowsRecyclerView: RecyclerView
    private lateinit var personShowsAdapter: CharacterPosterAdapter

    private lateinit var peopleCreditsFragment: PeopleCreditsFragment

    private val viewModel: PersonOverviewViewModel by activityViewModels()


    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindings = FragmentPersonOverviewBinding.inflate(inflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = bindings.personactivityProgressbar

        initPersonMoviesAdapter()
        initPersonShowsAdapter()

        getPerson()
        getPersonsMovies()
        getPersonsShows()
    }

    private fun getPerson() {
        lifecycleScope.launchWhenStarted {
            viewModel.person.collectLatest { personResource ->

                when(personResource) {
                    is Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        Log.d(TAG, "getPerson: Loading person")
                    }
                    is Resource.Success -> {
                        progressBar.visibility = View.GONE
                        displayPersonData(personResource.data)
                    }
                    is Resource.Error -> {
                        progressBar.visibility = View.GONE

                        Log.e(TAG, "getPerson: Error getting person ${personResource.error?.message}", )
                        personResource.error?.printStackTrace()
                    }
                }
            }
        }
    }

    private fun getPersonsMovies() {
        lifecycleScope.launchWhenStarted {
            viewModel.personMovies.collectLatest { personMoviesResource ->
                when(personMoviesResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getPersonsMovies: Loading ...")
                    }
                    is Resource.Success -> {
                        val movieData = personMoviesResource.data

                        if((movieData?.size ?: 0) > 6) {
                            personMoviesAdapter.submitList(movieData?.subList(0, 6))
                        } else {
                            personMoviesAdapter.submitList(movieData)

                        }

                    }
                    is Resource.Error -> {
                        Log.e(TAG, "getPersonsMovies: Error getting person movies ${personMoviesResource.error?.message}", )
                    }

                }
            }
        }
    }

    private fun getPersonsShows() {
        lifecycleScope.launchWhenStarted {
            viewModel.personShows.collectLatest { personShowsResource ->
                when(personShowsResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getPersonsShows: Loading ...")
                    }
                    is Resource.Success -> {
                        val showData = personShowsResource.data

                        if((showData?.size ?: 0) > 6) {
                            personShowsAdapter.submitList(showData?.subList(0, 6))
                        } else {
                            personShowsAdapter.submitList(showData)
                        }

                    }
                    is Resource.Error -> {
                        Log.e(TAG, "getPersonsShows: Error getting person movies ${personShowsResource.error?.message}", )
                    }

                }
            }
        }
    }

    private fun displayPersonData(person: Person?) {
        if(person == null) {
            return
        }
        (activity as OnTitleChangeListener).onTitleChanged(person.name)

        val datesSb = StringBuilder()

        if(person.birthday != null) {
            datesSb.append("Born: " + getFormattedDate(person.birthday, sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_FORMAT) ?: "", ""))
        }

        if(person.death != null) {
            datesSb.append("- " + getFormattedDate(person.death, sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_FORMAT) ?: "", ""))
        }

        bindings.apply {
            personactivityName.text = person.name

            personactivityDobDeath.text = datesSb.toString()

            if(person.picture_path != null && person.picture_path.isNotBlank()) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + person.picture_path)
                    .into(personactivityProfilePhoto)
            }

            personactivityBirthplace.text = "Birthplace: " + person.birthplace

            personactivityOverview.text = person.biography

            personactivityOverview.setOnClickListener {
                personactivityOverview.toggle()
            }

            personactivityMovieBtnAllMovies.setOnClickListener {
                showAll(Type.MOVIE)
            }

            personactivityShowsBtnAllShows.setOnClickListener {
                showAll(Type.SHOW)
            }

        }
    }

    private fun initPersonMoviesAdapter() {
        personMoviesRecyclerView = bindings.personactivityMoviesRecyclerview

        val lm = FlexboxLayoutManager(requireContext())
        lm.flexDirection = FlexDirection.ROW
        lm.flexWrap = FlexWrap.WRAP

        personMoviesAdapter = CharacterPosterAdapter(glide, tmdbImageLoader) { selectedCredit ->
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

        personMoviesRecyclerView.layoutManager = lm
        personMoviesRecyclerView.adapter = personMoviesAdapter
    }

    private fun initPersonShowsAdapter() {
        personShowsRecyclerView = bindings.personactivityShowsRecyclerview

        val lm = FlexboxLayoutManager(requireContext())
        lm.flexDirection = FlexDirection.ROW
        lm.flexWrap = FlexWrap.WRAP

        personShowsAdapter = CharacterPosterAdapter(glide, tmdbImageLoader) { selectedCredit ->
            val showIntent = Intent(requireContext(), ShowDetailsActivity::class.java)
            showIntent.putExtra(
                ShowDetailsActivity.SHOW_DATA_KEY,
                ShowDataModel(
                    selectedCredit.trakt_id,
                    selectedCredit.tmdb_id,
                    selectedCredit.title)
            )

            startActivity(showIntent)

        }


        personShowsRecyclerView.layoutManager = lm
        personShowsRecyclerView.adapter = personShowsAdapter
    }

    private fun showAll(type: Type) {

        peopleCreditsFragment = PeopleCreditsFragment.newInstance()

        val fragTransaction = activity?.supportFragmentManager?.beginTransaction()

        when(type) {
            Type.MOVIE -> {
                val bundle = Bundle()
                bundle.putString(PeopleCreditsFragment.CREDIT_TYPE_KEY, PeopleCreditsFragment.CREDIT_MOVIES_KEY)

                peopleCreditsFragment.arguments = bundle

                fragTransaction?.replace(this.id, peopleCreditsFragment)
                    ?.addToBackStack("pcf")
                    ?.commit()
            }
            Type.SHOW -> {
                val bundle = Bundle()
                bundle.putString(PeopleCreditsFragment.CREDIT_TYPE_KEY, PeopleCreditsFragment.SHOW_CREDITS_KEY)

                peopleCreditsFragment.arguments = bundle

                fragTransaction?.replace(this.id, peopleCreditsFragment)
                    ?.addToBackStack("pcf")
                    ?.commit()
            }
            else -> {
                Log.e(TAG, "showAll: Unsupported type ${type.name}", )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            PersonOverviewFragment()
    }
}