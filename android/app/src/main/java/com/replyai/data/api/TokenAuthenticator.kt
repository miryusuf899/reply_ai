package com.replyai.data.api
import com.replyai.utils.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Named
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    @Named("refresh") private val refreshApi: ApiService
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        val refresh = tokenManager.refreshToken ?: return null
        return try {
            val refreshResponse = runBlocking {
                refreshApi.refreshToken(mapOf("refresh" to refresh))
            }
            val body = refreshResponse.body() ?: return null
            tokenManager.saveTokens(body.access, body.refresh)
            response.request.newBuilder()
                .header("Authorization", "Bearer ${body.access}")
                .build()
        } catch (e: Exception) {
            null
        }
    }
    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}