from django.contrib import admin
from .models import ChatSession, ChatMessage, AIRequest, AIResponse


class ChatMessageInline(admin.TabularInline):
    model = ChatMessage
    extra = 0
    readonly_fields = ['id', 'created_at']
    fields = ['sender', 'content', 'original_language', 'timestamp']
    ordering = ['timestamp']


class AIRequestInline(admin.TabularInline):
    model = AIRequest
    extra = 0
    readonly_fields = ['id', 'created_at']
    fields = ['request_type', 'user_prompt', 'tone', 'target_language', 'created_at']
    show_change_link = True


@admin.register(ChatSession)
class ChatSessionAdmin(admin.ModelAdmin):
    list_display = [
        'id', 'user', 'messenger', 'title',
        'is_favorite', 'is_active', 'delete_after', 'created_at'
    ]
    list_filter = ['messenger', 'is_favorite', 'is_active', 'created_at']
    search_fields = ['user__email', 'title']
    readonly_fields = ['id', 'created_at', 'updated_at']
    autocomplete_fields = ['user']
    inlines = [ChatMessageInline, AIRequestInline]

    fieldsets = (
        ('Основное', {'fields': ('id', 'user', 'messenger', 'title')}),
        ('AI Контекст', {'fields': ('context_summary',)}),
        ('Статус', {'fields': ('is_favorite', 'is_active', 'delete_after')}),
        ('Даты', {'fields': ('created_at', 'updated_at')}),
    )

    actions = ['mark_as_favorite', 'unmark_as_favorite']

    @admin.action(description='Добавить в избранное')
    def mark_as_favorite(self, request, queryset):
        queryset.update(is_favorite=True, delete_after=None)

    @admin.action(description='Убрать из избранного')
    def unmark_as_favorite(self, request, queryset):
        from django.utils import timezone
        from datetime import timedelta
        from django.conf import settings
        queryset.update(
            is_favorite=False,
            delete_after=timezone.now() + timedelta(days=settings.CHAT_AUTO_DELETE_DAYS)
        )


@admin.register(ChatMessage)
class ChatMessageAdmin(admin.ModelAdmin):
    list_display = ['session', 'sender', 'short_content', 'original_language', 'timestamp']
    list_filter = ['sender', 'original_language', 'timestamp']
    search_fields = ['session__user__email', 'content']
    readonly_fields = ['id', 'created_at']

    @admin.display(description='Содержимое')
    def short_content(self, obj):
        return obj.content[:60] + '...' if len(obj.content) > 60 else obj.content


class AIResponseInline(admin.StackedInline):
    model = AIResponse
    extra = 0
    readonly_fields = ['id', 'created_at', 'tokens_used', 'generation_time_ms']
    fields = [
        'generated_text', 'model_used', 'tokens_used',
        'generation_time_ms', 'is_favorite', 'feedback', 'feedback_comment'
    ]


@admin.register(AIRequest)
class AIRequestAdmin(admin.ModelAdmin):
    list_display = [
        'id', 'user', 'request_type', 'tone',
        'target_language', 'created_at'
    ]
    list_filter = ['request_type', 'tone', 'target_language', 'created_at']
    search_fields = ['user__email', 'user_prompt']
    readonly_fields = ['id', 'created_at']
    autocomplete_fields = ['user']
    inlines = [AIResponseInline]

    fieldsets = (
        ('Основное', {'fields': ('id', 'user', 'session', 'request_type')}),
        ('Запрос', {'fields': ('user_prompt', 'tone', 'target_language')}),
        ('Даты', {'fields': ('created_at',)}),
    )


@admin.register(AIResponse)
class AIResponseAdmin(admin.ModelAdmin):
    list_display = [
        'id', 'get_user', 'get_request_type', 'model_used',
        'tokens_used', 'generation_time_ms', 'is_favorite', 'feedback', 'created_at'
    ]
    list_filter = ['is_favorite', 'feedback', 'model_used', 'created_at']
    search_fields = ['request__user__email', 'generated_text']
    readonly_fields = ['id', 'created_at', 'tokens_used', 'generation_time_ms']

    fieldsets = (
        ('Основное', {'fields': ('id', 'request', 'model_used')}),
        ('Ответ', {'fields': ('generated_text',)}),
        ('Метрики', {'fields': ('tokens_used', 'generation_time_ms')}),
        ('Обратная связь', {'fields': ('is_favorite', 'feedback', 'feedback_comment')}),
        ('Даты', {'fields': ('created_at',)}),
    )

    @admin.display(description='Пользователь')
    def get_user(self, obj):
        return obj.request.user.email

    @admin.display(description='Тип запроса')
    def get_request_type(self, obj):
        return obj.request.get_request_type_display()