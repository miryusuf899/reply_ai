from django.contrib import admin
from .models import UserSetting, SupportedLanguage, PromptTemplate


@admin.register(UserSetting)
class UserSettingAdmin(admin.ModelAdmin):
    list_display = [
        'user', 'default_tone', 'default_response_language',
        'default_input_language', 'auto_detect_language', 'save_history'
    ]
    list_filter = ['default_tone', 'default_response_language', 'auto_detect_language']
    search_fields = ['user__email']
    readonly_fields = ['id', 'created_at', 'updated_at']
    autocomplete_fields = ['user']

    fieldsets = (
        ('Пользователь', {'fields': ('id', 'user')}),
        ('Язык', {'fields': (
            'default_input_language',
            'default_response_language',
            'auto_detect_language',
        )}),
        ('Поведение', {'fields': (
            'default_tone',
            'preferred_messenger',
            'save_history',
        )}),
        ('Даты', {'fields': ('created_at', 'updated_at')}),
    )


@admin.register(SupportedLanguage)
class SupportedLanguageAdmin(admin.ModelAdmin):
    list_display = ['code', 'name', 'native_name', 'is_active']
    list_filter = ['is_active']
    search_fields = ['code', 'name', 'native_name']
    ordering = ['name']


@admin.register(PromptTemplate)
class PromptTemplateAdmin(admin.ModelAdmin):
    list_display = [
        'template_type', 'tone', 'language',
        'version', 'is_active', 'updated_at'
    ]
    list_filter = ['template_type', 'tone', 'language', 'is_active']
    search_fields = ['system_prompt']
    readonly_fields = ['id', 'created_at', 'updated_at']

    fieldsets = (
        ('Основное', {'fields': ('id', 'template_type', 'tone', 'language', 'version', 'is_active')}),
        ('Промпт', {'fields': ('system_prompt',)}),
        ('Даты', {'fields': ('created_at', 'updated_at')}),
    )