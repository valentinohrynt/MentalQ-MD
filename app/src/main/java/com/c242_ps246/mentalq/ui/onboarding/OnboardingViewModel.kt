package com.c242_ps246.mentalq.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.c242_ps246.mentalq.data.manager.MentalQAppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: MentalQAppPreferences
) : ViewModel() {
    val shouldShowOnboarding: StateFlow<Boolean> = preferences.shouldShowOnboarding
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true
        )

    fun onOnboardingCompleted() {
        viewModelScope.launch { preferences.completeOnboarding() }
    }
}
