import uuid
from django.db import models
from django.conf import settings


class ToneChoice(models.TextChoices):
    FORMAL = 'formal', 'Формальный'
    FRIENDLY = 'friendly', 'Дружеский'
    NEUTRAL = 'neutral', 'Нейтральный'
    ASSERTIVE = 'assertive', 'Уверенный'
    EMPATHETIC = 'empathetic', 'Эмпатичный'


class UserSetting(models.Model):
    """
    Настройки пользователя.
    Один пользователь — одна запись настроек (OneToOne).
    Создаётся автоматически при регистрации через сигнал.
    """
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.OneToOneField(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='settings'
    )
    default_tone = models.CharField(
        max_length=20,
        choices=ToneChoice.choices,
        default=ToneChoice.NEUTRAL
    )
    default_response_language = models.CharField(max_length=10, default='ru')
    default_input_language = models.CharField(max_length=10, default='auto')  # auto = определять автоматически
    preferred_messenger = models.CharField(
        max_length=20,
        choices=[
            ('telegram', 'Telegram'),
            ('instagram', 'Instagram'),
            ('whatsapp', 'WhatsApp'),
        ],
        blank=True
    )
    auto_detect_language = models.BooleanField(default=True)
    save_history = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'user_settings'
        verbose_name = 'Настройки пользователя'
        verbose_name_plural = 'Настройки пользователей'

    def __str__(self):
        return f'Settings of {self.user.email}'


class SupportedLanguage(models.Model):
    """
    Список поддерживаемых языков.
    Заполняется через фикстуры — легко расширять.
    """
    code = models.CharField(max_length=10, unique=True)  # ru, en, uz, tj, de...
    name = models.CharField(max_length=50)
    native_name = models.CharField(max_length=50)  # Русский, English, O'zbek...
    is_active = models.BooleanField(default=True)

    class Meta:
        db_table = 'supported_languages'
        verbose_name = 'Язык'
        verbose_name_plural = 'Языки'
        ordering = ['name']

    def __str__(self):
        return f'{self.native_name} ({self.code})'


class PromptTemplate(models.Model):
    """
    Системные промпты для разных типов запросов.
    Хранятся в БД — можно редактировать через admin без деплоя.
    """
    class TemplateType(models.TextChoices):
        REPLY_HELP = 'reply_help', 'Помощь с ответом'
        TRANSLATION = 'translation', 'Перевод'
        REWRITE = 'rewrite', 'Переписать'
        ANALYZE = 'analyze', 'Анализ контекста'
        TONE_CHANGE = 'tone_change', 'Изменить тон'

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    template_type = models.CharField(max_length=20, choices=TemplateType.choices)
    tone = models.CharField(
        max_length=20,
        choices=ToneChoice.choices,
        blank=True  # пусто = применяется ко всем тонам
    )
    language = models.CharField(max_length=10, default='ru')
    system_prompt = models.TextField()
    is_active = models.BooleanField(default=True)
    version = models.PositiveSmallIntegerField(default=1)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'prompt_templates'
        verbose_name = 'Промпт шаблон'
        verbose_name_plural = 'Промпт шаблоны'
        unique_together = [['template_type', 'tone', 'language', 'version']]

    def __str__(self):
        return f'{self.template_type} | {self.tone or "any"} | {self.language} v{self.version}'