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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.nickrankin.traktapp.adapter.credits.CharacterPosterAdapter
import com.nickrankin.traktapp.dao.credits.model.CreditPerson
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
class PersonOverviewFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var bindings: FragmentPersonOverviewBinding

    private lateinit var progressBar: ProgressBar

    private lateinit var personMoviesRecyclerView: RecyclerView
    private lateinit var personMoviesAdapter: CharacterPosterAdapter

    private lateinit var personShowsRecyclerView: RecyclerView
    private lateinit var personShowsAdapter: CharacterPosterAdapter

    private lateinit var persondirectedRecyclerView: RecyclerView
    private lateinit var personDirectedAdapter: CharacterPosterAdapter

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

        swipeRefreshLayout = bindings.personactivitySwipeLayout
        swipeRefreshLayout.setOnRefreshListener(this)

        initPersonMoviesAdapter()
        initPersonShowsAdapter()
        initPersonDirectedAdapter()

        getPerson()
        getCastAppearences()
        getCrewAppearences()
    }

    private fun getPerson() {
        lifecycleScope.launchWhenStarted {
            viewModel.person.collectLatest { personResource ->

                when (personResource) {
                    is Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        Log.d(TAG, "getPerson: Loading person")
                    }
                    is Resource.Success -> {
                        if(swipeRefreshLayout.isRefreshing) {
                            swipeRefreshLayout.isRefreshing = false
                        }
                        progressBar.visibility = View.GONE
                        displayPersonData(personResource.data)



                    }
                    is Resource.Error -> {
                        progressBar.visibility = View.GONE

                        (activity as IHandleError).showErrorSnackbarRetryButton(
                            personResource.error,
                            bindings.personactivitySwipeLayout
                        ) {
                            viewModel.onRefresh()
                        }
                    }
                }
            }
        }
    }

    private fun getCrewAppearences() {
        lifecycleScope.launchWhenStarted {
            viewModel.personCrew.collectLatest { personCrewResource ->
                when(personCrewResource) {
                    is Resource.Loading -> {

                    }
                    is Resource.Success -> {

                        val directed = personCrewResource.data?.filter {
                            it.job.uppercase() == "director".uppercase() || it.job.uppercase() == "writer"
                        }

                        if(!directed.isNullOrEmpty()) {
                            bindings.personactivityDirectedGroup.visibility = View.VISIBLE
                            displayCredits(directed, PeopleCreditsFragment.CREDIT_DIRECTED_KEY)
                        }
                    }
                    is Resource.Error -> TODO()

                }
            }
        }
    }

    private fun getCastAppearences() {
        lifecycleScope.launchWhenStarted {
            viewModel.personCast.collectLatest { creditsResource ->
                when (creditsResource) {
                    is Resource.Loading -> {
                        bindings.personactivityMovieGroup.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        val credits = creditsResource.data



                        val movieCredits = credits?.filter { it.type == Type.MOVIE }

                        if(!movieCredits.isNullOrEmpty()) {
                            bindings.personactivityMovieGroup.visibility = View.VISIBLE
                            displayCredits(movieCredits, PeopleCreditsFragment.CREDIT_MOVIES_KEY)
                        }

                        val showCredits = credits?.filter { it.type == Type.SHOW }

                        if(!showCredits.isNullOrEmpty()) {
                            bindings.personactivityShowGroup.visibility = View.VISIBLE
                            displayCredits(showCredits, PeopleCreditsFragment.SHOW_CREDITS_KEY)
                        }

                    }
                    is Resource.Error -> {
                        val credits = creditsResource.data

                        val movieCredits = credits?.filter { it.type == Type.MOVIE }

                        if(!movieCredits.isNullOrEmpty()) {
                            bindings.personactivityMovieGroup.visibility = View.VISIBLE
                            displayCredits(movieCredits, PeopleCreditsFragment.CREDIT_MOVIES_KEY)
                        }

                        val showCredits = credits?.filter { it.type == Type.SHOW }

                        if(!showCredits.isNullOrEmpty()) {
                            bindings.personactivityShowGroup.visibility = View.VISIBLE
                            displayCredits(showCredits, PeopleCreditsFragment.SHOW_CREDITS_KEY)
                        }

                        (activity as IHandleError).showErrorSnackbarRetryButton(
                            creditsResource.error,
                            bindings.personactivitySwipeLayout
                        ) {
                            viewModel.onRefresh()
                        }
                    }

                }
            }
        }
    }

    private fun displayCredits(credits: List<CreditPerson>?, type: String) {
        if(credits.isNullOrEmpty()) {
            return
        }

        when(type) {
            PeopleCreditsFragment.CREDIT_DIRECTED_KEY -> {
                if ((credits.size) > 6) {
                    personDirectedAdapter.submitList(credits.subList(0, 6))
                } else {
                    personDirectedAdapter.submitList(credits)
                }

                bindings.personactivityDirectedBtnAllMovies.setOnClickListener {
                    viewModel.changeFilter(PeopleCreditsFragment.CREDIT_DIRECTED_KEY)
                    showAllCredits()
                }
            }
            PeopleCreditsFragment.CREDIT_MOVIES_KEY -> {
                if ((credits.size) > 6) {
                    personMoviesAdapter.submitList(credits.subList(0, 6))
                } else {
                    personMoviesAdapter.submitList(credits)
                }

                bindings.personactivityMovieBtnAllMovies.setOnClickListener {
                    viewModel.changeFilter(PeopleCreditsFragment.CREDIT_MOVIES_KEY)

                    showAllCredits()
                }
            }
            PeopleCreditsFragment.SHOW_CREDITS_KEY -> {
                if ((credits.size) > 6) {
                    personShowsAdapter.submitList(credits.subList(0, 6))
                } else {
                    personShowsAdapter.submitList(credits)
                }

                bindings.personactivityShowsBtnAllShows.setOnClickListener {
                    viewModel.changeFilter(PeopleCreditsFragment.SHOW_CREDITS_KEY)

                    showAllCredits()

                }
            }
            else -> {
                Log.e(TAG, "displayCredits:  Invalid type $type", )
            }
        }



    }

    private fun displayPersonData(person: Person?) {
        if (person == null) {
            return
        }



        (activity as OnTitleChangeListener).onTitleChanged(person.name)

        val datesSb = StringBuilder()

        if (person.birthday != null) {
            datesSb.append(
                getFormattedDate(
                    person.birthday,
                    sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_FORMAT)
                        ?: "",
                    ""
                )
            )
        }

        if (person.death != null) {
            datesSb.append(
                " - " + getFormattedDate(
                    person.death,
                    sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_FORMAT)
                        ?: "",
                    ""
                )
            )
        }

        bindings.apply {
            personactivityName.text = person.name

            personactivityDobDeath.text = datesSb.toString()

            if (person.picture_path != null && person.picture_path.isNotBlank()) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + person.picture_path)
                    .into(personactivityProfilePhoto)
            }

            if(person.birthplace != null) {
                personactivityBirthplace.text = "Birthplace: " + person.birthplace
            }

            personactivityOverview.text = person.biography

            personactivityOverview.setOnClickListener {
                personactivityOverview.toggle()
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
                    selectedCredit.title
                )
            )

            startActivity(showIntent)

        }


        personShowsRecyclerView.layoutManager = lm
        personShowsRecyclerView.adapter = personShowsAdapter
    }

    private fun initPersonDirectedAdapter() {
        persondirectedRecyclerView = bindings.personactivityDirectedRecyclerview

        val lm = FlexboxLayoutManager(requireContext())
        lm.flexDirection = FlexDirection.ROW
        lm.flexWrap = FlexWrap.WRAP

        personDirectedAdapter = CharacterPosterAdapter(glide, tmdbImageLoader) { selectedCredit ->
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


        persondirectedRecyclerView.layoutManager = lm
        persondirectedRecyclerView.adapter = personDirectedAdapter
    }

    private fun showAllCredits() {

        val peopleCreditsFragment = PeopleCreditsFragment.newInstance()

        val fragTransaction = activity?.supportFragmentManager?.beginTransaction()

        fragTransaction
            ?.replace(this.id, peopleCreditsFragment)
            ?.addToBackStack("pcf")
            ?.commit()

    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {
        viewModel.onRefresh()
    }

    companion object {
        const val PERSON_NAME_KEY = "person_name_key"
        @JvmStatic
        fun newInstance() =
            PersonOverviewFragment()
    }
}