package com.nickrankin.traktapp.ui.auth

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.room.withTransaction
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.MainActivity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.TmApplication
import com.nickrankin.traktapp.dao.auth.AuthDatabase
import com.nickrankin.traktapp.dao.auth.AuthUserDao
import com.nickrankin.traktapp.dao.auth.model.AuthUser
import com.nickrankin.traktapp.databinding.FragmentAccountBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.IHandleError
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.TitleHelper
import com.nickrankin.traktapp.model.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "AccountFragment"
@AndroidEntryPoint
class AccountFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    private lateinit var bindings: FragmentAccountBinding
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var authUser: AuthUser? = null

    private val viewModel by activityViewModels<AuthViewModel>()

    @Inject
    lateinit var authDatabase: AuthDatabase

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var glide: RequestManager

    private lateinit var authUserDao: AuthUserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        bindings = FragmentAccountBinding.inflate(inflater)
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        authUserDao = authDatabase.authUserDao()
        progressBar = bindings.accountfragmentProgressbar
        swipeRefreshLayout = bindings.accountfragmentSwipeLayout
        swipeRefreshLayout.setOnRefreshListener(this)

        lifecycleScope.launch {
            getUser()
        }
    }

    private suspend fun getUser() {
        viewModel.userProfile.collectLatest { userResource ->
            when(userResource) {
                is Resource.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    Log.d(TAG, "getUser: Loading User Profile")
                }
                is Resource.Success -> {
                    progressBar.visibility = View.GONE

                    if(swipeRefreshLayout.isRefreshing) {
                        swipeRefreshLayout.isRefreshing = false
                    }

                    authUser = userResource.data

                    bindUserData(userResource.data)
                }
                is Resource.Error -> {
                    progressBar.visibility = View.GONE
                    if(swipeRefreshLayout.isRefreshing) {
                        swipeRefreshLayout.isRefreshing = false
                    }

                    if(userResource.data != null) {
                        bindUserData(userResource.data)
                    }

                    (activity as IHandleError).showErrorSnackbarRetryButton(userResource.error, bindings.accountfragmentSwipeLayout) {
                        viewModel.onRefresh()
                    }
                }
            }
        }
    }

    private fun bindUserData(authUser: AuthUser?) {
        bindings.apply {
            if(activity is TitleHelper) {
                (activity as TitleHelper).setTitle("Trakt Account: ${authUser?.username}")
            }

            if(authUser?.avatar?.isNotEmpty() == true) {
                glide
                    .load(authUser.avatar)
                    .into(accountfragmentAvatar)
            }

            accountfragmentName.text = "${authUser?.name} (${authUser?.username})"
            accountfragmentJoinedTrakt.text = "Joined Trakt: ${authUser?.joined_at?.format(DateTimeFormatter.ofPattern(AppConstants.DEFAULT_DATE_FORMAT))}"

            if(authUser?.is_private == true) {
                accountfragmentPrivacy.setBackgroundResource(R.drawable.ic_baseline_lock_24)
            } else {
                accountfragmentPrivacy.setBackgroundResource(R.drawable.ic_baseline_lock_open_24)
            }
            accountfragmentPrivacy.setOnClickListener {
                when(authUser?.is_private) {
                    true -> {
                        showToast("Private Trakt Account", Toast.LENGTH_LONG)
                    }
                    false -> {
                        showToast("Public Trakt Account", Toast.LENGTH_LONG)
                    }
                    else -> {}
                }
            }


            accountfragmentAge.text = "Age: ${authUser?.age}"
            accountfragmentGender.text = "Gender: ${authUser?.gender}"
            accountfragmentLocation.text = "Located: ${authUser?.location}"

            accountfragmentLogoffButton.setOnClickListener {
                handleLogoff()
            }

            progressBar.visibility = View.GONE

        }
    }

    private fun handleLogoff() {
        val app = activity?.application as TmApplication

        AlertDialog.Builder(activity)
            .setTitle("Trakt Logout")
            .setMessage("Are you sure you want to logout of Trakt? You will need to authenticate again to use most app features")
            .setPositiveButton("Logout", DialogInterface.OnClickListener { _, _ ->
                val isLoggedOut = app.logout(false)

                if(isLoggedOut) {
                    // Finally clean up the database cache
                    lifecycleScope.launch {
                        authDatabase.withTransaction {
                            authUserDao.deleteUser(authUser!!)
                        }
                    }

                    val intent = Intent(context, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                }
            })
            .setNegativeButton("Cancel", { dialogInterface, i ->
            })
            .show()


    }

    private fun showToast(message: String, duration: Int) {
        Toast.makeText(context, message, duration).show()
    }

    override fun onStart() {
        super.onStart()

        viewModel.onStart()
    }

    override fun onRefresh() {
        viewModel.onRefresh()
    }

    companion object {
        @JvmStatic
        fun newInstance() = AccountFragment()
    }
}