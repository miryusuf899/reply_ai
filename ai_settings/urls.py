from django.urls import path
from .views import UserSettingView, SupportedLanguageListView

urlpatterns = [
    path('my-settings/', UserSettingView.as_view(), name='my-settings'),
    path('languages/', SupportedLanguageListView.as_view(), name='languages'),
]