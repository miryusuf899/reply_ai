package com.replyai.data.repository

import com.replyai.data.api.RetrofitClient
import com.replyai.data.models.RegisterRequest
import com.replyai.data.models.UserProfile
import com.replyai.data.models.UserSettings
import com.replyai.data.models.UserSettingsUpdate
import com.replyai.utils.TokenManager

class AuthRepository {

    private val api = RetrofitClient.api
    private val tokenManager = TokenManager.getInstance()

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val response = api.login(
            mapOf("email" to email, "password" to password)
        )
        if (!response.isSuccessful) {
            throw Exception(parseError(response.errorBody()?.string()))
        }
        val body = response.body() ?: throw Exception("Empty response")
        tokenManager.saveTokens(body.access, body.refresh, email)
        loadProfile()
    }

    suspend fun register(
        email: String,
        fullName: String,
        password: String,
        password2: String
    ): Result<Unit> = runCatching {
        val response = api.register(
            RegisterRequest(email, fullName, password, password2)
        )
        if (!response.isSuccessful) {
            throw Exception(parseError(response.errorBody()?.string()))
        }
    }

    suspend fun loadProfile(): Result<UserProfile> = runCatching {
        val response = api.getProfile()
        if (!response.isSuccessful) {
            throw Exception(parseError(response.errorBody()?.string()))
        }
        val profile = response.body() ?: throw Exception("Empty response")
        tokenManager.userEmail = profile.email
        tokenManager.userName = profile.fullName
        profile
    }

    suspend fun getSettings(): Result<UserSettings> = runCatching {
        val response = api.getSettings()
        if (!response.isSuccessful) {
            throw Exception(parseError(response.errorBody()?.string()))
        }
        response.body() ?: throw Exception("Empty response")
    }

    suspend fun updateSettings(tone: String?, language: String?): Result<UserSettings> = runCatching {
        val response = api.updateSettings(
            UserSettingsUpdate(defaultTone = tone, defaultLanguage = language)
        )
        if (!response.isSuccessful) {
            throw Exception(parseError(response.errorBody()?.string()))
        }
        response.body() ?: throw Exception("Empty response")
    }

    fun logout() {
        tokenManager.clear()
    }

    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()

    private fun parseError(body: String?): String {
        if (body.isNullOrBlank()) return "Request failed"
        return try {
            val detail = Regex(""""detail"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1)
            detail ?: body.take(200)
        } catch (_: Exception) {
            body.take(200)
        }
    }
}
