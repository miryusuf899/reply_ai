from rest_framework import generics, status, permissions
from rest_framework.response import Response
from rest_framework.views import APIView
from django.shortcuts import get_object_or_404
from drf_spectacular.utils import extend_schema
from drf_spectacular.utils import extend_schema, OpenApiParameter

from .models import ChatSession, ChatMessage, AIRequest, AIResponse
from .serializers import (
    ChatSessionSerializer,
    ChatSessionDetailSerializer,
    ChatSessionCreateSerializer,
    ChatMessageSerializer,
    ChatMessageCreateSerializer,
    AIRequestCreateSerializer,
    AIRequestSerializer,
    AIResponseFeedbackSerializer,
)
from services.gemini import GeminiService


@extend_schema(tags=['Chats'])
class ChatSessionListCreateView(generics.ListCreateAPIView):
    """Список сессий и создание новой"""
    permission_classes = [permissions.IsAuthenticated]

    def get_serializer_class(self):
        if self.request.method == 'POST':
            return ChatSessionCreateSerializer
        return ChatSessionSerializer

    def get_queryset(self):
        return ChatSession.objects.filter(
            user=self.request.user,
            is_active=True
        ).order_by('-updated_at')

    def create(self, request, *args, **kwargs):
        serializer = ChatSessionCreateSerializer(
            data=request.data,
            context={'request': request}
        )
        serializer.is_valid(raise_exception=True)
        session = serializer.save()
        return Response(
            ChatSessionSerializer(session).data,
            status=status.HTTP_201_CREATED
        )


@extend_schema(tags=['Chats'])
class ChatSessionDetailView(generics.RetrieveUpdateDestroyAPIView):
    """Детали, редактирование и удаление сессии"""
    permission_classes = [permissions.IsAuthenticated]

    def get_serializer_class(self):
        if self.request.method in ['PUT', 'PATCH']:
            return ChatSessionSerializer
        return ChatSessionDetailSerializer

    def get_queryset(self):
        return ChatSession.objects.filter(user=self.request.user)

    def destroy(self, request, *args, **kwargs):
        session = self.get_object()
        session.is_active = False
        session.save()
        return Response({'detail': 'Сессия удалена'}, status=status.HTTP_200_OK)


@extend_schema(tags=['Chats'])
class FavoriteChatListView(generics.ListAPIView):
    """Список избранных сессий"""
    serializer_class = ChatSessionSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        return ChatSession.objects.filter(
            user=self.request.user,
            is_favorite=True
        ).order_by('-updated_at')


@extend_schema(
    tags=['Chats'],
    request=None,
    responses={200: OpenApiParameter(name='is_favorite', type=bool, location='query')},
)
class ToggleFavoriteView(APIView):
    """Добавить/убрать сессию из избранного"""
    permission_classes = [permissions.IsAuthenticated]

    def post(self, request, pk):
        session = get_object_or_404(ChatSession, pk=pk, user=request.user)
        session.is_favorite = not session.is_favorite
        session.save()
        return Response({
            'is_favorite': session.is_favorite,
            'detail': 'Добавлено в избранное' if session.is_favorite else 'Убрано из избранного'
        })


@extend_schema(tags=['Messages'])
class ChatMessageListCreateView(generics.ListCreateAPIView):
    """Сообщения сессии"""
    permission_classes = [permissions.IsAuthenticated]

    def get_serializer_class(self):
        if self.request.method == 'POST':
            return ChatMessageCreateSerializer
        return ChatMessageSerializer

    def get_session(self):
        return get_object_or_404(
            ChatSession,
            pk=self.kwargs['session_pk'],
            user=self.request.user
        )

    def get_queryset(self):
        return ChatMessage.objects.filter(
            session=self.get_session()
        ).order_by('timestamp')

    def create(self, request, *args, **kwargs):
        session = self.get_session()
        serializer = ChatMessageCreateSerializer(
            data=request.data,
            context={'session': session, 'request': request}
        )
        serializer.is_valid(raise_exception=True)
        message = serializer.save()
        return Response(
            ChatMessageSerializer(message).data,
            status=status.HTTP_201_CREATED
        )
@extend_schema(
    tags=['AI'],
    request=AIRequestCreateSerializer,
    responses=AIRequestSerializer,
)
class AIRequestCreateView(APIView):
    """
    Главный эндпоинт — отправить запрос к AI.
    Проверяет лимиты тарифа перед генерацией.
    """

    def get_permissions(self):
        from users.permissions import WithinDailyLimit
        return [permissions.IsAuthenticated(), WithinDailyLimit()]

    def post(self, request, session_pk):
        session = get_object_or_404(
            ChatSession,
            pk=session_pk,
            user=request.user
        )

        serializer = AIRequestCreateSerializer(
            data=request.data,
            context={'session': session, 'request': request}
        )
        serializer.is_valid(raise_exception=True)
        ai_request = serializer.save()

        messages = list(
            session.messages.order_by('timestamp').values('sender', 'content')
        )

        gemini = GeminiService()
        result = gemini.generate_response(
            messages=messages,
            user_prompt=ai_request.user_prompt,
            request_type=ai_request.request_type,
            tone=ai_request.tone,
            target_language=ai_request.target_language,
        )

        AIResponse.objects.create(
            request=ai_request,
            generated_text=result['text'],
            model_used=result['model'],
            tokens_used=result['tokens'],
            generation_time_ms=result['time_ms'],
        )

        try:
            usage = request.user.usage
            usage.daily_count += 1
            usage.monthly_count += 1
            usage.total_requests += 1
            usage.save()
        except Exception:
            pass

        return Response(
            AIRequestSerializer(ai_request).data,
            status=status.HTTP_201_CREATED
        )


@extend_schema(tags=['AI'])
class AIResponseFeedbackView(generics.UpdateAPIView):
    """Лайк/дизлайк и избранное для ответа AI"""
    serializer_class = AIResponseFeedbackSerializer
    permission_classes = [permissions.IsAuthenticated]
    http_method_names = ['patch']

    def get_queryset(self):
        return AIResponse.objects.filter(
            request__user=self.request.user
        )


@extend_schema(tags=['AI'])
class FavoriteResponseListView(generics.ListAPIView):
    """Список избранных ответов AI"""
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        from .models import AIResponse
        return AIResponse.objects.filter(
            request__user=self.request.user,
            is_favorite=True
        ).order_by('-created_at')

    def get_serializer_class(self):
        from .serializers import AIResponseSerializer
        return AIResponseSerializer