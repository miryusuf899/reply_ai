import uuid
from django.contrib.auth.models import AbstractBaseUser, PermissionsMixin, BaseUserManager
from django.db import models
from django.utils import timezone


class UserManager(BaseUserManager):
    def create_user(self, email, password=None, **extra_fields):
        if not email:
            raise ValueError('Email обязателен')
        email = self.normalize_email(email)
        user = self.model(email=email, **extra_fields)
        user.set_password(password)
        user.save(using=self._db)
        return user

    def create_superuser(self, email, password=None, **extra_fields):
        extra_fields.setdefault('is_staff', True)
        extra_fields.setdefault('is_superuser', True)
        return self.create_user(email, password, **extra_fields)


class User(AbstractBaseUser, PermissionsMixin):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    email = models.EmailField(unique=True)
    full_name = models.CharField(max_length=150, blank=True)
    avatar = models.ImageField(upload_to='avatars/', null=True, blank=True)
    is_active = models.BooleanField(default=True)
    is_staff = models.BooleanField(default=False)
    is_verified = models.BooleanField(default=False)  # email подтверждён
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    objects = UserManager()

    USERNAME_FIELD = 'email'
    REQUIRED_FIELDS = []

    class Meta:
        db_table = 'users'
        verbose_name = 'Пользователь'
        verbose_name_plural = 'Пользователи'
        ordering = ['-created_at']

    def __str__(self):
        return self.email


class SubscriptionPlan(models.Model):
    """
    Тарифные планы: free, pro, business
    Хранится в БД — можно менять без деплоя
    """
    class PlanType(models.TextChoices):
        FREE = 'free', 'Бесплатный'
        PRO = 'pro', 'Pro'
        BUSINESS = 'business', 'Business'

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    name = models.CharField(max_length=50)
    plan_type = models.CharField(max_length=20, choices=PlanType.choices, unique=True)
    daily_request_limit = models.PositiveIntegerField(default=10)
    monthly_request_limit = models.PositiveIntegerField(default=100)
    can_save_favorites = models.BooleanField(default=False)
    can_use_translation = models.BooleanField(default=False)
    can_choose_tone = models.BooleanField(default=False)
    price_usd = models.DecimalField(max_digits=6, decimal_places=2, default=0.00)
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'subscription_plans'
        verbose_name = 'Тарифный план'
        verbose_name_plural = 'Тарифные планы'

    def __str__(self):
        return f'{self.name} ({self.plan_type})'


class UserSubscription(models.Model):
    """
    Активная подписка пользователя.
    Один пользователь — одна активная подписка.
    """
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.OneToOneField(User, on_delete=models.CASCADE, related_name='subscription')
    plan = models.ForeignKey(SubscriptionPlan, on_delete=models.PROTECT, related_name='subscriptions')
    started_at = models.DateTimeField(default=timezone.now)
    expires_at = models.DateTimeField(null=True, blank=True)  # null = бессрочно (free)
    is_active = models.BooleanField(default=True)

    class Meta:
        db_table = 'user_subscriptions'
        verbose_name = 'Подписка'
        verbose_name_plural = 'Подписки'

    def __str__(self):
        return f'{self.user.email} — {self.plan.plan_type}'

    @property
    def is_expired(self):
        if self.expires_at is None:
            return False
        return timezone.now() > self.expires_at


class UsageCounter(models.Model):
    """
    Счётчик запросов пользователя.
    Сбрасывается каждый день/месяц через Celery.
    """
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.OneToOneField(User, on_delete=models.CASCADE, related_name='usage')
    daily_count = models.PositiveIntegerField(default=0)
    monthly_count = models.PositiveIntegerField(default=0)
    last_reset_daily = models.DateField(auto_now_add=True)
    last_reset_monthly = models.DateField(auto_now_add=True)
    total_requests = models.PositiveIntegerField(default=0)  # всего за всё время

    class Meta:
        db_table = 'usage_counters'
        verbose_name = 'Счётчик использования'
        verbose_name_plural = 'Счётчики использования'

    def __str__(self):
        return f'{self.user.email} — day:{self.daily_count} month:{self.monthly_count}'