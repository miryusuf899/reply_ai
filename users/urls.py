from django.urls import path
from rest_framework_simplejwt.views import TokenObtainPairView, TokenRefreshView
from .views import (
    RegisterView,
    LogoutView,
    ProfileView,
    ChangePasswordView,
    DeleteAccountView,
    SubscriptionPlanListView,
    MySubscriptionView,
    MyUsageView,
)

urlpatterns = [
    # Auth
    path('register/', RegisterView.as_view(), name='register'),
    path('login/', TokenObtainPairView.as_view(), name='login'),
    path('token/refresh/', TokenRefreshView.as_view(), name='token-refresh'),
    path('logout/', LogoutView.as_view(), name='logout'),

    # Profile
    path('profile/', ProfileView.as_view(), name='profile'),
    path('profile/change-password/', ChangePasswordView.as_view(), name='change-password'),
    path('profile/delete/', DeleteAccountView.as_view(), name='delete-account'),

    # Subscription
    path('plans/', SubscriptionPlanListView.as_view(), name='plans'),
    path('my-subscription/', MySubscriptionView.as_view(), name='my-subscription'),
    path('my-usage/', MyUsageView.as_view(), name='my-usage'),
]