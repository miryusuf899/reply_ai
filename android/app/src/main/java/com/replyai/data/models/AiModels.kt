package com.replyai.data.models

import com.google.gson.annotations.SerializedName

/**
 * Mirrors backend AIRequest.RequestType choices exactly.
 */
object RequestTypes {
    const val REPLY_HELP = "reply_help"
    const val TRANSLATION = "translation"
    const val REWRITE = "rewrite"
    const val TONE_CHANGE = "tone_change"
    const val ANALYZE = "analyze"

    private val ALLOWED = setOf(
        REPLY_HELP, TRANSLATION, REWRITE, TONE_CHANGE, ANALYZE
    )

    fun toApiValue(value: String): String {
        val normalized = value.trim().lowercase()
        return when (normalized) {
            "reply", "reply_help" -> REPLY_HELP
            "translation" -> TRANSLATION
            "rewrite" -> REWRITE
            "tone_change" -> TONE_CHANGE
            "analyze" -> ANALYZE
            else -> if (normalized in ALLOWED) normalized else REPLY_HELP
        }
    }
}

/**
 * Mirrors backend ToneChoice / AIRequestCreateSerializer.validate_tone exactly.
 */
object ToneChoices {
    const val FORMAL = "formal"
    const val FRIENDLY = "friendly"
    const val NEUTRAL = "neutral"
    const val ASSERTIVE = "assertive"
    const val EMPATHETIC = "empathetic"
    const val EMPTY = ""

    private val ALLOWED = setOf(FORMAL, FRIENDLY, NEUTRAL, ASSERTIVE, EMPATHETIC, EMPTY)

    /** Maps UI label or legacy key to exact API tone string. */
    fun toApiValue(uiOrApi: String): String {
        return when (uiOrApi.trim().lowercase()) {
            "formal" -> FORMAL
            "friendly" -> FRIENDLY
            "neutral" -> NEUTRAL
            "assertive" -> ASSERTIVE
            "empathic", "empathetic" -> EMPATHETIC
            "" -> EMPTY
            else -> if (uiOrApi.trim().lowercase() in ALLOWED) uiOrApi.trim().lowercase() else FRIENDLY
        }
    }
}

/**
 * POST /api/chats/sessions/{session_pk}/ask/
 * Matches AIRequestCreateSerializer fields exactly.
 */
data class AskRequest(
    @SerializedName("request_type")
    val requestType: String,
    @SerializedName("user_prompt")
    val userPrompt: String,
    @SerializedName("target_language")
    val targetLanguage: String,
    val tone: String
) {
    companion object {
        fun create(
            userPrompt: String,
            requestType: String = RequestTypes.REPLY_HELP,
            tone: String = ToneChoices.FRIENDLY,
            targetLanguage: String = "ru"
        ): AskRequest = AskRequest(
            requestType = RequestTypes.toApiValue(requestType),
            userPrompt = userPrompt.trim(),
            targetLanguage = targetLanguage.ifBlank { "ru" },
            tone = ToneChoices.toApiValue(tone)
        )
    }
}

/**
 * AIResponseSerializer — nested under AIRequestSerializer as "response".
 */
data class AIResponseDto(
    val id: String? = null,
    @SerializedName("generated_text")
    val generatedText: String? = null,
    @SerializedName("model_used")
    val modelUsed: String? = null,
    @SerializedName("tokens_used")
    val tokensUsed: Int? = null,
    @SerializedName("generation_time_ms")
    val generationTimeMs: Int? = null,
    @SerializedName("is_favorite")
    val isFavorite: Boolean = false,
    val feedback: Int? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
)

/**
 * AIRequestSerializer — 201 Created body from /ask/.
 */
data class AIRequestResponse(
    val id: String? = null,
    @SerializedName("request_type")
    val requestType: String? = null,
    @SerializedName("user_prompt")
    val userPrompt: String? = null,
    @SerializedName("target_language")
    val targetLanguage: String? = null,
    val tone: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    val response: AIResponseDto? = null
) {
    /** Safe access to response.generated_text per backend contract. */
    fun extractGeneratedText(): String? =
        response?.generatedText?.trim()?.takeIf { it.isNotEmpty() }
}

data class FeedbackRequest(
    val feedback: Int? = null,
    @SerializedName("is_favorite")
    val isFavorite: Boolean? = null
)

data class FavoriteAIResponse(
    val id: String,
    @SerializedName("generated_text")
    val generatedText: String,
    @SerializedName("is_favorite")
    val isFavorite: Boolean,
    val feedback: Int?,
    @SerializedName("created_at")
    val createdAt: String? = null
)
