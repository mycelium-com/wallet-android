package com.mycelium.wallet.activity.modern.vip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mycelium.bequant.remote.model.User
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.update
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class VipViewModel : ViewModel() {

    private val userRepository = Api.userRepository

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
            val initialUser = userRepository.userFlow.first()
            _stateFlow.update { state ->
                state.copy(
                    progress = false,
                    success = initialUser.isVIP(),
                )
            }
        }
    }

    fun updateVipText(text: String) {
        _stateFlow.update { state -> state.copy(text = text, success = false, error = false) }
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, _ ->
        _stateFlow.update { state ->
            state.copy(
                progress = false,
                success = false,
                error = true,
            )
        }
    }

    fun applyCode() {
        viewModelScope.launch(exceptionHandler) {
            _stateFlow.update { state ->
                state.copy(
                    progress = true,
                    success = false,
                    error = false,
                )
            }
            val status = userRepository.applyVIPCode(_stateFlow.value.text)
            val isVIP = status == User.Status.VIP
            _stateFlow.update { state ->
                state.copy(
                    progress = false,
                    success = isVIP,
                    error = !isVIP,
                )
            }
        }
    }
}