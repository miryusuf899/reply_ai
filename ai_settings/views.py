from rest_framework import generics, permissions
from drf_spectacular.utils import extend_schema

from .models import UserSetting, SupportedLanguage
from .serializers import (
    UserSettingSerializer,
    SupportedLanguageSerializer,
)


@extend_schema(tags=['Settings'])
class UserSettingView(generics.RetrieveUpdateAPIView):
    """Настройки пользователя — получить и обновить"""
    serializer_class = UserSettingSerializer
    permission_classes = [permissions.IsAuthenticated]
    http_method_names = ['get', 'patch']

    def get_object(self):
        setting, created = UserSetting.objects.get_or_create(
            user=self.request.user
        )
        return setting


@extend_schema(tags=['Settings'])
class SupportedLanguageListView(generics.ListAPIView):
    """Список поддерживаемых языков"""
    serializer_class = SupportedLanguageSerializer
    permission_classes = [permissions.AllowAny]
    queryset = SupportedLanguage.objects.filter(is_active=True)