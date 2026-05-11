package com.mycelium.supportchat.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a chat message in the support chat.
 */
@Serializable
data class Message(
    val id: String = "",
    val text: String,
    val sender: MessageSender,
    val createdAt: Long, // Timestamp in milliseconds
    val readAt: Long? = null,
    val imageUrl: String? = null
)

/**
 * Indicates who sent the message.
 */
@Serializable
enum class MessageSender {
    @SerialName("user")
    USER,
    @SerialName("support")
    SUPPORT
}

/**
 * Result of sending a message.
 */
sealed class SendMessageResult {
    data class Success(val messageId: String) : SendMessageResult()
    data class RateLimited(val message: String) : SendMessageResult()
    data class Error(val message: String) : SendMessageResult()
}
