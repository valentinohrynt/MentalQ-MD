package com.c242_ps246.mentalq.ui.main.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.c242_ps246.mentalq.data.manager.MentalQAppPreferences
import com.c242_ps246.mentalq.data.remote.response.UserData
import com.c242_ps246.mentalq.data.repository.AuthRepository
import com.c242_ps246.mentalq.data.repository.Result
import com.c242_ps246.mentalq.data.repository.UserRepository
import com.c242_ps246.mentalq.ui.auth.AuthScreenUIState
import com.c242_ps246.mentalq.ui.notification.dailyreminder.DailyReminderNotificationHelper.Companion.DAILY_REMINDER_WORK_NAME
import com.c242_ps246.mentalq.ui.notification.dailyreminder.DailyReminderWorker
import com.c242_ps246.mentalq.ui.notification.streak.StreakNotificationHelper
import com.c242_ps246.mentalq.ui.notification.streak.StreakWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val preferences: MentalQAppPreferences,
    private val workManager: WorkManager,
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthScreenUIState())
    val uiState = _uiState.asStateFlow()

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData = _userData.asStateFlow()

    val notificationsEnabled = preferences.getNotificationsState().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        false
    )

    init {
        getUserData()
    }

    fun logout(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            workManager.cancelUniqueWork(StreakNotificationHelper.WORK_NAME)
            workManager.cancelUniqueWork(DAILY_REMINDER_WORK_NAME)
            when (val result = authRepository.logout()) {
                Result.Loading -> Unit
                is Result.Success -> onComplete?.invoke()
                is Result.Error -> _uiState.value = AuthScreenUIState(error = result.error)
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setNotificationsEnabled(enabled)
            if (enabled) {
                StreakWorker.scheduleNextNotification(appContext)
                scheduleReminder()
            } else {
                workManager.cancelUniqueWork(StreakNotificationHelper.WORK_NAME)
                workManager.cancelUniqueWork(DAILY_REMINDER_WORK_NAME)
            }
        }
    }

    fun getUserData() {
        viewModelScope.launch {
            _uiState.value = AuthScreenUIState(isLoading = true)
            when (val result = authRepository.getUser()) {
                Result.Loading -> Unit
                is Result.Success -> {
                    _userData.value = result.data
                    _uiState.value = AuthScreenUIState()
                }
                is Result.Error -> _uiState.value = AuthScreenUIState(error = result.error)
            }
        }
    }

    fun updateProfile(
        name: RequestBody,
        email: RequestBody,
        birthday: RequestBody,
        profileImage: MultipartBody.Part?,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.value = AuthScreenUIState(isLoading = true)
            when (val result = userRepository.updateProfile(name, email, birthday, profileImage)) {
                Result.Loading -> Unit
                is Result.Success -> {
                    _userData.value = result.data
                    _uiState.value = AuthScreenUIState(success = true)
                    onSuccess?.invoke()
                }
                is Result.Error -> _uiState.value = AuthScreenUIState(error = result.error)
            }
        }
    }

    private fun scheduleReminder() {
        val reminderRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            DAILY_REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            reminderRequest
        )
    }
}
