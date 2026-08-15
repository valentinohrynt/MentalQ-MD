package com.c242_ps246.mentalq.data.manager

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "MentalQAppPreferences")

@Singleton
class MentalQAppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenCipher: TokenCipher
) {
    @Volatile
    private var cachedToken: String = ""

    companion object {
        private val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        private val TOKEN = stringPreferencesKey("token")
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val LAST_ENTRY_DATE = stringPreferencesKey("last_entry_date")
        private val STREAK_COUNT = stringPreferencesKey("streak_count")
        private val ROLE = stringPreferencesKey("role")
        private val USER_ID = stringPreferencesKey("user_id")
    }

    private val preferencesFlow = context.dataStore.data.catch { emit(emptyPreferences()) }

    val shouldShowOnboarding: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING] != true
        }

    suspend fun completeOnboarding() {
        context.dataStore.edit { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING] = true
        }
    }

    fun getToken(): Flow<String> {
        return preferencesFlow.map { preferences ->
            tokenCipher.decrypt(preferences[TOKEN].orEmpty()).also { cachedToken = it }
        }
    }

    fun currentToken(): String = cachedToken

    suspend fun saveToken(token: String) {
        cachedToken = token
        context.dataStore.edit { preferences ->
            preferences[TOKEN] = tokenCipher.encrypt(token)
        }
    }

    suspend fun saveStreakInfo(lastEntryDate: String, streakCount: Int) {
        context.dataStore.edit { preferences ->
            preferences[LAST_ENTRY_DATE] = lastEntryDate
            preferences[STREAK_COUNT] = streakCount.toString()
        }
    }

    fun getStreakInfo(): Flow<Pair<String, Int>> {
        return preferencesFlow.map { preferences ->
            val lastEntryDate = preferences[LAST_ENTRY_DATE] ?: ""
            val streakCount = preferences[STREAK_COUNT]?.toIntOrNull() ?: 0
            Pair(lastEntryDate, streakCount)
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    fun getNotificationsState(): Flow<Boolean> {
        return preferencesFlow.map { preferences ->
            preferences[NOTIFICATIONS_ENABLED] == true
        }
    }

    suspend fun saveUserRole(role: String) {
        context.dataStore.edit { preferences ->
            preferences[ROLE] = role
        }
    }

    fun getUserRole(): Flow<String> {
        return preferencesFlow.map { preferences ->
            preferences[ROLE] ?: ""
        }
    }

    suspend fun saveUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID] = userId
        }
    }

    fun getUserId(): Flow<String> {
        return preferencesFlow.map { preferences ->
            preferences[USER_ID] ?: ""
        }
    }
}
