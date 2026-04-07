package com.mycelium.supportchat.data.model

interface ChatIdentityManager {
    fun getChatId(): String
    fun signMessage(trimmedText: String, timestamp: Long): String
    fun isInitialized(): Boolean
    suspend fun initialize()
}
