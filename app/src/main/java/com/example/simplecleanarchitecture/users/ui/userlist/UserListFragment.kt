package com.example.simplecleanarchitecture.users.ui.userlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.simplecleanarchitecture.MainRouter
import com.example.simplecleanarchitecture.R
import com.example.simplecleanarchitecture.core.lib.view.updateDataSetIfChanged
import com.example.simplecleanarchitecture.core.lib.view.updateVisible
import com.example.simplecleanarchitecture.databinding.UserListFragmentBinding
import com.example.simplecleanarchitecture.users.ui.userlist.UserListViewModel.UiEffect.Message
import com.example.simplecleanarchitecture.users.ui.userlist.UserListViewModel.UiEffect.Routing
import com.example.simplecleanarchitecture.users.ui.userlist.UserListViewModel.UiEffect.UserActionConfirmation
import eu.davidea.flexibleadapter.FlexibleAdapter
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class UserListFragment : Fragment() {

    private lateinit var binding: UserListFragmentBinding

    private val viewModel: UserListViewModel by viewModel()

    private val router: MainRouter by inject()

    private val adapter: FlexibleAdapter<UserListItem> = FlexibleAdapter(listOf())


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = UserListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.userList.layoutManager = LinearLayoutManager(requireActivity())
        binding.userList.adapter = adapter

        binding.userAddButton.setOnClickListener { viewModel.addNewUser() }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    binding.progressBar.updateVisible(uiState.preloader)
                    adapter.updateDataSetIfChanged(uiState.userList) { current, new ->
                        current.user == new.user
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEffect.collect { uiEffect ->
                    if (true) {
                        when (uiEffect) {
                            is Message -> AlertDialog.Builder(requireContext())
                                .setMessage(uiEffect.text)
                                .setPositiveButton(R.string.dialog_button_close) { dialog, _ -> dialog.dismiss() }
                                .create()
                                .show()

                            is Routing -> router.execute(uiEffect.command)
                            is UserActionConfirmation -> AlertDialog.Builder(requireContext())
                                .setMessage(R.string.user_delete_confirmation_message)
                                .setPositiveButton(R.string.dialog_button_ok) { dialog, _ ->
                                    viewModel.deleteUserConfirmed(
                                        uiEffect.text
                                    )
                                }
                                .setNegativeButton(R.string.dialog_button_cancel) { dialog, _ -> dialog.dismiss() }
                                .create()
                                .show()
                        }
                    }
                }
            }
        }
        viewModel.loadUsers()
    }


    companion object {
        fun newInstance() = UserListFragment()
    }

}