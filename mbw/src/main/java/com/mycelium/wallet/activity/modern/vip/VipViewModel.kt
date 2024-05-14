package com.mycelium.wallet.activity.modern.vip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mycelium.wallet.external.changelly2.remote.Api
import com.mycelium.wallet.update
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class VipViewModel : ViewModel() {

    private val userRepository = Api.statusRepository

    data class State(
        val success: Boolean = false,
        val error: Boolean = false,
        val progress: Boolean = true,
        val text: String = "",
    )

    private val _stateFlow = MutableStateFlow(State())
    val stateFlow = _stateFlow.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.statusFlow.collect { status ->
                val success = status.isVIP()
                _stateFlow.update { state -> state.copy(progress = false, success = success) }
            }
        }
    }

    fun updateVipText(text: String) {
        _stateFlow.update { s -> s.copy(text = text, error = false) }
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, _ ->
        _stateFlow.update { s -> s.copy(progress = false, error = true, success = false) }
    }

    fun applyCode() {
        viewModelScope.launch(exceptionHandler) {
            _stateFlow.update { s -> s.copy(progress = true, error = false, success = false) }
            val status = userRepository.applyVIPCode(_stateFlow.value.text)
            _stateFlow.update { s -> s.copy(progress = false, error = !status.isVIP()) }
        }
    }
}