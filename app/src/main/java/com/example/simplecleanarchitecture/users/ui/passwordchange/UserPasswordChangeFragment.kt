package com.example.simplecleanarchitecture.users.ui.passwordchange

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.simplecleanarchitecture.MainRouter
import com.example.simplecleanarchitecture.R
import com.example.simplecleanarchitecture.core.lib.view.updateEnabled
import com.example.simplecleanarchitecture.core.lib.view.updateError
import com.example.simplecleanarchitecture.core.lib.view.updateText
import com.example.simplecleanarchitecture.core.lib.view.updateVisible
import com.example.simplecleanarchitecture.databinding.UserPasswordChangeFragmentBinding
import com.example.simplecleanarchitecture.users.ui.passwordchange.UserPasswordChangeViewModel.UiEffect.Message
import com.example.simplecleanarchitecture.users.ui.passwordchange.UserPasswordChangeViewModel.UiEffect.Routing
import com.github.terrakok.cicerone.Back
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.stateViewModel
import org.koin.core.parameter.parametersOf

class UserPasswordChangeFragment : Fragment() {

    private val viewModel: UserPasswordChangeViewModel by stateViewModel(state = { Bundle.EMPTY }) {
        parametersOf(arguments?.getString(USER_ID_KEY) ?: "")
    }

    private val routing: MainRouter by inject()

    private lateinit var binding: UserPasswordChangeFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = UserPasswordChangeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.submitButton.setOnClickListener { viewModel.submit() }
        binding.passwordText.addTextChangedListener {
            viewModel.setPassword(it.toString())
        }
        binding.passwordConfirmedText.addTextChangedListener {
            viewModel.setPasswordConfirmed(it.toString())
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    binding.passwordText.updateText(uiState.password)
                    binding.passwordInput.updateError(uiState.passwordValidation)
                    binding.passwordConfirmedText.updateText(uiState.passwordConfirmed)
                    binding.passwordConfirmedInput.updateError(uiState.passwordConfirmedValidation)
                    binding.submitButton.updateEnabled(uiState.isSubmitEnabled)
                    binding.progressBar.updateVisible(uiState.preloader)
                    if (uiState.preloader) {
                        hideKeyboard()
                    }

                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEffect.collect { uiEffect ->
                    when (uiEffect) {
                        is Message -> {
                            AlertDialog.Builder(requireContext())
                                .setMessage(uiEffect.text)
                                .setPositiveButton(
                                    R.string.dialog_button_close,
                                    DialogInterface.OnClickListener { _, _ -> })
                                .create()
                                .show()
                        }

                        is Routing -> {
                            routing.execute(uiEffect.command)
                        }
                    }
                }
            }
        }
        if (arguments?.getString(USER_ID_KEY).isNullOrEmpty()) {
            routing.execute(Back())
        }
    }

    private fun hideKeyboard() {
        view?.rootView?.windowToken?.let { token ->
            val inputMethodManager =
                requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(token, 0)
        }
    }

    companion object {

        private const val USER_ID_KEY = "USER_ID_KEY"

        fun newInstance(id: String? = null) = UserPasswordChangeFragment().apply {
            arguments = Bundle().apply {
                putString(USER_ID_KEY, id)
            }
        }

    }
}