package com.replyai.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replyai.data.models.AIRequest
import com.replyai.data.models.ChatSession
import com.replyai.data.repository.ChatRepository
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val repository = ChatRepository()

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _sessions = MutableLiveData<List<ChatSession>>(emptyList())
    val sessions: LiveData<List<ChatSession>> = _sessions

    private val _currentSession = MutableLiveData<ChatSession?>()
    val currentSession: LiveData<ChatSession?> = _currentSession

    private val _aiResponse = MutableLiveData<AIRequest?>()
    val aiResponse: LiveData<AIRequest?> = _aiResponse

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
            repository.deleteSession(uuid)
                .onSuccess { loadSessions() }
                .onFailure { _error.value = it.message }
        }
    }

    fun askAI(sessionId: String, userPrompt: String, tone: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            repository.askAI(sessionId, userPrompt, tone)
                .onSuccess {
                    _aiResponse.value = it
                    loadSession(sessionId)
                }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun clearAiResponse() {
        _aiResponse.value = null
    }

    fun clearSessionCreated() {
        _sessionCreated.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
