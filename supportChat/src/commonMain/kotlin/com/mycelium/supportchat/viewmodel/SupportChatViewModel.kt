package com.mycelium.supportchat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import co.touchlab.kermit.Logger
import com.mycelium.common.WithUIStateManger
import com.mycelium.common.push
import com.mycelium.common.uiState
import com.mycelium.supportchat.data.ChatRepository
import com.mycelium.supportchat.data.ImageUploadResult
import com.mycelium.supportchat.data.ImageUploadService
import com.mycelium.supportchat.data.model.ChatIdentityManager
import com.mycelium.supportchat.data.model.MessageSender
import com.mycelium.supportchat.data.model.SendMessageResult
import com.mycelium.supportchat.ui.SupportChatEvent
import com.mycelium.supportchat.ui.SupportChatUIState
import com.mycelium.wallet.domain.ShareProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tatarka.inject.annotations.Inject

@Inject
class SupportChatViewModel(
    override val uiStateM: SupportChatUIStateManager,
    private val chatRepository: ChatRepository,
    private val identityManager: ChatIdentityManager,
    private val imageUploadService: ImageUploadService,
    private val shareProvider: ShareProvider,
    private val navController: NavController
) : ViewModel(), WithUIStateManger<SupportChatUIState> {

    companion object {
        private const val TAG = "SupportChatViewModel"
        private const val RATE_LIMIT_COOLDOWN_MS = 60_000L
        private const val RECONNECT_DELAY_MS = 5_000L
        private const val MAX_MESSAGE_LENGTH = 2000
    }

    private val logger = Logger.withTag(TAG)
    private val messagesMutex = Mutex()
    private var rateLimitJob: Job? = null
    private var observeJob: Job? = null

    init {
        initialize()
    }

    fun handleEvent(event: SupportChatEvent) {
        when (event) {
            is SupportChatEvent.OnInputChanged -> {
                val constrained = event.text.take(MAX_MESSAGE_LENGTH)
                push(
                    uiState.copy(
                        inputText = constrained,
                        isOverLimit = event.text.length > MAX_MESSAGE_LENGTH
                    )
                )
            }
            is SupportChatEvent.OnSendClick -> sendMessage()
            is SupportChatEvent.OnLoadMore -> loadMoreMessages()
            is SupportChatEvent.OnRetry -> retryInitialization()
            is SupportChatEvent.OnImagePicked -> sendImage(event.imageBytes)
            is SupportChatEvent.OnImageClick -> push(uiState.copy(fullscreenImageUrl = event.imageUrl))
            is SupportChatEvent.OnDismissFullscreenImage -> push(uiState.copy(fullscreenImageUrl = null))
            is SupportChatEvent.OnCopyMessage -> shareProvider.copyToClipboard(event.text)
            is SupportChatEvent.OnDismissError -> dismissError()
            is SupportChatEvent.OnBack -> navController.popBackStack()
        }
    }

    private fun initialize() {
        viewModelScope.launch {
            push(uiState.copy(isLoading = true, error = null))

            try {
                if (!identityManager.isInitialized()) {
                    identityManager.initialize()
                }

                val messages = chatRepository.loadInitialMessages()

                messagesMutex.withLock {
                    push(
                        uiState.copy(
                            messages = messages,
                            isLoading = false,
                            hasMore = chatRepository.hasMore(),
                            isInitialized = true
                        )
                    )
                }

                runCatching { chatRepository.markAsRead() }
                    .onFailure { logger.w(it) { "Failed to mark as read" } }

                startObservingMessages()

                logger.d { "Support chat initialized with ${messages.size} messages" }
            } catch (e: Exception) {
                logger.e(e) { "Failed to initialize support chat" }
                push(
                    uiState.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load messages"
                    )
                )
            }
        }
    }

    private fun retryInitialization() {
        chatRepository.resetPagination()
        initialize()
    }

    private fun loadMoreMessages() {
        if (uiState.isLoadingMore || !uiState.hasMore) return

        viewModelScope.launch {
            push(uiState.copy(isLoadingMore = true))

            try {
                val olderMessages = chatRepository.loadMoreMessages()

                messagesMutex.withLock {
                    push(
                        uiState.copy(
                            messages = olderMessages + uiState.messages,
                            isLoadingMore = false,
                            hasMore = chatRepository.hasMore()
                        )
                    )
                }

                logger.d { "Loaded ${olderMessages.size} more messages" }
            } catch (e: Exception) {
                logger.e(e) { "Failed to load more messages" }
                push(uiState.copy(isLoadingMore = false))
            }
        }
    }

    private fun startObservingMessages() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            while (true) {
                try {
                    chatRepository.observeNewMessages().collect { newMessage ->
                        messagesMutex.withLock {
                            if (uiState.messages.none { it.id == newMessage.id }) {
                                push(uiState.copy(messages = uiState.messages + newMessage))
                            }
                        }

                        if (newMessage.sender == MessageSender.SUPPORT) {
                            runCatching { chatRepository.markAsRead() }
                                .onFailure { logger.w(it) { "Failed to mark as read" } }
                        }

                        logger.d { "New message received: ${newMessage.id}" }
                    }
                } catch (e: Exception) {
                    logger.e(e) { "Message observation failed, reconnecting in ${RECONNECT_DELAY_MS}ms" }
                    delay(RECONNECT_DELAY_MS)
                }
            }
        }
    }

    private fun sendMessage() {
        val text = uiState.inputText.trim()
        if (text.isBlank() || !uiState.canSend) return

        viewModelScope.launch {
            push(uiState.copy(isSending = true, inputText = ""))

            val result = chatRepository.sendMessage(text)
            handleSendResult(result, restoreText = text)
        }
    }

    private fun sendImage(imageBytes: ByteArray) {
        if (!uiState.canSend || uiState.isUploadingImage) return

        viewModelScope.launch {
            push(uiState.copy(isUploadingImage = true))

            when (val uploadResult = imageUploadService.uploadImage(imageBytes)) {
                is ImageUploadResult.Success -> {
                    val text = uiState.inputText.trim()
                    push(uiState.copy(isSending = true, inputText = ""))

                    val sendResult = chatRepository.sendMessage(
                        text = text,
                        imageUrl = uploadResult.imageUrl
                    )
                    handleSendResult(sendResult, restoreText = text)
                }
                is ImageUploadResult.Error -> {
                    push(
                        uiState.copy(
                            isUploadingImage = false,
                            error = uploadResult.message
                        )
                    )
                    logger.e { "Image upload failed: ${uploadResult.message}" }
                }
            }
        }
    }

    private fun handleSendResult(result: SendMessageResult, restoreText: String) {
        when (result) {
            is SendMessageResult.Success -> {
                push(uiState.copy(isSending = false, isUploadingImage = false))
                logger.d { "Message sent: ${result.messageId}" }
            }
            is SendMessageResult.RateLimited -> {
                push(
                    uiState.copy(
                        isSending = false,
                        isUploadingImage = false,
                        inputText = restoreText,
                        rateLimitError = result.message,
                        canSend = false
                    )
                )
                logger.w { "Rate limited: ${result.message}" }
                startRateLimitCooldown()
            }
            is SendMessageResult.Error -> {
                push(
                    uiState.copy(
                        isSending = false,
                        isUploadingImage = false,
                        inputText = restoreText,
                        error = result.message
                    )
                )
                logger.e { "Failed to send message: ${result.message}" }
            }
        }
    }

    private fun startRateLimitCooldown() {
        rateLimitJob?.cancel()
        rateLimitJob = viewModelScope.launch {
            delay(RATE_LIMIT_COOLDOWN_MS)
            push(uiState.copy(canSend = true, rateLimitError = null))
        }
    }

    private fun dismissError() {
        push(uiState.copy(error = null, rateLimitError = null))
    }

    override fun onCleared() {
        super.onCleared()
        observeJob?.cancel()
        rateLimitJob?.cancel()
        logger.d { "SupportChatViewModel cleared" }
    }
}
