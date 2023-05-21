package com.example.simplecleanarchitecture.users.ui.passwordchange

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.simplecleanarchitecture.R
import com.example.simplecleanarchitecture.core.lib.const.Patterns
import com.example.simplecleanarchitecture.core.lib.resources.AppResources
import com.example.simplecleanarchitecture.core.lib.viewmodel.BaseUiStateViewModel
import com.example.simplecleanarchitecture.users.ui.passwordchange.UserPasswordChangeViewModel.UiEffect
import com.example.simplecleanarchitecture.users.ui.passwordchange.UserPasswordChangeViewModel.UiState
import com.example.simplecleanarchitecture.users.usecase.user.UserPasswordUpdateUseCase
import com.github.terrakok.cicerone.Back
import com.github.terrakok.cicerone.Command
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class UserPasswordChangeViewModel(
    private val userId: String,
    private val savedState: SavedStateHandle,
    private val passwordUpdateUseCase: UserPasswordUpdateUseCase,
    private val appResources: AppResources
) : BaseUiStateViewModel<UiState, UiEffect>(UiState()) {


    init {
        setPassword(savedState[STATE_PASSWORD] ?: "")
        setPasswordConfirmed(savedState[STATE_PASSWORD_CONFIRMED] ?: "")
    }


    fun setPassword(password: String) {
        _uiState.value = uiState.value.copy(
            password = password,
            passwordValidation = validatePassword(password),
            passwordConfirmedValidation = validatePasswordConfirmed(
                password,
                uiState.value.passwordConfirmed
            ),
            isSubmitEnabled = isSubmitEnabled(password, uiState.value.passwordConfirmed)
        )
    }


    fun setPasswordConfirmed(passwordConfirmed: String) {
        _uiState.value = uiState.value.copy(
            passwordConfirmed = passwordConfirmed,
            passwordValidation = validatePassword(uiState.value.password),
            passwordConfirmedValidation = validatePasswordConfirmed(
                uiState.value.password,
                passwordConfirmed
            ),
            isSubmitEnabled = isSubmitEnabled(uiState.value.password, passwordConfirmed)
        )
    }


    fun submit() {
        userId.takeIf { it.isNotEmpty() }?.let { userId ->
            viewModelScope.launch {
                passwordUpdateUseCase(userId, uiState.value.password)
                    .onStart { _uiState.value = uiState.value.copy(preloader = true) }
                    .catch {
                        _uiState.value = uiState.value.copy(preloader = false)
                        _uiEffect.emit(UiEffect.Message(appResources.getStringResource(R.string.common_communication_error)))
                    }
                    .collectLatest { _uiEffect.emit(UiEffect.Routing(Back())) }
            }
        } ?: run {
            _uiEffect.tryEmit(UiEffect.Routing(Back()))
        }
    }


    private fun validatePassword(password: String) =
        if (!password.isNullOrEmpty() && !Patterns.PASSWORD.matcher(password).matches()) {
            appResources.getStringResource(R.string.password_validation_message)
        } else {
            ""
        }


    private fun validatePasswordConfirmed(password: String, passwordConfirmed: String) =
        if (!password.isNullOrEmpty() && !passwordConfirmed.isNullOrEmpty() && passwordConfirmed != password
        ) {
            appResources.getStringResource(R.string.password_confirmation_validation_message)
        } else {
            ""
        }


    private fun isSubmitEnabled(password: String, passwordConfirmed: String) =
        validatePassword(password).isNullOrEmpty()
                && validatePasswordConfirmed(password, passwordConfirmed).isNullOrEmpty()
                && !password.isNullOrEmpty()
                && !passwordConfirmed.isNullOrEmpty()


    override fun onCleared() {
        super.onCleared()
        savedState[STATE_PASSWORD] = uiState.value.password
        savedState[STATE_PASSWORD_CONFIRMED] = uiState.value.passwordConfirmed
    }

    companion object {
        private val STATE_PASSWORD = "STATE_PASSWORD"
        private val STATE_PASSWORD_CONFIRMED = "STATE_PASSWORD_CONFIRMED"
    }

    data class UiState(
        val password: String = "",
        val passwordConfirmed: String = "",
        val passwordValidation: String = "",
        val passwordConfirmedValidation: String = "",
        val preloader: Boolean = false,
        val isSubmitEnabled: Boolean = false
    )

    sealed class UiEffect {
        data class Routing(val command: Command) : UiEffect()
        data class Message(val text: String) : UiEffect()
    }

}