from rest_framework import serializers
from django.contrib.auth import get_user_model
from .models import SubscriptionPlan, UserSubscription, UsageCounter

User = get_user_model()


class UserSerializer(serializers.ModelSerializer):
    """Полная информация о пользователе — для профиля"""
    class Meta:
        model = User
        fields = [
            'id', 'email', 'full_name', 'avatar',
            'is_verified', 'created_at', 'updated_at'
        ]
        read_only_fields = ['id', 'email', 'is_verified', 'created_at', 'updated_at']


class UserShortSerializer(serializers.ModelSerializer):
    """Короткая версия — используется внутри других сериализаторов"""
    class Meta:
        model = User
        fields = ['id', 'email', 'full_name', 'avatar']
        read_only_fields = ['id', 'email']


class RegisterSerializer(serializers.ModelSerializer):
    """Регистрация через email + пароль"""
    password = serializers.CharField(write_only=True, min_length=8)
    password2 = serializers.CharField(write_only=True, min_length=8)

    class Meta:
        model = User
        fields = ['email', 'full_name', 'password', 'password2']

    def validate(self, attrs):
        if attrs['password'] != attrs['password2']:
            raise serializers.ValidationError({'password': 'Пароли не совпадают'})
        return attrs

    def create(self, validated_data):
        validated_data.pop('password2')
        user = User.objects.create_user(**validated_data)
        return user


class ChangePasswordSerializer(serializers.Serializer):
    old_password = serializers.CharField(write_only=True)
    new_password = serializers.CharField(write_only=True, min_length=8)
    new_password2 = serializers.CharField(write_only=True, min_length=8)

    def validate(self, attrs):
        if attrs['new_password'] != attrs['new_password2']:
            raise serializers.ValidationError({'new_password': 'Пароли не совпадают'})
        return attrs

    def validate_old_password(self, value):
        user = self.context['request'].user
        if not user.check_password(value):
            raise serializers.ValidationError('Неверный старый пароль')
        return value

    def save(self):
        user = self.context['request'].user
        user.set_password(self.validated_data['new_password'])
        user.save()
        return user


class SubscriptionPlanSerializer(serializers.ModelSerializer):
    class Meta:
        model = SubscriptionPlan
        fields = [
            'id', 'name', 'plan_type', 'daily_request_limit',
            'monthly_request_limit', 'can_save_favorites',
            'can_use_translation', 'can_choose_tone', 'price_usd'
        ]


class UserSubscriptionSerializer(serializers.ModelSerializer):
    plan = SubscriptionPlanSerializer(read_only=True)
    is_expired = serializers.BooleanField(read_only=True)

    class Meta:
        model = UserSubscription
        fields = [
            'id', 'plan', 'started_at',
            'expires_at', 'is_active', 'is_expired'
        ]
        read_only_fields = ['id', 'started_at']


class UsageCounterSerializer(serializers.ModelSerializer):
    class Meta:
        model = UsageCounter
        fields = [
            'daily_count', 'monthly_count', 'total_requests',
            'last_reset_daily', 'last_reset_monthly'
        ]
        read_only_fields = fields