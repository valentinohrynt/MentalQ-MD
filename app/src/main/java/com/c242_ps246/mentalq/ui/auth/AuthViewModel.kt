package com.c242_ps246.mentalq.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.c242_ps246.mentalq.data.repository.AuthRepository
import com.c242_ps246.mentalq.data.manager.FirebaseServiceProvider
import com.c242_ps246.mentalq.data.repository.Result
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class AuthScreenUIState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

enum class ForgotPasswordStep {
    EMAIL,
    OTP,
    NEW_PASSWORD
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firebaseServices: FirebaseServiceProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthScreenUIState())
    val uiState = _uiState.asStateFlow()

    val role = authRepository.getUserRole()
        .map { it.ifBlank { null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun login(email: String, password: String) = runRequest {
        authRepository.login(email, password)
    }

    fun loginWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch {
            _uiState.value = AuthScreenUIState(isLoading = true)
            val result = try {
                val googleToken = account.idToken
                    ?: return@launch fail("Google did not return an ID token")
                val firebaseAuth = firebaseServices.auth()
                    ?: return@launch fail(FirebaseServiceProvider.CONFIGURATION_ERROR)
                val credential = GoogleAuthProvider.getCredential(googleToken, null)
                val user = firebaseAuth.signInWithCredential(credential).await().user
                    ?: return@launch fail("Google authentication returned no user")
                val firebaseToken = user.getIdToken(true).await().token
                    ?: return@launch fail("Firebase did not return an ID token")
                authRepository.googleLogin(firebaseToken)
            } catch (error: Exception) {
                Result.Error(error.message ?: "Google sign-in failed")
            }
            applyResult(result)
        }
    }

    fun register(name: String, email: String, password: String, birthday: String) = runRequest {
        authRepository.register(name, email, password, birthday)
    }

    fun requestResetPassword(email: String) = runRequest {
        authRepository.requestResetPassword(email)
    }

    fun verifyOTP(email: String, otp: String) = runRequest {
        authRepository.verifyOTP(email, otp)
    }

    fun resetPassword(email: String, otp: String, newPassword: String) = runRequest {
        authRepository.resetPassword(email, otp, newPassword)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(success = false)
    }

    private fun runRequest(request: suspend () -> Result<*>) {
        viewModelScope.launch {
            _uiState.value = AuthScreenUIState(isLoading = true)
            applyResult(request())
        }
    }

    private fun applyResult(result: Result<*>) {
        _uiState.value = when (result) {
            Result.Loading -> AuthScreenUIState(isLoading = true)
            is Result.Success -> AuthScreenUIState(success = true)
            is Result.Error -> AuthScreenUIState(error = result.error)
        }
    }

    private fun fail(message: String) {
        _uiState.value = AuthScreenUIState(error = message)
    }
}
