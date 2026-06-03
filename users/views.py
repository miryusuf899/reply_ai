from rest_framework import generics, status, permissions
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework_simplejwt.tokens import RefreshToken
from django.contrib.auth import get_user_model
from drf_spectacular.utils import extend_schema, extend_schema_view

from .models import SubscriptionPlan, UserSubscription, UsageCounter
from .serializers import (
    UserSerializer,
    RegisterSerializer,
    ChangePasswordSerializer,
    SubscriptionPlanSerializer,
    UserSubscriptionSerializer,
    UsageCounterSerializer,
)

User = get_user_model()


@extend_schema(tags=['Auth'])
class RegisterView(generics.CreateAPIView):
    """Регистрация нового пользователя"""
    serializer_class = RegisterSerializer
    permission_classes = [permissions.AllowAny]

    def create(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = serializer.save()

        refresh = RefreshToken.for_user(user)
        return Response({
            'user': UserSerializer(user).data,
            'tokens': {
                'refresh': str(refresh),
                'access': str(refresh.access_token),
            }
        }, status=status.HTTP_201_CREATED)


@extend_schema(tags=['Auth'])
class LogoutView(APIView):
    """Выход — блокируем refresh token"""
    permission_classes = [permissions.IsAuthenticated]

    def post(self, request):
        try:
            refresh_token = request.data['refresh']
            token = RefreshToken(refresh_token)
            token.blacklist()
            return Response({'detail': 'Вы вышли из системы'}, status=status.HTTP_200_OK)
        except Exception:
            return Response({'detail': 'Неверный токен'}, status=status.HTTP_400_BAD_REQUEST)


@extend_schema(tags=['Profile'])
class ProfileView(generics.RetrieveUpdateAPIView):
    """Просмотр и редактирование профиля"""
    serializer_class = UserSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_object(self):
        return self.request.user


@extend_schema(tags=['Profile'])
class ChangePasswordView(APIView):
    """Смена пароля"""
    permission_classes = [permissions.IsAuthenticated]

    def post(self, request):
        serializer = ChangePasswordSerializer(
            data=request.data,
            context={'request': request}
        )
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response({'detail': 'Пароль успешно изменён'})


@extend_schema(tags=['Profile'])
class DeleteAccountView(APIView):
    """Удаление аккаунта"""
    permission_classes = [permissions.IsAuthenticated]

    def delete(self, request):
        user = request.user
        user.delete()
        return Response({'detail': 'Аккаунт удалён'}, status=status.HTTP_204_NO_CONTENT)


@extend_schema(tags=['Subscription'])
class SubscriptionPlanListView(generics.ListAPIView):
    """Список всех тарифных планов"""
    serializer_class = SubscriptionPlanSerializer
    permission_classes = [permissions.AllowAny]
    queryset = SubscriptionPlan.objects.filter(is_active=True)


@extend_schema(tags=['Subscription'])
class MySubscriptionView(generics.RetrieveAPIView):
    """Моя текущая подписка"""
    serializer_class = UserSubscriptionSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_object(self):
        subscription, created = UserSubscription.objects.get_or_create(
            user=self.request.user,
            defaults={
                'plan': SubscriptionPlan.objects.get(plan_type='free')
            }
        )
        return subscription


@extend_schema(tags=['Subscription'])
class MyUsageView(generics.RetrieveAPIView):
    """Мой счётчик использования"""
    serializer_class = UsageCounterSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_object(self):
        counter, created = UsageCounter.objects.get_or_create(
            user=self.request.user
        )
        return counter