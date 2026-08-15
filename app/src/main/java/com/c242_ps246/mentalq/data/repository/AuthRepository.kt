package com.c242_ps246.mentalq.data.repository

import com.c242_ps246.mentalq.data.local.room.AnalysisDao
import com.c242_ps246.mentalq.data.local.room.NoteDao
import com.c242_ps246.mentalq.data.local.room.UserDao
import com.c242_ps246.mentalq.data.local.room.toEntity
import com.c242_ps246.mentalq.data.local.room.toModel
import com.c242_ps246.mentalq.data.manager.MentalQAppPreferences
import com.c242_ps246.mentalq.data.manager.FirebaseServiceProvider
import com.c242_ps246.mentalq.data.remote.response.AuthResponse
import com.c242_ps246.mentalq.data.remote.response.RegisterResponse
import com.c242_ps246.mentalq.data.remote.response.UserData
import com.c242_ps246.mentalq.data.remote.retrofit.AuthApiService
import com.c242_ps246.mentalq.data.remote.retrofit.UserApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val userDao: UserDao,
    private val noteDao: NoteDao,
    private val analysisDao: AnalysisDao,
    private val preferences: MentalQAppPreferences,
    private val firebaseServices: FirebaseServiceProvider,
    private val userApiService: UserApiService
) {
    suspend fun login(email: String, password: String): Result<AuthResponse> =
        authenticate { authApiService.login(email, password) }

    suspend fun googleLogin(firebaseToken: String): Result<AuthResponse> =
        authenticate { authApiService.googleLogin(firebaseToken) }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        birthday: String
    ): Result<RegisterResponse> = try {
        val response = authApiService.register(name, email, password, birthday)
        val body = response.body()
        when {
            !response.isSuccessful -> Result.Error(response.errorMessage("Registration failed"))
            body == null -> Result.Error("Registration returned an empty response")
            body.error == true -> Result.Error(body.message ?: "Registration failed")
            else -> Result.Success(body)
        }
    } catch (error: Exception) {
        Result.Error(error.toUserMessage("Registration failed"))
    }

    suspend fun logout(): Result<Unit> = try {
        preferences.saveToken("")
        preferences.saveUserRole("")
        preferences.saveUserId("")
        preferences.saveStreakInfo("", 0)
        preferences.setNotificationsEnabled(false)
        userDao.clearUserData()
        noteDao.clearAllNotes()
        analysisDao.clearAllAnalysis()
        firebaseServices.auth()?.signOut()
        Result.Success(Unit)
    } catch (error: Exception) {
        Result.Error(error.toUserMessage("Logout failed"))
    }

    fun getToken(): Flow<String> = preferences.getToken()

    fun getUserId(): Flow<String> = preferences.getUserId()

    fun getUserRole(): Flow<String> = preferences.getUserRole()

    suspend fun ensureFirebaseSession(): Result<Unit> {
        val firebaseAuth = firebaseServices.auth()
            ?: return Result.Error(FirebaseServiceProvider.CONFIGURATION_ERROR)
        if (firebaseAuth.currentUser != null) return Result.Success(Unit)

        return try {
            val response = userApiService.createFirebaseToken()
            val body = response.body()
            val customToken = body?.firebaseCustomToken?.takeIf(String::isNotBlank)
            when {
                !response.isSuccessful -> Result.Error(response.errorMessage("Unable to restore chat access"))
                body?.error == true -> Result.Error(body.message ?: "Unable to restore chat access")
                customToken == null -> Result.Error("No Firebase session token was returned")
                else -> {
                    firebaseAuth.signInWithCustomToken(customToken).await()
                    Result.Success(Unit)
                }
            }
        } catch (error: Exception) {
            Result.Error(error.toUserMessage("Unable to restore chat access"))
        }
    }

    suspend fun getUser(): Result<UserData> = try {
        userDao.getUserData()?.toModel()?.let { Result.Success(it) } ?: Result.Error("User not found")
    } catch (error: Exception) {
        Result.Error(error.toUserMessage("Unable to load the user"))
    }

    suspend fun requestResetPassword(email: String): Result<Unit> = try {
        val response = authApiService.requestResetPassword(email)
        val body = response.body()
        if (response.isSuccessful && body?.error != true) {
            Result.Success(Unit)
        } else {
            Result.Error(body?.message ?: response.errorMessage("Unable to request a password reset"))
        }
    } catch (error: Exception) {
        Result.Error(error.toUserMessage("Unable to request a password reset"))
    }

    suspend fun verifyOTP(email: String, otp: String): Result<Unit> = try {
        val response = authApiService.verifyOTP(email, otp)
        val body = response.body()
        if (response.isSuccessful && body?.error != true) {
            Result.Success(Unit)
        } else {
            Result.Error(body?.message ?: response.errorMessage("Invalid verification code"))
        }
    } catch (error: Exception) {
        Result.Error(error.toUserMessage("Unable to verify the code"))
    }

    suspend fun resetPassword(email: String, otp: String, password: String): Result<Unit> = try {
        val response = authApiService.resetPassword(email, otp, password)
        val body = response.body()
        if (response.isSuccessful && body?.error != true) {
            Result.Success(Unit)
        } else {
            Result.Error(body?.message ?: response.errorMessage("Unable to reset the password"))
        }
    } catch (error: Exception) {
        Result.Error(error.toUserMessage("Unable to reset the password"))
    }

    private suspend fun authenticate(
        request: suspend () -> Response<AuthResponse>
    ): Result<AuthResponse> = try {
        val response = request()
        val body = response.body()
        when {
            !response.isSuccessful -> Result.Error(response.errorMessage("Authentication failed"))
            body == null -> Result.Error("Authentication returned an empty response")
            body.error == true -> Result.Error(body.message ?: "Authentication failed")
            else -> persistSession(body)
        }
    } catch (error: Exception) {
        Result.Error(error.toUserMessage("Authentication failed"))
    }

    private suspend fun persistSession(response: AuthResponse): Result<AuthResponse> {
        val user = response.user ?: return Result.Error("No user information was returned")
        val token = response.token?.takeIf(String::isNotBlank)
            ?: return Result.Error("No authentication token was returned")
        val role = user.role?.takeIf(String::isNotBlank)
            ?: return Result.Error("No user role was returned")
        val firebaseCustomToken = response.firebaseCustomToken?.takeIf(String::isNotBlank)
            ?: return Result.Error("No Firebase session token was returned")
        val firebaseAuth = firebaseServices.auth()
            ?: return Result.Error(FirebaseServiceProvider.CONFIGURATION_ERROR)

        try {
            firebaseAuth.signInWithCustomToken(firebaseCustomToken).await()
        } catch (error: Exception) {
            return Result.Error(error.toUserMessage("Firebase authentication failed"))
        }

        preferences.saveToken(token)
        preferences.saveUserRole(role)
        preferences.saveUserId(user.id)
        userDao.clearUserData()
        userDao.insertUser(user.toEntity())
        return Result.Success(response)
    }
}
