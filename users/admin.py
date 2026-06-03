from django.contrib import admin
from django.contrib.auth.admin import UserAdmin as BaseUserAdmin
from django.utils.translation import gettext_lazy as _
from .models import User, SubscriptionPlan, UserSubscription, UsageCounter


@admin.register(User)
class UserAdmin(BaseUserAdmin):
    ordering = ['-created_at']
    list_display = ['email', 'full_name', 'is_active', 'is_verified', 'is_staff', 'created_at']
    list_filter = ['is_active', 'is_verified', 'is_staff', 'created_at']
    search_fields = ['email', 'full_name']
    readonly_fields = ['id', 'created_at', 'updated_at']

    fieldsets = (
        (_('Основное'), {'fields': ('id', 'email', 'password')}),
        (_('Личные данные'), {'fields': ('full_name', 'avatar')}),
        (_('Статус'), {'fields': ('is_active', 'is_verified', 'is_staff', 'is_superuser')}),
        (_('Даты'), {'fields': ('last_login', 'created_at', 'updated_at')}),
        (_('Права'), {'fields': ('groups', 'user_permissions')}),
    )

    add_fieldsets = (
        (None, {
            'classes': ('wide',),
            'fields': ('email', 'full_name', 'password1', 'password2'),
        }),
    )


@admin.register(SubscriptionPlan)
class SubscriptionPlanAdmin(admin.ModelAdmin):
    list_display = [
        'name', 'plan_type', 'daily_request_limit',
        'monthly_request_limit', 'price_usd', 'is_active'
    ]
    list_filter = ['plan_type', 'is_active']
    search_fields = ['name', 'plan_type']
    readonly_fields = ['id', 'created_at']

    fieldsets = (
        ('Основное', {'fields': ('id', 'name', 'plan_type', 'price_usd', 'is_active')}),
        ('Лимиты', {'fields': ('daily_request_limit', 'monthly_request_limit')}),
        ('Возможности', {'fields': (
            'can_save_favorites',
            'can_use_translation',
            'can_choose_tone',
        )}),
        ('Даты', {'fields': ('created_at',)}),
    )


@admin.register(UserSubscription)
class UserSubscriptionAdmin(admin.ModelAdmin):
    list_display = ['user', 'plan', 'started_at', 'expires_at', 'is_active', 'is_expired']
    list_filter = ['is_active', 'plan']
    search_fields = ['user__email']
    readonly_fields = ['id', 'started_at']
    autocomplete_fields = ['user']

    @admin.display(boolean=True, description='Истекла?')
    def is_expired(self, obj):
        return obj.is_expired


@admin.register(UsageCounter)
class UsageCounterAdmin(admin.ModelAdmin):
    list_display = [
        'user', 'daily_count', 'monthly_count',
        'total_requests', 'last_reset_daily', 'last_reset_monthly'
    ]
    search_fields = ['user__email']
    readonly_fields = ['id', 'total_requests', 'last_reset_daily', 'last_reset_monthly']
    autocomplete_fields = ['user']