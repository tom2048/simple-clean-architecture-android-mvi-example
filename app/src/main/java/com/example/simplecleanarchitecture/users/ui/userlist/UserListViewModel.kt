package com.example.simplecleanarchitecture.users.ui.userlist

import androidx.lifecycle.viewModelScope
import com.example.simplecleanarchitecture.R
import com.example.simplecleanarchitecture.RouterScreen
import com.example.simplecleanarchitecture.core.lib.resources.AppResources
import com.example.simplecleanarchitecture.core.lib.viewmodel.BaseUiStateViewModel
import com.example.simplecleanarchitecture.core.repository.model.UserDetails
import com.example.simplecleanarchitecture.users.ui.userlist.UserListViewModel.UiEffect
import com.example.simplecleanarchitecture.users.ui.userlist.UserListViewModel.UiEffect.Message
import com.example.simplecleanarchitecture.users.ui.userlist.UserListViewModel.UiEffect.Routing
import com.example.simplecleanarchitecture.users.ui.userlist.UserListViewModel.UiEffect.UserActionConfirmation
import com.example.simplecleanarchitecture.users.ui.userlist.UserListViewModel.UiState
import com.example.simplecleanarchitecture.users.usecase.user.UserDeleteUseCase
import com.example.simplecleanarchitecture.users.usecase.user.UserShowListUseCase
import com.github.terrakok.cicerone.Command
import com.github.terrakok.cicerone.Forward
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class UserListViewModel(
    private val showListUseCase: UserShowListUseCase,
    private val userDeleteUseCase: UserDeleteUseCase,
    private val appResources: AppResources
) : BaseUiStateViewModel<UiState, UiEffect>(UiState()) {

    fun loadUsers() {
        viewModelScope.launch {
            showListUseCase.invoke()
                .onStart {
                    _uiState.emit(uiState.value.copy(preloader = true))
                }
                .catch {
                    _uiState.emit(uiState.value.copy(preloader = false))
                    _uiEffect.emit(Message(appResources.getStringResource(R.string.common_communication_error)))
                }
                .collectLatest {
                    _uiState.emit(uiState.value.copy(preloader = false))
                    _uiState.value = uiState.value.copy(userList = prepareUserItems(it))
                }
        }
    }

    fun editUser(id: String) {
        viewModelScope.launch {
            _uiEffect.emit(Routing(Forward(RouterScreen.UserEditScreen(id), true)))
        }
    }

    fun addNewUser() {
        viewModelScope.launch {
            _uiEffect.emit(Routing(Forward(RouterScreen.UserEditScreen(null), true)))
        }
    }

    fun deleteUser(id: String) {
        viewModelScope.launch {
            _uiEffect.emit(UserActionConfirmation(id))
        }
    }

    fun changeUserPassword(id: String) {
        viewModelScope.launch {
            _uiEffect.emit(Routing(Forward(RouterScreen.UserPasswordChangeScreen(id), true)))
        }
    }

    fun deleteUserConfirmed(id: String) {
        viewModelScope.launch {
            userDeleteUseCase.invoke(id)
                .combine(showListUseCase.invoke()) { _, list ->
                    list
                }
                .onStart {
                    _uiState.emit(uiState.value.copy(preloader = true))
                }
                .catch {
                    _uiState.emit(uiState.value.copy(preloader = false))
                    _uiEffect.emit(Message(appResources.getStringResource(R.string.common_communication_error)))
                }
                .collectLatest {
                    _uiState.emit(
                        uiState.value.copy(
                            preloader = false,
                            userList = prepareUserItems(it)
                        )
                    )
                    _uiEffect.emit(Message(appResources.getStringResource(R.string.user_delete_success_message)))
                }
        }
    }

    private fun prepareUserItems(users: List<UserDetails>): List<UserListItem> = users.map {
        UserListItem(it,
            { id -> editUser(id) },
            { id -> deleteUser(id) },
            { id -> changeUserPassword(id) }
        )
    }

    data class UiState(
        val preloader: Boolean = false,
        val userList: List<UserListItem> = listOf()
    )

    sealed class UiEffect() {
        data class Routing(val command: Command) : UiEffect()
        data class Message(val text: String) : UiEffect()
        data class UserActionConfirmation(val text: String) : UiEffect()
    }
}