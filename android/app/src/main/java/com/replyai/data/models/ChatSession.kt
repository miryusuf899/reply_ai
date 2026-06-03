package com.replyai.data.models

import com.google.gson.annotations.SerializedName

data class ChatSession(
    val id: String,
    val messenger: String,
    @SerializedName("messenger_display") val messengerDisplay: String? = null,
    val title: String? = null,
    @SerializedName("context_summary") val contextSummary: String? = null,
    @SerializedName("is_favorite") val isFavorite: Boolean = false,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("request_count") val requestCount: Int = 0,
    val messages: List<ChatMessage>? = null,
    @SerializedName("ai_requests") val aiRequests: List<AIRequest>? = null
)

data class CreateSessionRequest(
    val messenger: String,
    val title: String = ""
)

data class ChatMessage(
    val id: String? = null,
    val sender: String,
    val content: String,
    @SerializedName("original_language") val originalLanguage: String? = null,
    val timestamp: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class AIRequest(
    val id: String? = null,
    @SerializedName("request_type") val requestType: String? = null,
    @SerializedName("user_prompt") val userPrompt: String? = null,
    @SerializedName("target_language") val targetLanguage: String? = null,
    val tone: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val response: AIResponse? = null
)

data class AIResponse(
    val id: String? = null,
    @SerializedName("generated_text") val generatedText: String,
    @SerializedName("model_used") val modelUsed: String? = null,
    @SerializedName("tokens_used") val tokensUsed: Int? = null,
    @SerializedName("generation_time_ms") val generationTimeMs: Int? = null,
    @SerializedName("is_favorite") val isFavorite: Boolean = false,
    val feedback: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

/**
 * POST /api/chats/sessions/{uuid}/ask/
 *
 * {
 *   "request_type": "reply_help",
 *   "user_prompt": "...",
 *   "tone": "formal" | "friendly" | "empathic"
 * }
 */
data class AskRequest(
    @SerializedName("request_type")
    val requestType: String = REQUEST_TYPE_REPLY_HELP,
    @SerializedName("user_prompt")
    val userPrompt: String,
    val tone: String
) {
    companion object {
        const val REQUEST_TYPE_REPLY_HELP = "reply_help"
        const val REQUEST_TYPE_TRANSLATION = "translation"
        const val REQUEST_TYPE_REWRITE = "rewrite"
        const val REQUEST_TYPE_TONE_CHANGE = "tone_change"
        const val REQUEST_TYPE_ANALYZE = "analyze"
    }
}

data class ApiError(
    val detail: String? = null,
    val message: String? = null
)
