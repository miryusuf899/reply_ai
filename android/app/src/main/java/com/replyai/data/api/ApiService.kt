package com.replyai.data.api

import com.replyai.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ─── AUTH ───
    @POST("users/login/")
    suspend fun login(@Body body: Map<String, String>): Response<LoginResponse>

    @POST("auth/google/")
    suspend fun googleLogin(@Body body: GoogleAuthRequest): Response<GoogleLoginResponse>

    @POST("users/register/")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("users/token/refresh/")
    suspend fun refreshToken(@Body body: Map<String, String>): Response<LoginResponse>

    @POST("users/logout/")
    suspend fun logout(@Body body: Map<String, String>): Response<Map<String, String>>

    // ─── PROFILE ───
    @GET("users/profile/")
    suspend fun getProfile(): Response<UserProfile>

    @PATCH("users/profile/")
    suspend fun patchProfile(@Body body: ProfilePatch): Response<UserProfile>

    @GET("users/plans/")
    suspend fun getPlans(): Response<List<SubscriptionPlan>>

    @GET("users/my-subscription/")
    suspend fun getMySubscription(): Response<UserSubscription>

    @GET("users/my-usage/")
    suspend fun getMyUsage(): Response<UsageCounter>

    // ─── CHATS ───
    @GET("chats/sessions/")
    suspend fun getSessions(): Response<List<ChatSession>>

    @GET("chats/sessions/favorites/")
    suspend fun getFavoriteSessions(): Response<List<ChatSession>>

    @POST("chats/sessions/")
    suspend fun createSession(@Body request: CreateSessionRequest): Response<ChatSession>

    @GET("chats/sessions/{uuid}/")
    suspend fun getSession(@Path("uuid") uuid: String): Response<ChatSession>

    @PATCH("chats/sessions/{uuid}/")
    suspend fun patchSession(
        @Path("uuid") uuid: String,
        @Body body: SessionPatch
    ): Response<ChatSession>

    @DELETE("chats/sessions/{uuid}/")
    suspend fun deleteSession(@Path("uuid") uuid: String): Response<Map<String, String>>

    @POST("chats/sessions/{uuid}/toggle-favorite/")
    suspend fun toggleFavorite(@Path("uuid") uuid: String): Response<ToggleFavoriteResponse>

    // ─── MESSAGES & AI ───
    @POST("chats/sessions/{uuid}/messages/")
    suspend fun addMessage(
        @Path("uuid") uuid: String,
        @Body message: CreateMessageRequest
    ): Response<ChatMessage>

    /**
     * AIRequestCreateSerializer body:
     * request_type, user_prompt, target_language, tone
     * Returns AIRequestSerializer with nested response.generated_text
     */
    @POST("chats/sessions/{uuid}/ask/")
    suspend fun askAI(
        @Path("uuid") uuid: String,
        @Body request: AskRequest
    ): Response<AIRequestResponse>

    @PATCH("chats/responses/{id}/feedback/")
    suspend fun updateFeedback(
        @Path("id") id: String,
        @Body body: FeedbackRequest
    ): Response<AIResponseDto>

    @GET("chats/responses/favorites/")
    suspend fun getFavoriteResponses(): Response<List<FavoriteAIResponse>>

    // ─── SETTINGS ───
    @GET("settings/my-settings/")
    suspend fun getSettings(): Response<UserSettings>

    @PATCH("settings/my-settings/")
    suspend fun patchSettings(@Body settings: UserSettingsPatch): Response<UserSettings>

    @GET("settings/languages/")
    suspend fun getLanguages(): Response<List<SupportedLanguage>>
}
