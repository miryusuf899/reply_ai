from django.urls import path
from .views import (
    ChatSessionListCreateView,
    ChatSessionDetailView,
    FavoriteChatListView,
    ToggleFavoriteView,
    ChatMessageListCreateView,
    AIRequestCreateView,
    AIResponseFeedbackView,
    FavoriteResponseListView,
)

urlpatterns = [
    # Sessions
    path('sessions/', ChatSessionListCreateView.as_view(), name='session-list'),
    path('sessions/<uuid:pk>/', ChatSessionDetailView.as_view(), name='session-detail'),
    path('sessions/favorites/', FavoriteChatListView.as_view(), name='session-favorites'),
    path('sessions/<uuid:pk>/toggle-favorite/', ToggleFavoriteView.as_view(), name='toggle-favorite'),

    # Messages
    path('sessions/<uuid:session_pk>/messages/', ChatMessageListCreateView.as_view(), name='message-list'),

    # AI
    path('sessions/<uuid:session_pk>/ask/', AIRequestCreateView.as_view(), name='ai-ask'),
    path('responses/<uuid:pk>/feedback/', AIResponseFeedbackView.as_view(), name='response-feedback'),
    path('responses/favorites/', FavoriteResponseListView.as_view(), name='response-favorites'),
]