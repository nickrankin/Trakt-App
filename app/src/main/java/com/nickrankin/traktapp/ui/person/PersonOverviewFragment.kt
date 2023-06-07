package com.nickrankin.traktapp.ui.person

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.chip.Chip
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.OnNavigateToEntity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.credits.CharacterPosterAdapter
import com.nickrankin.traktapp.dao.credits.model.CreditPerson
import com.nickrankin.traktapp.dao.credits.model.CrewType
import com.nickrankin.traktapp.dao.credits.model.Person
import com.nickrankin.traktapp.dao.credits.model.TmCrewPerson
import com.nickrankin.traktapp.databinding.FragmentPersonOverviewBinding
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.person.PersonOverviewViewModel
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val LIMIT = 6
private const val TAG = "PersonOverviewFragment"
private const val TRAKT_DIRECTOR_KEY = "Director"
private const val TRAKT_WRITING_KEY = "Writer"
private const val TRAKT_PRODUCER_KEY = "Producer"
@AndroidEntryPoint
class PersonOverviewFragment : BaseFragment() {

    private var _bindings: FragmentPersonOverviewBinding? = null
    private val bindings get() = _bindings!!

    private lateinit var progressBar: ProgressBar

    private lateinit var personMoviesRecyclerView: RecyclerView
    private var _personMoviesAdapter: CharacterPosterAdapter? = null
    private val personMoviesAdapter get() = _personMoviesAdapter!!

    private lateinit var personShowsRecyclerView: RecyclerView
    private var _personShowsAdapter: CharacterPosterAdapter? = null
    private val personShowsAdapter get() = _personShowsAdapter!!

    private lateinit var persondirectedRecyclerView: RecyclerView
    private var _personDirectedAdapter: CharacterPosterAdapter? = null
    private val personDirectedAdapter get() = _personDirectedAdapter!!

    private val viewModel: PersonOverviewViewModel by activityViewModels()


    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _bindings = FragmentPersonOverviewBinding.inflate(inflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = bindings.personactivityProgressbar

        val personTraktId = arguments?.getInt(PERSON_ID_KEY)

        if(personTraktId != null) {
            viewModel.switchPerson(personTraktId)
        }

        (activity as OnNavigateToEntity).enableOverviewLayout(true)

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
                        progressBar.visibility = View.GONE
                        displayPersonData(personResource.data)

                    }
                    is Resource.Error -> {
                        progressBar.visibility = View.GONE

                        (activity as IHandleError).showErrorSnackbarRetryButton(
                            personResource.error,
                            bindings.root
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
                        handleDirectorsProgressBar(true, false)
                    }
                    is Resource.Success -> {
                        val directed = personCrewResource.data
//                            ?.filter {
//                            it.job.uppercase() == TRAKT_DIRECTOR_KEY.uppercase() || it.job.uppercase() == TRAKT_WRITING_KEY
//                        }

                        if(!directed.isNullOrEmpty()) {
                            handleDirectorsProgressBar(false, true)
                            displayCredits(directed, PeopleCreditsFragment.CREDIT_DIRECTED_KEY)

                            if(directed.size > LIMIT) {
                                bindings.personactivityDirectedBtnAllMovies.visibility = View.VISIBLE
                            } else {
                                bindings.personactivityDirectedBtnAllMovies.visibility = View.GONE
                            }

                        } else {
                            handleDirectorsProgressBar(false, false)
                        }
                    }
                    is Resource.Error -> {
                        if(personCrewResource.data != null) {
                            val directed = personCrewResource.data?.filter {
                                it.job.uppercase() == TRAKT_DIRECTOR_KEY.uppercase() || it.job.uppercase() == TRAKT_WRITING_KEY
                            }

                            if(directed?.isNotEmpty() == true) {
                                handleDirectorsProgressBar(false, true)
                                displayCredits(directed, PeopleCreditsFragment.CREDIT_DIRECTED_KEY)
                            } else {
                                handleDirectorsProgressBar(false, false)
                            }

                            bindings.personactivityDirectedLinearlayout.visibility = View.VISIBLE

                        }

                        handleError(personCrewResource.error, null)
                    }

                }
            }
        }
    }

    private fun handleDirectorsProgressBar(isLoading: Boolean, hasEntries: Boolean) {
        if(isLoading) {
            bindings.personactivityDirectedLinearlayout.visibility = View.GONE
            bindings.personactivityDirectedProgressbar.visibility = View.VISIBLE
        } else {
            if(hasEntries) {
                bindings.personactivityDirectedLinearlayout.visibility = View.VISIBLE
            }
            bindings.personactivityDirectedProgressbar.visibility = View.GONE
        }
    }

    private fun getCastAppearences() {
        lifecycleScope.launchWhenStarted {
            viewModel.personCast.collectLatest { creditsResource ->
                when (creditsResource) {
                    is Resource.Loading -> {
                        handleMovieCreditsProgressBar(true, false)
                        handleShowCreditsProgressBar(true, false)
                    }
                    is Resource.Success -> {
                        val credits = creditsResource.data

                        val movieCredits = credits?.filter { it.type == Type.MOVIE }

                        if(!movieCredits.isNullOrEmpty()) {
                            handleMovieCreditsProgressBar(false, true)
                            displayCredits(movieCredits, PeopleCreditsFragment.CREDIT_MOVIES_KEY)

                            if(movieCredits.size > LIMIT) {
                                bindings.personactivityMovieBtnAllMovies.visibility = View.VISIBLE
                            } else {
                                bindings.personactivityMovieBtnAllMovies.visibility = View.GONE
                            }
                        } else {
                            handleMovieCreditsProgressBar(false, false)
                        }

                        val showCredits = credits?.filter { it.type == Type.SHOW }

                        if(!showCredits.isNullOrEmpty()) {
                            handleShowCreditsProgressBar(false, true)
                            displayCredits(showCredits, PeopleCreditsFragment.SHOW_CREDITS_KEY)

                            if(showCredits.size > LIMIT) {
                                bindings.personactivityShowsBtnAllShows.visibility = View.VISIBLE
                            } else {
                                bindings.personactivityShowsBtnAllShows.visibility = View.GONE
                            }
                        } else {
                            handleShowCreditsProgressBar(false, false)
                        }

                    }
                    is Resource.Error -> {
                        val credits = creditsResource.data

                        val movieCredits = credits?.filter { it.type == Type.MOVIE }

                        if(!movieCredits.isNullOrEmpty()) {
                            handleMovieCreditsProgressBar(false, true)

                            displayCredits(movieCredits, PeopleCreditsFragment.CREDIT_MOVIES_KEY)
                        } else {
                            handleMovieCreditsProgressBar(false, false)
                        }

                        val showCredits = credits?.filter { it.type == Type.SHOW }

                        if(!showCredits.isNullOrEmpty()) {
                            handleShowCreditsProgressBar(false, true)
                            displayCredits(showCredits, PeopleCreditsFragment.SHOW_CREDITS_KEY)
                        } else {
                            handleShowCreditsProgressBar(false, false)
                        }

                        (activity as IHandleError).showErrorSnackbarRetryButton(
                            creditsResource.error,
                            bindings.root
                        ) {
                            viewModel.onRefresh()
                        }
                    }
                }
            }
        }
    }

    private fun handleMovieCreditsProgressBar(isLoading: Boolean, hasEntries: Boolean) {
        if(isLoading) {
            bindings.personactivityMoviesLinearlayout.visibility = View.GONE
            bindings.personactivityMoviesProgressbar.visibility = View.VISIBLE
        } else {
            if(hasEntries) {
                bindings.personactivityMoviesLinearlayout.visibility = View.VISIBLE
            }
            bindings.personactivityMoviesProgressbar.visibility = View.GONE
        }
    }

    private fun handleShowCreditsProgressBar(isLoading: Boolean, hasEntries: Boolean) {
        if(isLoading) {
            bindings.personactivityShowsLinearlayout.visibility = View.GONE
            bindings.personactivityShowsProgressbar.visibility = View.VISIBLE
        } else {
            if(hasEntries) {
                bindings.personactivityShowsLinearlayout.visibility = View.VISIBLE
            }
            bindings.personactivityShowsProgressbar.visibility = View.GONE
        }
    }

    private suspend fun displayCredits(credits: List<CreditPerson>?, type: String) {
        if(credits.isNullOrEmpty()) {
            return
        }

        when(type) {
            PeopleCreditsFragment.CREDIT_DIRECTED_KEY -> {

                displayCrew(credits as List<TmCrewPerson>)

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
                Log.e(TAG, "displayCredits:  Invalid type $type")
            }
        }
    }

    private suspend fun displayCrew(crewList: List<TmCrewPerson>) {

        val writingChip = activity?.findViewById<Chip>(R.id.WRITING)
        val directingChip = activity?.findViewById<Chip>(R.id.DIRECTING)
        val producingChip = activity?.findViewById<Chip>(R.id.PRODUCING)

        // Update The Crewtype Title to include count
        writingChip?.text = "Writing (${crewList.filter { it.crewType == CrewType.WRITING }.size })"
        directingChip?.text = "Directing (${crewList.filter { it.crewType == CrewType.DIRECTING }.size })"
        producingChip?.text = "Pruducer (${crewList.filter { it.crewType == CrewType.PRODUCING }.size })"
        lifecycleScope.launchWhenStarted {
            viewModel.crewType.collectLatest { crewType ->

                // Mark the current crewType as Selected
                when(crewType) {
                    CrewType.DIRECTING -> {
                        directingChip?.isChecked = true
                    }
                    CrewType.PRODUCING -> {
                        producingChip?.isChecked = true

                    }
                    CrewType.WRITING -> {
                        writingChip?.isChecked = true

                    }
                }

                val filteredCrew = crewList.filter { it.crewType == crewType }
                if ((filteredCrew.size) > 6) {
                    personDirectedAdapter.submitList(filteredCrew.subList(0, 6))
                } else {
                    personDirectedAdapter.submitList(filteredCrew)
                }
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
                getFormattedDateTime(
                    person.birthday,
                    sharedPreferences.getString(AppConstants.DATE_FORMAT, AppConstants.DEFAULT_DATE_FORMAT)
                        ?: "",
                    ""
                )
            )
        }

        if (person.death != null) {
            datesSb.append(
                " - " + getFormattedDateTime(
                    person.death,
                    sharedPreferences.getString(AppConstants.DATE_FORMAT, AppConstants.DEFAULT_DATE_FORMAT)
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

        val lm = getResponsiveGridLayoutManager(requireContext(), 3)


        _personMoviesAdapter = CharacterPosterAdapter(glide, tmdbImageLoader) { selectedCredit ->
            (activity as OnNavigateToEntity).navigateToMovie(
                MovieDataModel(
                    selectedCredit.trakt_id,
                    selectedCredit.tmdb_id,
                    selectedCredit.title,
                    selectedCredit.year
                )
            )
        }

        personMoviesRecyclerView.layoutManager = lm
        personMoviesRecyclerView.adapter = personMoviesAdapter
    }

    private fun initPersonShowsAdapter() {
        personShowsRecyclerView = bindings.personactivityShowsRecyclerview

        val lm =  getResponsiveGridLayoutManager(requireContext(), 3)

        _personShowsAdapter = CharacterPosterAdapter(glide, tmdbImageLoader) { selectedCredit ->
            (activity as OnNavigateToEntity).navigateToShow(
                ShowDataModel(
                    selectedCredit.trakt_id,
                    selectedCredit.tmdb_id,
                    selectedCredit.title
                )
            )


        }


        personShowsRecyclerView.layoutManager = lm
        personShowsRecyclerView.adapter = personShowsAdapter
    }

    private fun initPersonDirectedAdapter() {
        val toggleChipGroup = bindings.personactivityDirectedChipgroup
        persondirectedRecyclerView = bindings.personactivityDirectedRecyclerview

        // Populate the toggler chipgroup
        CrewType.values().map { crewType ->
            val chip = Chip(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Choice)
           chip.isCheckable = true

            when(crewType) {
                CrewType.DIRECTING -> {
                    chip.id = R.id.DIRECTING
                }
                CrewType.PRODUCING -> {
                    chip.id = R.id.PRODUCING
                }
                CrewType.WRITING -> {
                    chip.id = R.id.WRITING
                }
            }

            chip.text = crewType.name

            chip.setOnClickListener {
                viewModel.changeCrewType(crewType)
            }

            toggleChipGroup.addView(
                chip
            )
        }

        val lm =  getResponsiveGridLayoutManager(requireContext(), 3)

        _personDirectedAdapter = CharacterPosterAdapter(glide, tmdbImageLoader) { selectedCredit ->
            when(selectedCredit.type) {
                Type.MOVIE -> {
                    (activity as OnNavigateToEntity).navigateToMovie(
                        MovieDataModel(
                            selectedCredit.trakt_id,
                            selectedCredit.tmdb_id,
                            selectedCredit.title,
                            selectedCredit.year
                        )
                    )
                }
                Type.SHOW -> {
                    (activity as OnNavigateToEntity).navigateToShow(
                        ShowDataModel(
                            selectedCredit.trakt_id,
                            selectedCredit.tmdb_id,
                            selectedCredit.title
                        )
                    )

                }
                else -> {
                    Log.e(TAG, "initPersonDirectedAdapter: Unsupported Type ${selectedCredit.type.name}")
                }
            }


        }


        persondirectedRecyclerView.layoutManager = lm
        persondirectedRecyclerView.adapter = personDirectedAdapter
    }

    private fun showAllCredits() {

        val peopleCreditsFragment = PeopleCreditsFragment.newInstance()

        val fragTransaction = activity?.supportFragmentManager?.beginTransaction()

        fragTransaction
            ?.replace(this.id, peopleCreditsFragment)
            ?.commit()

    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Added to prevent memory leaks
        _bindings = null
        _personShowsAdapter = null
        _personDirectedAdapter = null
        _personMoviesAdapter = null

    }

    companion object {
        const val PERSON_ID_KEY = "person_id"
        const val PERSON_NAME_KEY = "person_name_key"
        @JvmStatic
        fun newInstance() =
            PersonOverviewFragment()
    }
}