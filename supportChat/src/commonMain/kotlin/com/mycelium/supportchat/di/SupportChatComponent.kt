package com.mycelium.supportchat.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mycelium.common.provideUIState
import com.mycelium.supportchat.ui.SupportChatScreen
import com.mycelium.supportchat.viewmodel.SupportChatViewModel
import me.tatarka.inject.annotations.Inject

/**
 * Typealias for the Support Chat screen composable.
 */
typealias SupportChatScreenComposable = @Composable () -> Unit

/**
 * Composable wrapper that connects the ViewModel to the UI.
 */
@Inject
@Composable
fun SupportChatScreenComposable(
    getViewModel: () -> SupportChatViewModel
) {
    val viewModel: SupportChatViewModel = viewModel { getViewModel() }
    val state by viewModel.provideUIState().collectAsState()

    SupportChatScreen(
        state = state,
        onEvent = { event -> viewModel.handleEvent(event) }
    )
}
