package com.replyai.data.api

import com.replyai.data.models.AIRequest
import com.replyai.data.models.AskRequest
import com.replyai.data.models.ChatSession
import com.replyai.data.models.CreateSessionRequest
import com.replyai.data.models.LoginResponse
import com.replyai.data.models.RegisterRequest
import com.replyai.data.models.RegisterResponse
import com.replyai.data.models.SupportedLanguage
import com.replyai.data.models.UserProfile
import com.replyai.data.models.UserSettings
import com.replyai.data.models.UserSettingsUpdate
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    @POST("users/login/")
    suspend fun login(@Body body: Map<String, String>): Response<LoginResponse>

    @POST("users/register/")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("users/token/refresh/")
    suspend fun refreshToken(@Body body: Map<String, String>): Response<LoginResponse>

    @GET("users/profile/")
    suspend fun getProfile(): Response<UserProfile>

    @GET("chats/sessions/")
    suspend fun getSessions(): Response<List<ChatSession>>

    @POST("chats/sessions/")
    suspend fun createSession(@Body request: CreateSessionRequest): Response<ChatSession>

    @GET("chats/sessions/{uuid}/")
    suspend fun getSession(@Path("uuid") uuid: String): Response<ChatSession>

    @DELETE("chats/sessions/{uuid}/")
    suspend fun deleteSession(@Path("uuid") uuid: String): Response<Map<String, String>>

    /**
     * Body: { "request_type": "reply_help", "user_prompt": "...", "tone": "friendly" }
     */
    @POST("chats/sessions/{uuid}/ask/")
    suspend fun askAI(
        @Path("uuid") uuid: String,
        @Body request: AskRequest
    ): Response<AIRequest>

    @GET("settings/my-settings/")
    suspend fun getSettings(): Response<UserSettings>

    @PUT("settings/my-settings/")
    suspend fun updateSettings(@Body settings: UserSettingsUpdate): Response<UserSettings>

    @GET("settings/languages/")
    suspend fun getLanguages(): Response<List<SupportedLanguage>>
}
