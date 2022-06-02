package com.nickrankin.traktapp.ui.person

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.nickrankin.traktapp.BaseActivity
import com.nickrankin.traktapp.adapter.credits.CharacterPosterAdapter
import com.nickrankin.traktapp.dao.credits.model.Person
import com.nickrankin.traktapp.databinding.ActivityPersonBinding
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.person.PersonOverviewViewModel
import com.nickrankin.traktapp.ui.movies.moviedetails.MovieDetailsActivity
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "PersonActivity"
@AndroidEntryPoint
class PersonActivity : BaseActivity(), OnTitleChangeListener {
    private lateinit var bindings: ActivityPersonBinding
    private lateinit var personOverviewFragment: PersonOverviewFragment



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = ActivityPersonBinding.inflate(layoutInflater)

        setContentView(bindings.root)


        setSupportActionBar(bindings.personactivityToolbar.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        showOverviewFragment()
    }

    private fun showOverviewFragment() {
        personOverviewFragment = PersonOverviewFragment.newInstance()

        supportFragmentManager.beginTransaction()
            .replace(bindings.personactivityFragmentContainer.id, personOverviewFragment)
            .commit()
    }

    override fun onTitleChanged(newTitle: String) {
        supportActionBar?.title = newTitle
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()

        return true
    }

    companion object {
        const val PERSON_ID_KEY = "person_id_key"
    }
}