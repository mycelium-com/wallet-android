package com.mycelium.supportchat.viewmodel

import com.mycelium.common.UIStateManager
import com.mycelium.supportchat.ui.SupportChatUIState
import me.tatarka.inject.annotations.Inject

/**
 * UI State Manager for the Support Chat screen.
 */
@Inject
class SupportChatUIStateManager : UIStateManager<SupportChatUIState>(
    SupportChatUIState()
)
