package com.nickrankin.traktapp.ui.shows.showdetails

import android.content.Intent
import android.os.Bundle
import android.util.LayoutDirection
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
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.credits.ShowCastCreditsAdapter
import com.nickrankin.traktapp.databinding.FragmentShowCreditsBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.ICreditsPersons
import com.nickrankin.traktapp.model.shows.ShowDetailsViewModel
import com.nickrankin.traktapp.ui.person.PersonActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "ShowCreditsFragment"
@AndroidEntryPoint
class ShowCreditsFragment() : BaseFragment() {
    private lateinit var bindings: FragmentShowCreditsBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ShowCastCreditsAdapter

    private val viewModel: ShowDetailsViewModel by activityViewModels()

    @Inject
    lateinit var glide: RequestManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initRecycler()
        //setupCastSwitcher()
        getCredits()
    }

    private fun getCredits() {
        lifecycleScope.launchWhenStarted {
            val progressBar = bindings.showcreditsfragmentProgressbar
            viewModel.cast.collectLatest { castResource ->
                when(castResource) {
                    is Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        Log.d(TAG, "getCredits: Got ${castResource.data?.size} castpeople!")

                        val castList = castResource.data

                        if(castList.isNullOrEmpty()) {
                            bindings.showcreditsfragmentNoCast.visibility = View.VISIBLE
                            adapter.submitList(emptyList())
                        } else {
                            bindings.showcreditsfragmentNoCast.visibility = View.GONE
                            adapter.submitList(castList)
                        }

                        progressBar.visibility = View.GONE
                    }
                    is Resource.Error -> {
                        val castList = castResource.data

                        if(castList.isNullOrEmpty()) {
                            bindings.showcreditsfragmentNoCast.visibility = View.VISIBLE
                            adapter.submitList(emptyList())
                        } else {
                            bindings.showcreditsfragmentNoCast.visibility = View.GONE
                            adapter.submitList(castList)
                        }

                        progressBar.visibility = View.GONE

                        handleError(castResource.error, null)
                    }
                }
            }
        }
    }

    private fun initRecycler() {
        recyclerView = bindings.showcreditsfragmentCastRecycler
        val lm = LinearLayoutManager(requireContext())
        lm.orientation = LinearLayoutManager.HORIZONTAL

        adapter = ShowCastCreditsAdapter(glide) { person ->
            navigateToPerson(person.person_trakt_id)
        }

        recyclerView.layoutManager = lm
        recyclerView.adapter = adapter
    }

    private fun setupCastSwitcher() {
        // Display the switcher
        bindings.showcreditsfragmentChipgroup.visibility = View.VISIBLE

        val mainCastBtn = bindings.showcreditsfragmentCastAll
        val guestCastBtn = bindings.showcreditsfragmentCastGuest

        mainCastBtn.setOnClickListener {
            viewModel.filterCast(false)

            guestCastBtn.isChecked = false
            mainCastBtn.isChecked = true
        }

        guestCastBtn.setOnClickListener {
            viewModel.filterCast(true)

            guestCastBtn.isChecked = true
            mainCastBtn.isChecked = false
        }
    }

    private fun navigateToPerson(personTraktId: Int) {
        val intent = Intent(context, PersonActivity::class.java)
        intent.putExtra(PersonActivity.PERSON_ID_KEY, personTraktId)
        startActivity(intent)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = FragmentShowCreditsBinding.inflate(layoutInflater)

        return bindings.root

    }

    companion object {
        @JvmStatic
        fun newInstance() = ShowCreditsFragment()
    }
}