package com.replyai.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replyai.data.models.UserProfile
import com.replyai.data.models.UserSettings
import com.replyai.data.models.UserSettingsPatch
import com.replyai.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

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
            repository.login(email, password)
                .onSuccess { _loginSuccess.value = true }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun googleLogin(idToken: String) {
        viewModelScope.launch {
            _loading.value = true
            repository.googleLogin(idToken)
                .onSuccess { _loginSuccess.value = true }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun register(email: String, fullName: String, password: String, password2: String) {
        viewModelScope.launch {
            _loading.value = true
            repository.register(email, fullName, password, password2)
                .onSuccess { _registerSuccess.value = true }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            repository.loadProfile().onSuccess { _profile.value = it }
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            repository.getSettings().onSuccess { _settings.value = it }
        }
    }

    fun patchSettings(patch: UserSettingsPatch) {
        viewModelScope.launch {
            repository.patchSettings(patch).onSuccess { _settings.value = it }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }
}
