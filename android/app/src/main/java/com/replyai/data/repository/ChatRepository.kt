package com.replyai.data.repository

import com.replyai.data.api.RetrofitClient
import com.replyai.data.models.AIRequest
import com.replyai.data.models.AskRequest
import com.replyai.data.models.ChatSession
import com.replyai.data.models.CreateSessionRequest
import com.replyai.utils.toApiTone

class ChatRepository {

    private val api = RetrofitClient.api

    suspend fun getSessions(): Result<List<ChatSession>> = runCatching {
        val response = api.getSessions()
        if (!response.isSuccessful) {
            throw Exception(parseError(response.errorBody()?.string()))
        }
        response.body() ?: emptyList()
    }

    suspend fun createSession(messenger: String, title: String): Result<ChatSession> = runCatching {
        val response = api.createSession(CreateSessionRequest(messenger, title))
        if (!response.isSuccessful) {
            throw Exception(parseError(response.errorBody()?.string()))
        }
        response.body() ?: throw Exception("Empty response")
    }

    suspend fun getSession(uuid: String): Result<ChatSession> = runCatching {
        val response = api.getSession(uuid)
        if (!response.isSuccessful) {
            throw Exception(parseError(response.errorBody()?.string()))
        }
        response.body() ?: throw Exception("Empty response")
    }

    suspend fun deleteSession(uuid: String): Result<Unit> = runCatching {
        val response = api.deleteSession(uuid)
        if (!response.isSuccessful) {
            throw Exception(parseError(response.errorBody()?.string()))
        }
    }

    /**
     * POST /api/chats/sessions/{uuid}/ask/
     * Payload: { request_type, user_prompt, tone }
     */
    suspend fun askAI(
        sessionId: String,
        userPrompt: String,
        tone: String,
        requestType: String = AskRequest.REQUEST_TYPE_REPLY_HELP
    ): Result<AIRequest> = runCatching {
        val response = api.askAI(
            sessionId,
            AskRequest(
                requestType = requestType,
                userPrompt = userPrompt,
                tone = tone.toApiTone()
            )
        )
        if (!response.isSuccessful) {
            throw Exception(parseError(response.errorBody()?.string()))
        }
        response.body() ?: throw Exception("Empty response")
    }

    private fun parseError(body: String?): String {
        if (body.isNullOrBlank()) return "Request failed"
        return try {
            val fieldErrors = Regex(""""(\w+)"\s*:\s*\["([^"]+)"/""").findAll(body)
                .map { "${it.groupValues[1]}: ${it.groupValues[2]}" }
                .joinToString("; ")
            if (fieldErrors.isNotBlank()) return fieldErrors
            val detail = Regex(""""detail"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1)
            detail ?: body.take(200)
        } catch (_: Exception) {
            body.take(200)
        }
    }
}
