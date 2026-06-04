package com.replyai.data.models

import com.google.gson.annotations.SerializedName

data class ChatSession(
    val id: String,
    val messenger: String,
    @SerializedName("messenger_display") val messengerDisplay: String? = null,
    val title: String? = null,
    @SerializedName("context_summary") val contextSummary: String? = null,
    @SerializedName("is_favorite") val isFavorite: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("request_count") val requestCount: Int = 0,
    val messages: List<ChatMessage>? = null,
    @SerializedName("ai_requests") val aiRequests: List<AIRequestResponse>? = null
)

data class CreateSessionRequest(
    val messenger: String,
    val title: String = ""
)

data class SessionPatch(
    val title: String? = null,
    @SerializedName("is_favorite") val isFavorite: Boolean? = null
)

data class ToggleFavoriteResponse(
    @SerializedName("is_favorite") val isFavorite: Boolean,
    val detail: String? = null
)

data class ChatMessage(
    val id: String? = null,
    val sender: String,
    val content: String,
    val timestamp: String? = null
)

data class CreateMessageRequest(
    val sender: String,
    val content: String,
    val timestamp: String? = null
)

/** @see AIRequestResponse */
typealias AIRequest = AIRequestResponse

/** @see AIResponseDto */
typealias AIResponse = AIResponseDto
