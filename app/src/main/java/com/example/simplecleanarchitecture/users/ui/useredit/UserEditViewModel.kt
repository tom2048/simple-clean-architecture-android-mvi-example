package com.example.simplecleanarchitecture.users.ui.useredit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.simplecleanarchitecture.R
import com.example.simplecleanarchitecture.core.lib.const.Patterns
import com.example.simplecleanarchitecture.core.lib.exception.ValidationException
import com.example.simplecleanarchitecture.core.lib.resources.AppResources
import com.example.simplecleanarchitecture.core.lib.viewmodel.BaseUiStateViewModel
import com.example.simplecleanarchitecture.core.model.User
import com.example.simplecleanarchitecture.users.ui.useredit.UserEditViewModel.UiEffect
import com.example.simplecleanarchitecture.users.ui.useredit.UserEditViewModel.UiEffect.Routing
import com.example.simplecleanarchitecture.users.ui.useredit.UserEditViewModel.UiState
import com.example.simplecleanarchitecture.users.usecase.user.UserAddAttachmentUseCase
import com.example.simplecleanarchitecture.users.usecase.user.UserAddAttachmentUseCaseDefault.Type.Avatar
import com.example.simplecleanarchitecture.users.usecase.user.UserAddAttachmentUseCaseDefault.Type.IdScan
import com.example.simplecleanarchitecture.users.usecase.user.UserGetAttachmentUseCase
import com.example.simplecleanarchitecture.users.usecase.user.UserShowDetailsUseCase
import com.example.simplecleanarchitecture.users.usecase.user.UserUpdateUseCase
import com.github.terrakok.cicerone.Back
import com.github.terrakok.cicerone.Command
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class UserEditViewModel(
    private val userId: String,
    private val state: SavedStateHandle,
    private val userShowDetailsUseCase: UserShowDetailsUseCase,
    private val userAddAttachmentUseCase: UserAddAttachmentUseCase,
    private val userGetAttachmentUseCase: UserGetAttachmentUseCase,
    private val userUpdateUseCase: UserUpdateUseCase,
    private val appResources: AppResources
) : BaseUiStateViewModel<UiState, UiEffect>(UiState()) {

    private var isInitialized: Boolean
        get() = state.get<Boolean>(STATE_IS_INITIALIZED) == true
        set(value) = state.set(STATE_IS_INITIALIZED, value)

    init {
        _uiState.value = UiState(
            header = if (!userId.isNullOrEmpty()) {
                appResources.getStringResource(R.string.user_edit_header)
            } else {
                appResources.getStringResource(R.string.user_add_header)
            }
        )
    }

    fun setNickname(nickname: String) {
        _uiState.value = uiState.value.copy(
            nickname = nickname,
            nicknameValidationError = getNicknameValidationError(nickname),
            isSubmitEnabled = getIsSubmitEnabled(
                nickname,
                uiState.value.email,
                uiState.value.description
            )
        )
    }

    private fun getNicknameValidationError(nickname: String): String = when {
        nickname.length > 10 -> appResources.getStringResource(R.string.nickname_validation_message)
        else -> ""
    }

    fun setEmail(email: String) {
        _uiState.value = uiState.value.copy(
            email = email,
            emailValidationError = getEmailValidationError(email),
            isSubmitEnabled = getIsSubmitEnabled(
                uiState.value.nickname,
                email,
                uiState.value.description
            )
        )
    }

    private fun getEmailValidationError(email: String): String = when {
        !Patterns.EMAIL_ADDRESS.matcher(email)
            .matches() -> appResources.getStringResource(R.string.email_validation_message)

        else -> ""
    }

    fun setDescription(description: String) {
        _uiState.value = uiState.value.copy(
            description = description,
            descriptionValidationError = getDescriptionValidationError(description),
            isSubmitEnabled = getIsSubmitEnabled(
                uiState.value.nickname,
                uiState.value.email,
                description
            )
        )
    }

    private fun getDescriptionValidationError(description: String): String = when {
        !Patterns.ALPHANUMERIC.matcher(description)
            .matches() -> appResources.getStringResource(R.string.description_validation_message)

        else -> ""
    }

    private fun getIsSubmitEnabled(nickname: String, email: String, description: String): Boolean =
        nickname.isNotEmpty() && getNicknameValidationError(nickname).isEmpty()
                && email.isNotEmpty() && getEmailValidationError(email).isEmpty()
                && description.isNotEmpty() && getDescriptionValidationError(description).isEmpty()

    fun loadDetails() {
        if (!isInitialized) {
            userId.takeIf { it.isNotEmpty() }?.let { userId ->
                viewModelScope.launch {
                    userShowDetailsUseCase.invoke(userId)
                        .onStart {
                            _uiState.value = uiState.value.copy(preloader = true)
                        }
                        .catch {
                            _uiState.value = uiState.value.copy(preloader = false)
                            _uiEffect.tryEmit(Routing(Back()))
                        }
                        .collectLatest { user ->
                            isInitialized = true
                            _uiState.value = uiState.value.copy(
                                nickname = user.nickname,
                                email = user.email,
                                description = user.description,
                                nicknameValidationError = getNicknameValidationError(user.nickname),
                                emailValidationError = getEmailValidationError(user.email),
                                descriptionValidationError = getDescriptionValidationError(user.description),
                                isSubmitEnabled = getIsSubmitEnabled(
                                    user.nickname,
                                    user.email,
                                    user.description
                                ),
                                avatar = byteDataOf(user.photo),
                                idScan = byteDataOf(user.idScan),
                                preloader = false
                            )
                        }
                }
            } ?: run {
                isInitialized = true
            }
        } else {
            viewModelScope.launch {
                uiState.value.avatarNewAssetKey.takeIf { !it.isNullOrEmpty() }
                    ?.let { userGetAttachmentUseCase(it) }
                    ?: flow { emit(byteArrayOf()) }
                        .combine(uiState.value.idScanNewAssetKey.takeIf { !it.isNullOrEmpty() }
                            ?.let { userGetAttachmentUseCase(it) }
                            ?: flow { emit(byteArrayOf()) }
                        ) { avatar, idScan ->
                            Pair(
                                avatar.takeIf { it.isNotEmpty() },
                                idScan.takeIf { it.isNotEmpty() }
                            )
                        }.catch { it.printStackTrace() }
                        .collectLatest {
                            it.first?.let { avatar ->
                                _uiState.value = uiState.value.copy(avatar = byteDataOf(avatar))
                            }
                            it.second?.let { idScan ->
                                _uiState.value = uiState.value.copy(idScan = byteDataOf(idScan))
                            }
                        }
            }
        }
    }

    fun addAvatar(url: String) {
        viewModelScope.launch {
            userAddAttachmentUseCase
                .invoke(userId, url, Avatar)
                .flatMapConcat { key ->
                    userGetAttachmentUseCase.invoke(key)
                        .map { key to it }
                }
                .flatMapConcat { (key, value) ->
                    userGetAttachmentUseCase(key)
                        .map { key to it }
                }
                .catch {
                    it.printStackTrace()
                }
                .collectLatest {
                    _uiState.value = uiState.value.copy(
                        avatarNewAssetKey = it.first,
                        avatar = byteDataOf(it.second)
                    )
                    //avatarNewAssetKey.value = it.first
                    //avatar.value = it.second
                }
        }
    }

    fun addIdScan(url: String) {
        viewModelScope.launch {
            userAddAttachmentUseCase(userId, url, IdScan)
                .flatMapConcat { key -> userGetAttachmentUseCase(key).map { key to it } }
                .catch { it.printStackTrace() }
                .collectLatest {
                    _uiState.value = uiState.value.copy(
                        idScanNewAssetKey = it.first,
                        idScan = byteDataOf(it.second)
                    )
                }
        }
    }

    fun submit() {
        viewModelScope.launch {
            userUpdateUseCase
                .invoke(
                    User(
                        id = userId,
                        nickname = uiState.value.nickname,
                        email = uiState.value.email,
                        description = uiState.value.description,
                        photo = uiState.value.avatar?.bytes,
                        idScan = uiState.value.idScan?.bytes
                    )
                )
                .onStart { _uiState.value = uiState.value.copy(preloader = true) }
                .catch {
                    _uiState.value = uiState.value.copy(preloader = false)
                    if (it is ValidationException) {
                        _uiEffect.emit(UiEffect.Message(it.validationMessages.map { it.second }
                            .joinToString(separator = "\n")))
                    } else {
                        _uiEffect.emit(UiEffect.Message(appResources.getStringResource(R.string.common_communication_error)))
                    }
                }
                .collectLatest {
                    _uiEffect.emit(Routing(Back()))
                }
        }
    }

    fun cancel() {
        viewModelScope.launch {
            _uiEffect.emit(Routing(Back()))
        }
    }

    companion object {
        private val STATE_IS_INITIALIZED = "STATE_IS_INITIALIZED"
        private val STATE_NICKNAME = "STATE_NICKNAME"
        private val STATE_EMAIL = "STATE_EMAIL"
        private val STATE_DESCRIPTION = "STATE_DESCRIPTION"
        private val STATE_AVATAR = "STATE_AVATAR"
        private val STATE_ID_SCAN = "STATE_ID_SCAN"
    }

    data class UiState(
        val header: String = "",
        val nickname: String = "",
        val email: String = "",
        val description: String = "",
        val nicknameValidationError: String = "",
        val emailValidationError: String = "",
        val descriptionValidationError: String = "",
        val isSubmitEnabled: Boolean = false,
        val avatar: ByteData? = null,
        val avatarNewAssetKey: String = "",
        val idScan: ByteData? = null,
        val idScanNewAssetKey: String = "",
        val preloader: Boolean = false
    )

    sealed class UiEffect {
        data class Routing(val command: Command) : UiEffect()
        data class Message(val text: String) : UiEffect()
    }

}