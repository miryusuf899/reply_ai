package com.replyai.data.models

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val access: String,
    val refresh: String
)

/** dj-rest-auth Google may return nested tokens */
data class GoogleLoginResponse(
    val access: String? = null,
    val refresh: String? = null,
    val access_token: String? = null,
    val refresh_token: String? = null,
    val key: String? = null
) {
    fun resolveAccess(): String? = access ?: access_token ?: key
    fun resolveRefresh(): String? = refresh ?: refresh_token
}

data class GoogleAuthRequest(
    @SerializedName("access_token") val accessToken: String
)

data class RegisterRequest(
    val email: String,
    @SerializedName("full_name") val fullName: String,
    val password: String,
    val password2: String
)

data class RegisterResponse(
    val user: UserProfile,
    val tokens: TokenPair
)

data class TokenPair(
    val access: String,
    val refresh: String
)

data class UserProfile(
    val id: String,
    val email: String,
    @SerializedName("full_name") val fullName: String?,
    val avatar: String?,
    @SerializedName("is_verified") val isVerified: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null
)

data class ProfilePatch(
    @SerializedName("full_name") val fullName: String? = null,
    val avatar: String? = null
)

data class SubscriptionPlan(
    val id: String,
    val name: String,
    @SerializedName("plan_type") val planType: String,
    @SerializedName("daily_request_limit") val dailyRequestLimit: Int,
    @SerializedName("monthly_request_limit") val monthlyRequestLimit: Int,
    @SerializedName("price_usd") val priceUsd: String?
)

data class UserSubscription(
    val id: String,
    val plan: SubscriptionPlan,
    @SerializedName("started_at") val startedAt: String?,
    @SerializedName("expires_at") val expiresAt: String?,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("is_expired") val isExpired: Boolean
)

data class UsageCounter(
    @SerializedName("daily_count") val dailyCount: Int,
    @SerializedName("monthly_count") val monthlyCount: Int,
    @SerializedName("total_requests") val totalRequests: Int
)

data class UserSettings(
    val id: String? = null,
    @SerializedName("default_tone") val defaultTone: String = "neutral",
    @SerializedName("default_response_language") val defaultLanguage: String = "ru",
    @SerializedName("preferred_messenger") val preferredMessenger: String? = null,
    @SerializedName("auto_detect_language") val autoDetectLanguage: Boolean = true,
    @SerializedName("save_history") val saveHistory: Boolean = true
)

data class UserSettingsPatch(
    @SerializedName("default_tone") val defaultTone: String? = null,
    @SerializedName("default_response_language") val defaultLanguage: String? = null,
    @SerializedName("preferred_messenger") val preferredMessenger: String? = null
)

data class SupportedLanguage(
    val code: String,
    val name: String,
    @SerializedName("native_name") val nativeName: String
)
