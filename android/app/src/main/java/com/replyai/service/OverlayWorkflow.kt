package com.replyai.service

import com.replyai.data.models.RequestTypes
import com.replyai.data.models.ToneChoices
import com.replyai.data.repository.AuthRepository
import com.replyai.data.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class OverlayResult(
    val generatedText: String,
    val responseId: String?,
    val sessionId: String,
    val messagesSynced: Int,
    val inserted: Boolean
)

@Singleton
class OverlayWorkflow @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) {

    /**
     * Automated flow aligned with AIRequestCreateSerializer:
     * - request_type: reply_help | translation | rewrite | tone_change | analyze
     * - user_prompt: overlay input
     * - tone: formal | friendly | neutral | assertive | empathetic
     * - target_language: e.g. ru
     * Response: AIRequestResponse.response.generated_text
     */
    suspend fun run(
        requestType: String,
        userPrompt: String,
        tone: String
    ): Result<OverlayResult> = withContext(Dispatchers.IO) {
        val messageCount = ReplyAIAccessibilityService.forceRefreshMessages()
        val messages = ReplyAIAccessibilityService.getMessagesSnapshot()

        val targetLanguage = authRepository.getSettings()
            .getOrNull()?.defaultLanguage?.ifBlank { "ru" } ?: "ru"

        val messenger = ReplyAIAccessibilityService.currentMessenger
        val title = when (messenger) {
            "telegram" -> "Чат"
            "instagram" -> "Чат Instagram"
            "whatsapp" -> "Чат WhatsApp"
            else -> "Чат"
        }

        val session = chatRepository.createSession(messenger, title).getOrElse {
            return@withContext Result.failure(it)
        }

        val synced = if (messages.isNotEmpty()) {
            chatRepository.syncMessages(session.id, messages).getOrElse { 0 }
        } else {
            0
        }

        val normalizedType = RequestTypes.toApiValue(requestType)
        val normalizedTone = ToneChoices.toApiValue(tone)

        val aiRequest = chatRepository.askAI(
            sessionId = session.id,
            userPrompt = userPrompt,
            requestType = normalizedType,
            tone = normalizedTone,
            targetLanguage = targetLanguage
        ).getOrElse {
            return@withContext Result.failure(it)
        }

        val text = aiRequest.extractGeneratedText()
            ?: return@withContext Result.failure(Exception("Пустой response.generated_text"))

        val inserted = ReplyAIAccessibilityService.insertText(text)

        Result.success(
            OverlayResult(
                generatedText = text,
                responseId = aiRequest.response?.id,
                sessionId = session.id,
                messagesSynced = synced.coerceAtLeast(messageCount),
                inserted = inserted
            )
        )
    }

    fun requestTypeLabel(type: String): String = when (RequestTypes.toApiValue(type)) {
        RequestTypes.REPLY_HELP -> "Помочь ответить"
        RequestTypes.TRANSLATION -> "Перевести текст"
        RequestTypes.TONE_CHANGE -> "Изменить тон"
        RequestTypes.ANALYZE -> "Анализировать чат"
        else -> "AI"
    }
}
