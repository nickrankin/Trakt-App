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

    private var _bindings: FragmentNotLoggedInBinding? = null
    private val bindings get() = _bindings!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _bindings = FragmentNotLoggedInBinding.inflate(inflater)

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

    override fun onDestroyView() {
        super.onDestroyView()

        _bindings = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = NotLoggedInFragment()
    }
}