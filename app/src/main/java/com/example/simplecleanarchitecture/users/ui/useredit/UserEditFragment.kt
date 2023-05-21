package com.example.simplecleanarchitecture.users.ui.useredit

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.whenStarted
import com.example.simplecleanarchitecture.MainRouter
import com.example.simplecleanarchitecture.R
import com.example.simplecleanarchitecture.core.lib.view.updateError
import com.example.simplecleanarchitecture.core.lib.view.updateText
import com.example.simplecleanarchitecture.core.lib.view.updateVisible
import com.example.simplecleanarchitecture.databinding.UserEditFragmentBinding
import com.example.simplecleanarchitecture.users.ui.useredit.UserEditViewModel.UiEffect.Message
import com.example.simplecleanarchitecture.users.ui.useredit.UserEditViewModel.UiEffect.Routing
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.stateViewModel
import org.koin.core.parameter.parametersOf


class UserEditFragment : Fragment() {

    private lateinit var binding: UserEditFragmentBinding

    private val viewModel: UserEditViewModel by stateViewModel(state = { Bundle.EMPTY }) {
        parametersOf(arguments?.getString(USER_ID_KEY) ?: "")
    }

    private val router: MainRouter by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = UserEditFragmentBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.avatarImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                setType("*/*")
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/png", "image/jpg", "image/jpeg"))
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(
                Intent.createChooser(
                    intent,
                    getString(R.string.image_chooser_title)
                ), AVATAR_REQUEST_ID
            )
        }
        binding.idScanImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/png", "image/jpg", "image/jpeg"))
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(
                Intent.createChooser(
                    intent,
                    getString(R.string.image_chooser_title)
                ), ID_SCAN_REQUEST_ID
            )
        }

        binding.nicknameText.addTextChangedListener { viewModel.setNickname(it.toString()) }
        binding.emailText.addTextChangedListener { viewModel.setEmail(it.toString()) }
        binding.descriptionText.addTextChangedListener { viewModel.setDescription(it.toString()) }
        binding.submitButton.setOnClickListener { viewModel.submit() }
        binding.cancelButton.setOnClickListener { viewModel.cancel() }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.header.updateText(state.header)
                    binding.nicknameText.updateText(state.nickname)
                    binding.emailText.updateText(state.email)
                    binding.descriptionText.updateText(state.description)

                    binding.nicknameInput.updateError(state.nicknameValidationError)
                    binding.emailInput.updateError(state.emailValidationError)
                    binding.descriptionInput.updateError(state.descriptionValidationError)

                    state.avatar?.bytes?.let {
                        binding.avatarImage.setImageBitmap(
                            BitmapFactory.decodeByteArray(
                                it,
                                0,
                                it.size,
                                BitmapFactory.Options()
                            )
                        )
                    } ?: run {
                        binding.avatarImage.setImageResource(R.drawable.user_avatar_ico)
                    }

                    state.idScan?.bytes?.let {
                        binding.idScanImage.setImageBitmap(
                            BitmapFactory.decodeByteArray(
                                it,
                                0,
                                it.size,
                                BitmapFactory.Options()
                            )
                        )
                    } ?: run {
                        binding.idScanImage.setImageResource(R.drawable.user_id_scan_ico)
                    }

                    binding.submitButton.updateVisible(state.isSubmitEnabled)
                    binding.progressBar.updateVisible(state.preloader)
                    if (binding.progressBar.isVisible != state.preloader && state.preloader) {
                        hideKeyboard()
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
                        is Routing -> {
                            router.execute(effect.command)
                        }

                        is Message -> {
                            AlertDialog.Builder(requireContext())
                                .setMessage(effect.text)
                                .setPositiveButton(R.string.dialog_button_close, { _, _ -> })
                                .create()
                                .show()
                        }
                    }

                }
            }
        }
        viewModel.loadDetails()
    }

    private fun hideKeyboard() {
        view?.rootView?.windowToken?.let { token ->
            val inputMethodManager =
                requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(token, 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        lifecycleScope.launch {
            viewLifecycleOwner.whenStarted {
                if (requestCode == AVATAR_REQUEST_ID && resultCode == Activity.RESULT_OK && data != null) {
                    data.data?.let { url ->
                        viewModel.addAvatar(url.toString())
                    }
                } else if (requestCode == ID_SCAN_REQUEST_ID && resultCode == Activity.RESULT_OK && data != null) {
                    data.data?.let { url ->
                        viewModel.addIdScan(url.toString())
                    }
                }
            }
        }
    }

    companion object {

        private const val AVATAR_REQUEST_ID = 1001
        private const val ID_SCAN_REQUEST_ID = 1002

        private const val USER_ID_KEY = "USER_ID_KEY"

        fun newInstance(id: String? = null) = UserEditFragment().apply {
            arguments = Bundle().apply {
                putString(USER_ID_KEY, id)
            }
        }
    }

}