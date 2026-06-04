package com.replyai.data.repository

import com.replyai.data.api.ApiService
import com.replyai.data.models.*
import com.replyai.utils.parseApiError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun getSessions(): Result<List<ChatSession>> = runCatching {
        val response = api.getSessions()
        if (!response.isSuccessful) throw Exception(parseApiError(response.errorBody()?.string()))
        response.body() ?: emptyList()
    }

    suspend fun getFavoriteSessions(): Result<List<ChatSession>> = runCatching {
        val response = api.getFavoriteSessions()
        if (!response.isSuccessful) throw Exception(parseApiError(response.errorBody()?.string()))
        response.body() ?: emptyList()
    }

    suspend fun getFavoriteResponses(): Result<List<FavoriteAIResponse>> = runCatching {
        val response = api.getFavoriteResponses()
        if (!response.isSuccessful) throw Exception(parseApiError(response.errorBody()?.string()))
        response.body() ?: emptyList()
    }

    suspend fun createSession(messenger: String, title: String): Result<ChatSession> = runCatching {
        val response = api.createSession(CreateSessionRequest(messenger, title))
        if (!response.isSuccessful) throw Exception(parseApiError(response.errorBody()?.string()))
        response.body() ?: throw Exception("Пустой ответ")
    }

    suspend fun getSession(uuid: String): Result<ChatSession> = runCatching {
        val response = api.getSession(uuid)
        if (!response.isSuccessful) throw Exception(parseApiError(response.errorBody()?.string()))
        response.body() ?: throw Exception("Пустой ответ")
    }

    suspend fun patchSession(uuid: String, patch: SessionPatch): Result<ChatSession> = runCatching {
        val response = api.patchSession(uuid, patch)
        if (!response.isSuccessful) throw Exception(parseApiError(response.errorBody()?.string()))
        response.body() ?: throw Exception("Пустой ответ")
    }

    suspend fun toggleFavorite(uuid: String): Result<ToggleFavoriteResponse> = runCatching {
        val response = api.toggleFavorite(uuid)
        if (!response.isSuccessful) throw Exception(parseApiError(response.errorBody()?.string()))
        response.body() ?: throw Exception("Пустой ответ")
    }

    suspend fun deleteSession(uuid: String): Result<Unit> = runCatching {
        val response = api.deleteSession(uuid)
        if (!response.isSuccessful) throw Exception(parseApiError(response.errorBody()?.string()))
    }

    suspend fun syncMessages(sessionId: String, messages: List<String>): Result<Int> = runCatching {
        var synced = 0
        for (msg in messages) {
            addMessage(sessionId, "other", msg).onSuccess { synced++ }
        }
        synced
    }

    suspend fun addMessage(sessionId: String, sender: String, content: String): Result<ChatMessage> =
        runCatching {
            val response = api.addMessage(
                sessionId,
                CreateMessageRequest(sender = sender, content = content)
            )
            if (!response.isSuccessful) throw Exception(parseApiError(response.errorBody()?.string()))
            response.body() ?: throw Exception("Пустой ответ")
        }

    /**
     * POST /api/chats/sessions/{id}/ask/
     * Body: { request_type, user_prompt, tone, target_language } — validated via [AskRequest.create].
     */
    suspend fun askAI(
        sessionId: String,
        userPrompt: String,
        requestType: String = RequestTypes.REPLY_HELP,
        tone: String = ToneChoices.FRIENDLY,
        targetLanguage: String = "ru"
    ): Result<AIRequestResponse> = runCatching {
        val body = AskRequest.create(
            userPrompt = userPrompt,
            requestType = requestType,
            tone = tone,
            targetLanguage = targetLanguage
        )
        val response = api.askAI(sessionId, body)
        if (!response.isSuccessful) {
            throw Exception(parseApiError(response.errorBody()?.string()))
        }
        val aiRequest = response.body() ?: throw Exception("Пустой ответ сервера")
        if (aiRequest.extractGeneratedText() == null) {
            throw Exception("Ответ AI без generated_text")
        }
        aiRequest
    }

    suspend fun sendFeedback(
        responseId: String,
        feedback: Int? = null,
        isFavorite: Boolean? = null
    ): Result<AIResponseDto> = runCatching {
        val response = api.updateFeedback(
            responseId,
            FeedbackRequest(feedback = feedback, isFavorite = isFavorite)
        )
        if (!response.isSuccessful) throw Exception(parseApiError(response.errorBody()?.string()))
        response.body() ?: throw Exception("Пустой ответ")
    }
}
