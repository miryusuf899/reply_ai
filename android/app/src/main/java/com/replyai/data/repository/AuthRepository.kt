package com.replyai.data.repository

import com.replyai.data.api.ApiService
import com.replyai.data.models.GoogleAuthRequest
import com.replyai.data.models.ProfilePatch
import com.replyai.data.models.RegisterRequest
import com.replyai.data.models.UserProfile
import com.replyai.data.models.UserSettings
import com.replyai.data.models.UserSettingsPatch
import com.replyai.utils.TokenManager
import com.replyai.utils.parseApiError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val tokenManager: TokenManager
) {

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val response = api.login(mapOf("email" to email, "password" to password))
        if (!response.isSuccessful) throw Exception(parseApiError(response.errorBody()?.string()))
        val body = response.body() ?: throw Exception("Пустой ответ")
        tokenManager.saveTokens(body.access, body.refresh, email)
        loadProfile()
    }

    suspend fun googleLogin(idToken: String): Result<Unit> = runCatching {
        val response = api.googleLogin(GoogleAuthRequest(idToken))
        if (!response.isSuccessful) throw Exception(parseApiError(response.errorBody()?.string()))
        val body = response.body() ?: throw Exception("Пустой ответ")
        val access = body.resolveAccess() ?: throw Exception("Нет access токена")
        val refresh = body.resolveRefresh() ?: ""
        tokenManager.saveTokens(access, refresh)
        loadProfile()
    }

    suspend fun register(
        email: String,
        fullName: String,
        password: String,
        password2: String
    ): Result<Unit> = runCatching {
        val response = api.register(RegisterRequest(email, fullName, password, password2))
        if (!response.isSuccessful) throw Exception(parseApiError(response.errorBody()?.string()))
    }

    suspend fun logout(): Result<Unit> = runCatching {
        val refresh = tokenManager.refreshToken
        if (!refresh.isNullOrBlank()) {
            api.logout(mapOf("refresh" to refresh))
        }
        tokenManager.clear()
    }

    suspend fun loadProfile(): Result<UserProfile> = runCatching {
        val response = api.getProfile()
        if (!response.isSuccessful) throw Exception(parseApiError(response.errorBody()?.string()))
        val profile = response.body() ?: throw Exception("Пустой ответ")
        tokenManager.userEmail = profile.email
        tokenManager.userName = profile.fullName
        profile
    }

    suspend fun patchProfile(fullName: String?): Result<UserProfile> = runCatching {
        val response = api.patchProfile(ProfilePatch(fullName = fullName))
        if (!response.isSuccessful) throw Exception(parseApiError(response.errorBody()?.string()))
        response.body() ?: throw Exception("Пустой ответ")
    }

    suspend fun getSettings(): Result<UserSettings> = runCatching {
        val response = api.getSettings()
        if (!response.isSuccessful) throw Exception(parseApiError(response.errorBody()?.string()))
        response.body() ?: throw Exception("Пустой ответ")
    }

    suspend fun patchSettings(patch: UserSettingsPatch): Result<UserSettings> = runCatching {
        val response = api.patchSettings(patch)
        if (!response.isSuccessful) throw Exception(parseApiError(response.errorBody()?.string()))
        response.body() ?: throw Exception("Пустой ответ")
    }

    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()
}
