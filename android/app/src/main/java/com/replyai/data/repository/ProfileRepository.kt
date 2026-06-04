package com.replyai.data.repository

import com.replyai.data.api.ApiService
import com.replyai.data.models.SubscriptionPlan
import com.replyai.data.models.UsageCounter
import com.replyai.data.models.UserSubscription
import com.replyai.utils.parseApiError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun getPlans(): Result<List<SubscriptionPlan>> = runCatching {
        val response = api.getPlans()
        if (!response.isSuccessful) throw Exception(parseApiError(response.errorBody()?.string()))
        response.body() ?: emptyList()
    }

    suspend fun getSubscription(): Result<UserSubscription> = runCatching {
        val response = api.getMySubscription()
        if (!response.isSuccessful) throw Exception(parseApiError(response.errorBody()?.string()))
        response.body() ?: throw Exception("Пустой ответ")
    }

    suspend fun getUsage(): Result<UsageCounter> = runCatching {
        val response = api.getMyUsage()
        if (!response.isSuccessful) throw Exception(parseApiError(response.errorBody()?.string()))
        response.body() ?: throw Exception("Пустой ответ")
    }
}
