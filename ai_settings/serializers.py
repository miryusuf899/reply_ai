from rest_framework import serializers
from .models import UserSetting, SupportedLanguage, PromptTemplate


class UserSettingSerializer(serializers.ModelSerializer):
    class Meta:
        model = UserSetting
        fields = [
            'id', 'default_tone', 'default_response_language',
            'default_input_language', 'preferred_messenger',
            'auto_detect_language', 'save_history',
            'created_at', 'updated_at'
        ]
        read_only_fields = ['id', 'created_at', 'updated_at']

    def validate_default_response_language(self, value):
        """Проверяем что язык есть в нашем списке"""
        if not SupportedLanguage.objects.filter(code=value, is_active=True).exists():
            raise serializers.ValidationError(
                f'Язык "{value}" не поддерживается'
            )
        return value


class SupportedLanguageSerializer(serializers.ModelSerializer):
    class Meta:
        model = SupportedLanguage
        fields = ['code', 'name', 'native_name']


class PromptTemplateSerializer(serializers.ModelSerializer):
    """Только для чтения — промпты редактируются через admin"""
    class Meta:
        model = PromptTemplate
        fields = [
            'id', 'template_type', 'tone',
            'language', 'version', 'created_at'
        ]
        read_only_fields = fields