package com.example.aiassistant.data.repository

import com.example.aiassistant.data.local.MessageDao
import com.example.aiassistant.data.models.Message
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface MessageRepository {
    fun getMessagesForConversation(conversationId: String): Flow<List<Message>>
    suspend fun insertMessage(message: Message): Long
    suspend fun deleteConversation(conversationId: String)
    suspend fun deleteAllMessages()
}

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao
) : MessageRepository {

    override fun getMessagesForConversation(conversationId: String): Flow<List<Message>> {
        return messageDao.getMessagesForConversation(conversationId)
    }

    override suspend fun insertMessage(message: Message): Long {
        return messageDao.insertMessage(message)
    }

    override suspend fun deleteConversation(conversationId: String) {
        messageDao.deleteConversation(conversationId)
    }

    override suspend fun deleteAllMessages() {
        messageDao.deleteAllMessages()
    }
}

