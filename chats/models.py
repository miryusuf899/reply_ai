import uuid
from django.db import models
from django.utils import timezone
from django.conf import settings


class MessengerType(models.TextChoices):
    TELEGRAM = 'telegram', 'Telegram'
    INSTAGRAM = 'instagram', 'Instagram'
    WHATSAPP = 'whatsapp', 'WhatsApp'


class ChatSession(models.Model):
    """
    Одна сессия = один чат в мессенджере.
    Пользователь открыл чат, включил программу — создаётся сессия.
    Автоматически удаляется через CHAT_AUTO_DELETE_DAYS дней (Celery).
    Если is_favorite=True — не удаляется.
    """
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='chat_sessions'
    )
    messenger = models.CharField(max_length=20, choices=MessengerType.choices)
    title = models.CharField(max_length=255, blank=True)  # название чата если есть
    context_summary = models.TextField(blank=True)  # краткое саммари чата от AI
    is_favorite = models.BooleanField(default=False)  # избранное — не удаляется
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    delete_after = models.DateTimeField(null=True, blank=True)  # когда удалить

    class Meta:
        db_table = 'chat_sessions'
        verbose_name = 'Сессия чата'
        verbose_name_plural = 'Сессии чатов'
        ordering = ['-updated_at']
        indexes = [
            models.Index(fields=['user', 'is_favorite']),
            models.Index(fields=['delete_after']),
        ]

    def save(self, *args, **kwargs):
        # Автоматически ставим дату удаления при создании
        if not self.delete_after and not self.is_favorite:
            from datetime import timedelta
            self.delete_after = timezone.now() + timedelta(
                days=settings.CHAT_AUTO_DELETE_DAYS
            )
        # Если пометили как избранное — снимаем дату удаления
        if self.is_favorite:
            self.delete_after = None
        super().save(*args, **kwargs)

    def __str__(self):
        return f'{self.user.email} | {self.messenger} | {self.created_at.date()}'


class ChatMessage(models.Model):
    """
    Сообщения из реального чата (то что AI анализирует).
    Это НЕ сообщения пользователя к нам — это содержимое переписки в мессенджере.
    """
    class SenderType(models.TextChoices):
        ME = 'me', 'Я'
        OTHER = 'other', 'Собеседник'

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    session = models.ForeignKey(ChatSession, on_delete=models.CASCADE, related_name='messages')
    sender = models.CharField(max_length=10, choices=SenderType.choices)
    content = models.TextField()
    original_language = models.CharField(max_length=10, blank=True)  # определяется AI
    timestamp = models.DateTimeField(default=timezone.now)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'chat_messages'
        verbose_name = 'Сообщение чата'
        verbose_name_plural = 'Сообщения чата'
        ordering = ['timestamp']
        indexes = [
            models.Index(fields=['session', 'timestamp']),
        ]

    def __str__(self):
        return f'[{self.sender}] {self.content[:50]}'


class AIRequest(models.Model):
    """
    Запрос пользователя к AI.
    Пример: "не знаю как ответить на агрессию" или "переведи на английский"
    """
    class RequestType(models.TextChoices):
        REPLY_HELP = 'reply_help', 'Помощь с ответом'
        TRANSLATION = 'translation', 'Перевод'
        REWRITE = 'rewrite', 'Переписать'
        TONE_CHANGE = 'tone_change', 'Изменить тон'
        ANALYZE = 'analyze', 'Анализ чата'

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    session = models.ForeignKey(ChatSession, on_delete=models.CASCADE, related_name='ai_requests')
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='ai_requests'
    )
    request_type = models.CharField(max_length=20, choices=RequestType.choices)
    user_prompt = models.TextField()  # что написал пользователь в инпуте
    target_language = models.CharField(max_length=10, blank=True)  # язык ответа
    tone = models.CharField(max_length=20, blank=True)  # формальный / дружеский / нейтральный
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'ai_requests'
        verbose_name = 'AI Запрос'
        verbose_name_plural = 'AI Запросы'
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['user', 'created_at']),
            models.Index(fields=['session']),
        ]

    def __str__(self):
        return f'{self.request_type} | {self.user.email} | {self.created_at.date()}'


class AIResponse(models.Model):
    """
    Ответ AI на запрос пользователя.
    Один запрос — один ответ.
    Пользователь может поставить лайк/дизлайк и сохранить в избранное.
    """
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    request = models.OneToOneField(AIRequest, on_delete=models.CASCADE, related_name='response')
    generated_text = models.TextField()  # готовый текст для отправки
    model_used = models.CharField(max_length=50, default='gemini-pro')
    tokens_used = models.PositiveIntegerField(default=0)
    generation_time_ms = models.PositiveIntegerField(default=0)  # время генерации в мс
    is_favorite = models.BooleanField(default=False)
    feedback = models.SmallIntegerField(
        null=True,
        blank=True,
        choices=[(1, '👍 Хорошо'), (-1, '👎 Плохо')]
    )
    feedback_comment = models.CharField(max_length=255, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'ai_responses'
        verbose_name = 'AI Ответ'
        verbose_name_plural = 'AI Ответы'
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['is_favorite']),
        ]

    def __str__(self):
        return f'Response to {self.request.request_type} | {self.created_at.date()}'