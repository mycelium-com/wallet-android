package com.mycelium.supportchat.ui

import com.mycelium.supportchat.data.model.Message

/**
 * UI state for the Support Chat screen.
 */
data class SupportChatUIState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSending: Boolean = false,
    val isUploadingImage: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
    val rateLimitError: String? = null,
    val canSend: Boolean = true,
    val isInitialized: Boolean = false,
    val isOverLimit: Boolean = false,
    val fullscreenImageUrl: String? = null
)
