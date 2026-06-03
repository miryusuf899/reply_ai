from django.db.models.signals import post_save
from django.dispatch import receiver
from .models import ChatSession, AIResponse


@receiver(post_save, sender=ChatSession)
def update_context_on_favorite(sender, instance, created, **kwargs):
    """
    Когда сессия помечается как избранная —
    убираем дату удаления (уже делается в save(),
    но сигнал страхует от прямых QuerySet.update())
    """
    if not created and instance.is_favorite and instance.delete_after is not None:
        ChatSession.objects.filter(pk=instance.pk).update(delete_after=None)


@receiver(post_save, sender=AIResponse)
def sync_session_favorite(sender, instance, **kwargs):
    """
    Если ответ помечен как избранный —
    автоматически помечаем и сессию как избранную
    """
    if instance.is_favorite:
        session = instance.request.session
        if not session.is_favorite:
            session.is_favorite = True
            session.save()