package com.mycelium.supportchat.ui

/**
 * Events for the Support Chat screen.
 */
sealed class SupportChatEvent {
    /**
     * User changed the input text.
     */
    data class OnInputChanged(val text: String) : SupportChatEvent()

    /**
     * User clicked the send button.
     */
    data object OnSendClick : SupportChatEvent()

    /**
     * User scrolled to load more messages.
     */
    data object OnLoadMore : SupportChatEvent()

    /**
     * User clicked retry after an error.
     */
    data object OnRetry : SupportChatEvent()

    /**
     * User dismissed the error.
     */
    data object OnDismissError : SupportChatEvent()

    /**
     * User tapped on a message to copy it.
     */
    data class OnCopyMessage(val text: String) : SupportChatEvent()
    /**
     * User picked an image to send.
     */
    data class OnImagePicked(val imageBytes: ByteArray) : SupportChatEvent()

    /**
     * User clicked on an image to open it fullscreen.
     */
    data class OnImageClick(val imageUrl: String) : SupportChatEvent()

    /**
     * User dismissed the fullscreen image viewer.
     */
    data object OnDismissFullscreenImage : SupportChatEvent()

    /**
     * User clicked back.
     */
    data object OnBack : SupportChatEvent()
}
