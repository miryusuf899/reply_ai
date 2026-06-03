from django.db.models.signals import post_save
from django.dispatch import receiver
from django.conf import settings
from .models import UsageCounter, UserSubscription, SubscriptionPlan

User = settings.AUTH_USER_MODEL


@receiver(post_save, sender=User)
def create_user_related_objects(sender, instance, created, **kwargs):
    """
    При создании пользователя автоматически создаём:
    - UsageCounter (счётчик запросов)
    - UserSubscription (бесплатный тариф)
    - UserSetting (настройки по умолчанию)
    """
    if created:
        # Счётчик использования
        UsageCounter.objects.get_or_create(user=instance)

        # Бесплатная подписка
        free_plan = SubscriptionPlan.objects.filter(plan_type='free').first()
        if free_plan:
            UserSubscription.objects.get_or_create(
                user=instance,
                defaults={'plan': free_plan}
            )

        # Настройки пользователя (из другого приложения)
        try:
            from ai_settings.models import UserSetting
            UserSetting.objects.get_or_create(user=instance)
        except Exception:
            pass