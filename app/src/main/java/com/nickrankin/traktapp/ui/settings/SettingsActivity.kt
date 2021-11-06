package com.nickrankin.traktapp.ui.settings

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.databinding.ActivitySettingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    private lateinit var bindings: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindings = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        setSupportActionBar(bindings.toolbarLayout.toolbar)

        supportActionBar?.title = "Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        showSettings()
    }

    private fun showSettings() {
        supportFragmentManager
            .beginTransaction()
            .add(R.id.settingsactivity_container, SettingsFragment())
            .commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}