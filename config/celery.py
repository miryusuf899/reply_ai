import os
from celery import Celery
from celery.schedules import crontab

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')

app = Celery('reply_ai')
app.config_from_object('django.conf:settings', namespace='CELERY')
app.autodiscover_tasks()

app.conf.beat_schedule = {
    # Удаление старых чатов — каждый день в 03:00
    'delete-old-chats': {
        'task': 'cleanup.delete_old_chats',
        'schedule': crontab(hour=3, minute=0),
    },
    # Сброс дневных счётчиков — каждый день в 00:00
    'reset-daily-counters': {
        'task': 'cleanup.reset_daily_counters',
        'schedule': crontab(hour=0, minute=0),
    },
    # Сброс месячных счётчиков — 1-го числа в 00:05
    'reset-monthly-counters': {
        'task': 'cleanup.reset_monthly_counters',
        'schedule': crontab(hour=0, minute=5, day_of_month=1),
    },
}