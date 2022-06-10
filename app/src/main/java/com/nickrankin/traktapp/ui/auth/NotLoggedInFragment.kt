package com.nickrankin.traktapp.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.databinding.FragmentNotLoggedInBinding

class NotLoggedInFragment : Fragment() {

    private lateinit var bindings: FragmentNotLoggedInBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        bindings = FragmentNotLoggedInBinding.inflate(inflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindings.notloggedinConnectButton.setOnClickListener {
            handleAuthButtonPress()
        }
    }

    private fun handleAuthButtonPress() {
        startActivity(Intent(requireContext(), AuthActivity::class.java))
    }

    companion object {
        @JvmStatic
        fun newInstance() = NotLoggedInFragment()
    }
}