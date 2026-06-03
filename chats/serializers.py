from rest_framework import serializers
from .models import ChatSession, ChatMessage, AIRequest, AIResponse


class ChatMessageSerializer(serializers.ModelSerializer):
    class Meta:
        model = ChatMessage
        fields = [
            'id', 'sender', 'content',
            'original_language', 'timestamp', 'created_at'
        ]
        read_only_fields = ['id', 'original_language', 'created_at']


class ChatMessageCreateSerializer(serializers.ModelSerializer):
    """Для добавления сообщений в сессию"""
    class Meta:
        model = ChatMessage
        fields = ['sender', 'content', 'timestamp']

    def create(self, validated_data):
        session = self.context['session']
        return ChatMessage.objects.create(session=session, **validated_data)


class AIResponseSerializer(serializers.ModelSerializer):
    class Meta:
        model = AIResponse
        fields = [
            'id', 'generated_text', 'model_used', 'tokens_used',
            'generation_time_ms', 'is_favorite', 'feedback',
            'feedback_comment', 'created_at'
        ]
        read_only_fields = [
            'id', 'generated_text', 'model_used',
            'tokens_used', 'generation_time_ms', 'created_at'
        ]


class AIResponseFeedbackSerializer(serializers.ModelSerializer):
    """Только для лайка/дизлайка и избранного"""
    class Meta:
        model = AIResponse
        fields = ['is_favorite', 'feedback', 'feedback_comment']


class AIRequestSerializer(serializers.ModelSerializer):
    response = AIResponseSerializer(read_only=True)

    class Meta:
        model = AIRequest
        fields = [
            'id', 'request_type', 'user_prompt', 'target_language',
            'tone', 'created_at', 'response'
        ]
        read_only_fields = ['id', 'created_at', 'response']


class AIRequestCreateSerializer(serializers.ModelSerializer):
    """
    Создание запроса к AI.
    session и user берутся из контекста — пользователь их не передаёт.
    """
    class Meta:
        model = AIRequest
        fields = ['request_type', 'user_prompt', 'target_language', 'tone']

    def validate_tone(self, value):
        allowed = ['formal', 'friendly', 'neutral', 'assertive', 'empathetic', '']
        if value not in allowed:
            raise serializers.ValidationError(f'Тон должен быть одним из: {allowed}')
        return value

    def create(self, validated_data):
        session = self.context['session']
        user = self.context['request'].user
        return AIRequest.objects.create(
            session=session,
            user=user,
            **validated_data
        )


class ChatSessionSerializer(serializers.ModelSerializer):
    """Список сессий — без сообщений (легко грузится)"""
    messenger_display = serializers.CharField(
        source='get_messenger_display',
        read_only=True
    )
    request_count = serializers.SerializerMethodField()

    class Meta:
        model = ChatSession
        fields = [
            'id', 'messenger', 'messenger_display', 'title',
            'context_summary', 'is_favorite', 'is_active',
            'delete_after', 'created_at', 'updated_at', 'request_count'
        ]
        read_only_fields = [
            'id', 'context_summary', 'delete_after',
            'created_at', 'updated_at'
        ]

    def get_request_count(self, obj):
        return obj.ai_requests.count()


class ChatSessionDetailSerializer(serializers.ModelSerializer):
    """Детальная сессия — со всеми сообщениями и запросами"""
    messages = ChatMessageSerializer(many=True, read_only=True)
    ai_requests = AIRequestSerializer(many=True, read_only=True)
    messenger_display = serializers.CharField(
        source='get_messenger_display',
        read_only=True
    )

    class Meta:
        model = ChatSession
        fields = [
            'id', 'messenger', 'messenger_display', 'title',
            'context_summary', 'is_favorite', 'is_active',
            'delete_after', 'created_at', 'updated_at',
            'messages', 'ai_requests'
        ]
        read_only_fields = [
            'id', 'context_summary', 'delete_after',
            'created_at', 'updated_at'
        ]


class ChatSessionCreateSerializer(serializers.ModelSerializer):
    """Создание новой сессии"""
    class Meta:
        model = ChatSession
        fields = ['messenger', 'title']

    def create(self, validated_data):
        user = self.context['request'].user
        return ChatSession.objects.create(user=user, **validated_data)