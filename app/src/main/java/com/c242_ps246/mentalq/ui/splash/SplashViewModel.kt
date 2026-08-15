package com.c242_ps246.mentalq.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.c242_ps246.mentalq.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplashSession(
    val isLoaded: Boolean = false,
    val token: String? = null,
    val role: String? = null
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _session = MutableStateFlow(SplashSession())
    val session = _session.asStateFlow()

    init {
        viewModelScope.launch {
            combine(authRepository.getToken(), authRepository.getUserRole()) { token, role ->
                token to role
            }.collectLatest { (token, role) ->
                if (token.isNotBlank()) {
                    authRepository.ensureFirebaseSession()
                }
                _session.value = SplashSession(
                    isLoaded = true,
                    token = token.ifBlank { null },
                    role = role.ifBlank { null }
                )
            }
        }
    }
}
