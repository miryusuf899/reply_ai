from rest_framework.permissions import BasePermission


class HasActiveSubscription(BasePermission):
    """Проверяет что у пользователя есть активная подписка"""
    message = 'Требуется активная подписка'

    def has_permission(self, request, view):
        if not request.user.is_authenticated:
            return False
        try:
            return request.user.subscription.is_active
        except Exception:
            return False


class CanSaveFavorites(BasePermission):
    """Проверяет что тариф позволяет сохранять избранное"""
    message = 'Ваш тариф не поддерживает избранное. Обновите подписку.'

    def has_permission(self, request, view):
        if not request.user.is_authenticated:
            return False
        try:
            return request.user.subscription.plan.can_save_favorites
        except Exception:
            return False


class CanUseTranslation(BasePermission):
    """Проверяет что тариф позволяет использовать перевод"""
    message = 'Ваш тариф не поддерживает перевод. Обновите подписку.'

    def has_permission(self, request, view):
        if not request.user.is_authenticated:
            return False
        try:
            return request.user.subscription.plan.can_use_translation
        except Exception:
            return False


class CanChooseTone(BasePermission):
    """Проверяет что тариф позволяет выбирать тон"""
    message = 'Ваш тариф не поддерживает выбор тона. Обновите подписку.'

    def has_permission(self, request, view):
        if not request.user.is_authenticated:
            return False
        try:
            return request.user.subscription.plan.can_choose_tone
        except Exception:
            return False


class WithinDailyLimit(BasePermission):
    """Проверяет что пользователь не превысил дневной лимит"""
    message = 'Дневной лимит запросов исчерпан. Попробуйте завтра.'

    def has_permission(self, request, view):
        if not request.user.is_authenticated:
            return False
        try:
            usage = request.user.usage
            limit = request.user.subscription.plan.daily_request_limit
            return usage.daily_count < limit
        except Exception:
            return False