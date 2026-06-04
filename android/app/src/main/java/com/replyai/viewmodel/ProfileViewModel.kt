package com.replyai.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replyai.data.models.SubscriptionPlan
import com.replyai.data.models.UsageCounter
import com.replyai.data.models.UserProfile
import com.replyai.data.models.UserSubscription
import com.replyai.data.repository.AuthRepository
import com.replyai.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _profile = MutableLiveData<UserProfile?>()
    val profile: LiveData<UserProfile?> = _profile

    private val _subscription = MutableLiveData<UserSubscription?>()
    val subscription: LiveData<UserSubscription?> = _subscription

    private val _usage = MutableLiveData<UsageCounter?>()
    val usage: LiveData<UsageCounter?> = _usage

    private val _plans = MutableLiveData<List<SubscriptionPlan>>(emptyList())
    val plans: LiveData<List<SubscriptionPlan>> = _plans

    fun loadAll() {
        viewModelScope.launch {
            _loading.value = true
            authRepository.loadProfile().onSuccess { _profile.value = it }
            profileRepository.getSubscription().onSuccess { _subscription.value = it }
            profileRepository.getUsage().onSuccess { _usage.value = it }
            profileRepository.getPlans().onSuccess { _plans.value = it }
            _loading.value = false
        }
    }
}
