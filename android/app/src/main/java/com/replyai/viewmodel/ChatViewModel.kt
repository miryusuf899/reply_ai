package com.replyai.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replyai.data.models.AIRequestResponse
import com.replyai.data.models.ChatSession
import com.replyai.data.models.FavoriteAIResponse
import com.replyai.data.models.RequestTypes
import com.replyai.data.models.ToneChoices
import com.replyai.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _sessions = MutableLiveData<List<ChatSession>>(emptyList())
    val sessions: LiveData<List<ChatSession>> = _sessions

    private val _favoriteSessions = MutableLiveData<List<ChatSession>>(emptyList())
    val favoriteSessions: LiveData<List<ChatSession>> = _favoriteSessions

    private val _favoriteResponses = MutableLiveData<List<FavoriteAIResponse>>(emptyList())
    val favoriteResponses: LiveData<List<FavoriteAIResponse>> = _favoriteResponses

    private val _currentSession = MutableLiveData<ChatSession?>()
    val currentSession: LiveData<ChatSession?> = _currentSession

    private val _aiResponse = MutableLiveData<AIRequestResponse?>()
    val aiResponse: LiveData<AIRequestResponse?> = _aiResponse

    private val _sessionCreated = MutableLiveData<ChatSession?>()
    val sessionCreated: LiveData<ChatSession?> = _sessionCreated

    fun loadSessions() {
        viewModelScope.launch {
            _loading.value = true
            repository.getSessions()
                .onSuccess { _sessions.value = it }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun loadFavorites() {
        viewModelScope.launch {
            _loading.value = true
            repository.getFavoriteSessions()
                .onSuccess { _favoriteSessions.value = it }
                .onFailure { _error.value = it.message }
            repository.getFavoriteResponses()
                .onSuccess { _favoriteResponses.value = it }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun createSession(messenger: String, title: String) {
        viewModelScope.launch {
            _loading.value = true
            repository.createSession(messenger, title)
                .onSuccess {
                    _sessionCreated.value = it
                    loadSessions()
                }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun loadSession(uuid: String) {
        viewModelScope.launch {
            _loading.value = true
            repository.getSession(uuid)
                .onSuccess { _currentSession.value = it }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun deleteSession(uuid: String) {
        viewModelScope.launch {
            repository.deleteSession(uuid).onSuccess { loadSessions() }
        }
    }

    fun toggleFavorite(uuid: String) {
        viewModelScope.launch {
            repository.toggleFavorite(uuid)
                .onSuccess { loadSessions() }
                .onFailure { _error.value = it.message }
        }
    }

    fun askAI(
        sessionId: String,
        userPrompt: String,
        tone: String,
        requestType: String = RequestTypes.REPLY_HELP,
        language: String = "ru"
    ) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            repository.askAI(
                sessionId = sessionId,
                userPrompt = userPrompt,
                requestType = requestType,
                tone = tone,
                targetLanguage = language
            )
                .onSuccess {
                    _aiResponse.value = it
                    loadSession(sessionId)
                }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun clearSessionCreated() { _sessionCreated.value = null }
    fun clearError() { _error.value = null }
}
