from celery import shared_task
from django.utils import timezone
from django.conf import settings
from datetime import date, timedelta


@shared_task(name='cleanup.delete_old_chats')
def delete_old_chats():
    """
    Удаляет сессии у которых истёк срок хранения.
    Запускается каждые 24 часа через Celery Beat.
    Избранные сессии (is_favorite=True) не трогаем.
    """
    from chats.models import ChatSession

    now = timezone.now()
    deleted_count, _ = ChatSession.objects.filter(
        is_favorite=False,
        delete_after__lte=now,
    ).delete()

    return f'Удалено сессий: {deleted_count}'


@shared_task(name='cleanup.reset_daily_counters')
def reset_daily_counters():
    """
    Сбрасывает дневные счётчики запросов.
    Запускается каждый день в полночь.
    """
    from users.models import UsageCounter

    today = date.today()
    updated = UsageCounter.objects.exclude(
        last_reset_daily=today
    ).update(
        daily_count=0,
        last_reset_daily=today,
    )

    return f'Сброшено дневных счётчиков: {updated}'


@shared_task(name='cleanup.reset_monthly_counters')
def reset_monthly_counters():
    """
    Сбрасывает месячные счётчики.
    Запускается 1-го числа каждого месяца.
    """
    from users.models import UsageCounter

    today = date.today()
    first_day = today.replace(day=1)
    updated = UsageCounter.objects.exclude(
        last_reset_monthly=first_day
    ).update(
        monthly_count=0,
        last_reset_monthly=first_day,
    )

    return f'Сброшено месячных счётчиков: {updated}'