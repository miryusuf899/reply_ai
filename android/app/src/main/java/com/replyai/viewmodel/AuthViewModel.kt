package com.replyai.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replyai.data.models.UserProfile
import com.replyai.data.models.UserSettings
import com.replyai.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loginSuccess = MutableLiveData(false)
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    private val _registerSuccess = MutableLiveData(false)
    val registerSuccess: LiveData<Boolean> = _registerSuccess

    private val _profile = MutableLiveData<UserProfile?>()
    val profile: LiveData<UserProfile?> = _profile

    private val _settings = MutableLiveData<UserSettings?>()
    val settings: LiveData<UserSettings?> = _settings

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            repository.login(email, password)
                .onSuccess {
                    _loginSuccess.value = true
                }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun register(email: String, fullName: String, password: String, password2: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            repository.register(email, fullName, password, password2)
                .onSuccess { _registerSuccess.value = true }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            repository.loadProfile()
                .onSuccess { _profile.value = it }
                .onFailure { _error.value = it.message }
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            _loading.value = true
            repository.getSettings()
                .onSuccess { _settings.value = it }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun updateSettings(tone: String, language: String) {
        viewModelScope.launch {
            _loading.value = true
            repository.updateSettings(tone, language)
                .onSuccess { _settings.value = it }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun logout() {
        repository.logout()
    }

    fun clearEvents() {
        _loginSuccess.value = false
        _registerSuccess.value = false
        _error.value = null
    }
}
