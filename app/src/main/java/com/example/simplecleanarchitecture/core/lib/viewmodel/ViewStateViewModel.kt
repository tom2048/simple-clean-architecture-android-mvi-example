package com.example.simplecleanarchitecture.core.lib.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class BaseUiStateViewModel<UiState : Any, UiEffect : Any>(uiState: UiState) : ViewModel() {

    @Suppress("PropertyName")
    protected val _uiState = MutableStateFlow<UiState>(uiState)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    @Suppress("PropertyName")
    protected val _uiEffect = MutableSharedFlow<UiEffect>()
    val uiEffect: SharedFlow<UiEffect> = _uiEffect.asSharedFlow()

}