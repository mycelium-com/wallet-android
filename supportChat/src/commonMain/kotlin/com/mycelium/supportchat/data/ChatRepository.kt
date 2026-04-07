package com.mycelium.supportchat.data

import co.touchlab.kermit.Logger
import com.mycelium.common.platform
import com.mycelium.supportchat.data.model.ChatIdentityManager
import com.mycelium.supportchat.data.model.Message
import com.mycelium.supportchat.data.model.MessageSender
import com.mycelium.supportchat.data.model.SendMessageResult
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import me.tatarka.inject.annotations.Inject

/**
 * Repository for managing support chat messages with Firebase Firestore.
 * Handles pagination, real-time updates, and message sending with cryptographic signatures.
 */
@OptIn(ExperimentalTime::class)
@Inject
class ChatRepository(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val identityManager: ChatIdentityManager
) {
    companion object {
        private const val TAG = "ChatRepository"
        private const val COLLECTION_SUPPORT_CHATS = "support_chats"
        private const val COLLECTION_MESSAGES = "messages"
        private const val PAGE_SIZE = 30
        private const val MAX_MESSAGE_LENGTH = 2000
    }

    private val logger = Logger.withTag(TAG)

    private var lastVisibleMessageId: String? = null
    private var lastVisibleCreatedAt: Long? = null
    private var hasMoreMessages = true

    /**
     * Get the current chat ID.
     */
    fun getChatId(): String = identityManager.getChatId()

    /**
     * Check if there are more messages to load.
     */
    fun hasMore(): Boolean = hasMoreMessages

    /**
     * Load the initial page of messages (newest first, then reversed).
     */
    suspend fun loadInitialMessages(): List<Message> {
        val chatId = getChatId()
        logger.d { "Loading initial messages for chat: $chatId" }

        // Reset pagination state
        lastVisibleMessageId = null
        lastVisibleCreatedAt = null
        hasMoreMessages = true

        return try {
            val snapshot = firestore
                .collection(COLLECTION_SUPPORT_CHATS)
                .document(chatId)
                .collection(COLLECTION_MESSAGES)
                .orderBy("createdAt", Direction.DESCENDING)
                .limit(PAGE_SIZE)
                .get()

            val messages = snapshot.documents.map { it.toMessage() }

            if (messages.isNotEmpty()) {
                val lastDoc = messages.last()
                lastVisibleMessageId = lastDoc.id
                lastVisibleCreatedAt = lastDoc.createdAt
            }
            hasMoreMessages = messages.size == PAGE_SIZE

            logger.d { "Loaded ${messages.size} messages, hasMore: $hasMoreMessages" }

            // Return in chronological order (oldest first)
            messages.reversed()
        } catch (e: Exception) {
            logger.e(e) { "Failed to load initial messages" }
            throw e
        }
    }

    /**
     * Load more (older) messages for pagination.
     */
    suspend fun loadMoreMessages(): List<Message> {
        if (!hasMoreMessages) {
            logger.d { "No more messages to load" }
            return emptyList()
        }

        val chatId = getChatId()
        val lastCreatedAt = lastVisibleCreatedAt

        if (lastCreatedAt == null) {
            logger.w { "Cannot load more: no last message timestamp" }
            return emptyList()
        }

        logger.d { "Loading more messages before: $lastCreatedAt" }

        return try {
            val snapshot = firestore
                .collection(COLLECTION_SUPPORT_CHATS)
                .document(chatId)
                .collection(COLLECTION_MESSAGES)
                .orderBy("createdAt", Direction.DESCENDING)
                .where { "createdAt" lessThan lastCreatedAt }
                .limit(PAGE_SIZE)
                .get()

            val messages = snapshot.documents.map { it.toMessage() }

            if (messages.isNotEmpty()) {
                val lastDoc = messages.last()
                lastVisibleMessageId = lastDoc.id
                lastVisibleCreatedAt = lastDoc.createdAt
            }
            hasMoreMessages = messages.size == PAGE_SIZE

            logger.d { "Loaded ${messages.size} more messages, hasMore: $hasMoreMessages" }

            // Return in chronological order
            messages.reversed()
        } catch (e: Exception) {
            logger.e(e) { "Failed to load more messages" }
            throw e
        }
    }

    /**
     * Observe new messages in real-time.
     * Emits only new messages that arrive after subscription.
     */
    fun observeNewMessages(): Flow<Message> {
        val chatId = getChatId()
        logger.d { "Starting to observe new messages for chat: $chatId" }

        return firestore
            .collection(COLLECTION_SUPPORT_CHATS)
            .document(chatId)
            .collection(COLLECTION_MESSAGES)
            .orderBy("createdAt", Direction.DESCENDING)
            .limit(1)
            .snapshots
            .mapNotNull { snapshot ->
                snapshot.documents.firstOrNull()?.toMessage()
            }
            .distinctUntilChanged { old, new -> old.id == new.id }
            .catch { e ->
                logger.e(e) { "Error observing messages" }
            }
    }

    /**
     * Observe the count of unread messages from support.
     */
    fun observeUnreadCount(): Flow<Int> {
        return try {
            val chatId = getChatId()
            firestore
                .collection(COLLECTION_SUPPORT_CHATS)
                .document(chatId)
                .snapshots
                .map { doc ->
                    doc.get<Long?>("unreadByUser")?.toInt() ?: 0
                }
                .catch {
                    logger.e(it) { "Error observing unread count" }
                    emit(0)
                }
        } catch (e: Exception) {
            logger.e(e) { "Failed to start unread count observation" }
            flowOf(0)
        }
    }

    /**
     * Send a message with cryptographic signature verification.
     *
     * TODO: When Cloud Function is deployed, switch back to verifyAndSaveMessage call
     * Currently writes directly to Firestore for testing.
     */
    suspend fun sendMessage(text: String, imageUrl: String? = null): SendMessageResult {
        val trimmedText = text.trim()

        // Validate message
        if (trimmedText.isEmpty() && imageUrl == null) {
            return SendMessageResult.Error("Message cannot be empty")
        }
        if (trimmedText.length > MAX_MESSAGE_LENGTH) {
            return SendMessageResult.Error("Message too long. Maximum $MAX_MESSAGE_LENGTH characters")
        }

        val timestamp = Clock.System.now().toEpochMilliseconds()
        val signature = identityManager.signMessage(trimmedText, timestamp)
        val chatId = getChatId()

        logger.d { "Sending message with timestamp: $timestamp" }

        return try {
            // Direct Firestore write for testing (bypasses Cloud Function verification)
            // TODO: Replace with Cloud Function call when deployed:
            // val result = functions.httpsCallable("verifyAndSaveMessage").invoke(...)

            // Ensure chat document exists
            val chatRef = firestore
                .collection(COLLECTION_SUPPORT_CHATS)
                .document(chatId)

            val chatDoc = chatRef.get()
            if (!chatDoc.exists) {
                // Create chat document if it doesn't exist
                chatRef.set(
                    mapOf(
                        "publicKey" to chatId,
                        "createdAt" to timestamp,
                        "lastMessageAt" to timestamp,
                        "unreadByUser" to 0,
                        "unreadBySupport" to 1,
                        "platform" to platform().lowercase()
                    )
                )
            } else {
                // Update lastMessageAt and increment unread counter
                chatRef.update(
                    mapOf(
                        "lastMessageAt" to timestamp,
                        "unreadBySupport" to FieldValue.increment(1)
                    )
                )
            }

            // Add message to subcollection
            val messageData = mutableMapOf<String, Any?>(
                "text" to trimmedText,
                "sender" to "user",
                "createdAt" to timestamp,
                "signature" to signature,
                "verified" to false  // Not verified without Cloud Function
            )
            if (imageUrl != null) {
                messageData["imageUrl"] = imageUrl
            }

            val messageRef = chatRef
                .collection(COLLECTION_MESSAGES)
                .add(messageData)

            logger.d { "Message sent successfully: ${messageRef.id}" }
            SendMessageResult.Success(messageRef.id)
        } catch (e: Exception) {
            logger.e(e) { "Error sending message: ${e.message}" }
            // Check for rate limiting based on error message
            val errorMessage = e.message ?: "Failed to send message"
            if (errorMessage.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                errorMessage.contains("rate limit", ignoreCase = true)) {
                SendMessageResult.RateLimited("Too many messages. Please wait before sending another.")
            } else {
                SendMessageResult.Error(errorMessage)
            }
        }
    }

    /**
     * Mark all unread messages from support as read.
     */
    suspend fun markAsRead() {
        val chatId = getChatId()
        logger.d { "Marking messages as read for chat: $chatId" }

        try {
            // Get unread messages from support
            val unreadMessages = firestore
                .collection(COLLECTION_SUPPORT_CHATS)
                .document(chatId)
                .collection(COLLECTION_MESSAGES)
                .where { "sender" equalTo "support" }
                .where { "readAt" equalTo null }
                .get()
                .documents

            if (unreadMessages.isNotEmpty()) {
                logger.d { "Marking ${unreadMessages.size} messages as read" }

                // Update each message with readAt timestamp
                val readAt = Clock.System.now().toEpochMilliseconds()

                unreadMessages.forEach { doc ->
                    doc.reference.update(mapOf("readAt" to readAt))
                }
            }

            // Always reset the unread counter
            firestore
                .collection(COLLECTION_SUPPORT_CHATS)
                .document(chatId)
                .update(mapOf("unreadByUser" to 0))

            logger.d { "Messages marked as read successfully" }
        } catch (e: Exception) {
            logger.e(e) { "Failed to mark messages as read" }
            // Don't throw - marking as read is not critical
        }
    }

    /**
     * Update the FCM token for push notifications.
     */
    suspend fun updateFcmToken(token: String) {
        val chatId = getChatId()
        logger.d { "Updating FCM token for chat: $chatId" }

        try {
            firestore
                .collection(COLLECTION_SUPPORT_CHATS)
                .document(chatId)
                .update(mapOf("fcmToken" to token))

            logger.d { "FCM token updated successfully" }
        } catch (e: Exception) {
            logger.e(e) { "Failed to update FCM token" }
            // Don't throw - FCM update failure is not critical
        }
    }

    /**
     * Reset pagination state.
     */
    fun resetPagination() {
        lastVisibleMessageId = null
        lastVisibleCreatedAt = null
        hasMoreMessages = true
    }

    private fun DocumentSnapshot.toMessage(): Message {
        return Message(
            id = id,
            text = get("text") ?: "",
            sender = when (get<String>("sender")) {
                "support" -> MessageSender.SUPPORT
                else -> MessageSender.USER
            },
            createdAt = get<Long>("createdAt") ?: 0L,
            readAt = get<Long?>("readAt"),
            imageUrl = get<String?>("imageUrl")
        )
    }
}
