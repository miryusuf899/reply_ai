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

data class UserSettings(
    val id: String? = null,
    @SerializedName("default_tone") val defaultTone: String = "neutral",
    @SerializedName("default_response_language") val defaultLanguage: String = "ru",
    @SerializedName("default_input_language") val defaultInputLanguage: String = "auto",
    @SerializedName("preferred_messenger") val preferredMessenger: String? = null,
    @SerializedName("auto_detect_language") val autoDetectLanguage: Boolean = true,
    @SerializedName("save_history") val saveHistory: Boolean = true
)

data class UserSettingsUpdate(
    @SerializedName("default_tone") val defaultTone: String? = null,
    @SerializedName("default_response_language") val defaultLanguage: String? = null
)

data class SupportedLanguage(
    val code: String,
    val name: String,
    @SerializedName("native_name") val nativeName: String
)
