package com.c242_ps246.mentalq.ui.main.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.c242_ps246.mentalq.data.manager.MentalQAppPreferences
import com.c242_ps246.mentalq.data.remote.response.ListNoteItem
import com.c242_ps246.mentalq.data.remote.response.UserData
import com.c242_ps246.mentalq.data.repository.AnalysisRepository
import com.c242_ps246.mentalq.data.repository.AuthRepository
import com.c242_ps246.mentalq.data.repository.NoteRepository
import com.c242_ps246.mentalq.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val authRepository: AuthRepository,
    private val analysisRepository: AnalysisRepository,
    private val preferences: MentalQAppPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardScreenUiState())
    val uiState = _uiState.asStateFlow()

    private val _listNote = MutableStateFlow<List<ListNoteItem>>(emptyList())
    val listNote = _listNote.asStateFlow()

    private val _streakInfo = MutableStateFlow(StreakInfo())
    val streakInfo = _streakInfo.asStateFlow()

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData = _userData.asStateFlow()

    private val _predictedStatusMode = MutableStateFlow<String?>(null)
    val predictedStatusMode = _predictedStatusMode.asStateFlow()

    private val _analysisSize = MutableStateFlow(0)
    val analysisSize = _analysisSize.asStateFlow()

    private val loadingOperations = mutableSetOf<String>()
    private var notesJob: Job? = null
    private var analysisJob: Job? = null
    private var userJob: Job? = null
    private var hadPendingNotes = false

    fun refresh() {
        loadLatestNotes()
        getUserData()
        getPredictedStatusMode()
    }

    fun loadLatestNotes() {
        notesJob?.cancel()
        notesJob = viewModelScope.launch {
            startLoading(NOTES)
            try {
                noteRepository.getAllNotes().collect { result ->
                    when (result) {
                        Result.Loading -> Unit
                        is Result.Success -> {
                            val hasPendingNotes = result.data.any { it.pendingAction != null }
                            val noteSyncJustFinished = hadPendingNotes && !hasPendingNotes
                            hadPendingNotes = hasPendingNotes
                            _listNote.value = result.data.take(5)
                            updateStreak(result.data)
                            clearError()
                            stopLoading(NOTES)
                            if (noteSyncJustFinished) getPredictedStatusMode()
                        }
                        is Result.Error -> {
                            setError(result.error)
                            stopLoading(NOTES)
                        }
                    }
                }
            } finally {
                stopLoading(NOTES)
            }
        }
    }

    fun getUserData() {
        userJob?.cancel()
        userJob = viewModelScope.launch {
            startLoading(USER)
            when (val result = authRepository.getUser()) {
                Result.Loading -> Unit
                is Result.Success -> {
                    _userData.value = result.data
                    clearError()
                }
                is Result.Error -> setError(result.error)
            }
            stopLoading(USER)
        }
    }

    fun calculateStreak() {
        updateStreak(_listNote.value)
    }

    fun getPredictedStatusMode() {
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            startLoading(ANALYSIS)
            try {
                analysisRepository.getAnalysis().collect { result ->
                    when (result) {
                        Result.Loading -> Unit
                        is Result.Success -> {
                            val (_, size, mode) = result.data
                            _analysisSize.value = size
                            _predictedStatusMode.value = mode
                            clearError()
                        }
                        is Result.Error -> setError(result.error)
                    }
                }
            } finally {
                stopLoading(ANALYSIS)
            }
        }
    }

    private fun updateStreak(notes: List<ListNoteItem>) {
        val dates = notes.mapNotNull { note ->
            runCatching {
                Instant.parse(note.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
            }.getOrNull()
        }
        val streak = StreakCalculator.calculate(dates)
        _streakInfo.value = streak
        viewModelScope.launch {
            preferences.saveStreakInfo(streak.lastEntryDate?.toString().orEmpty(), streak.currentStreak)
        }
    }

    private fun startLoading(operation: String) {
        loadingOperations += operation
        _uiState.value = _uiState.value.copy(isLoading = true)
    }

    private fun stopLoading(operation: String) {
        loadingOperations -= operation
        _uiState.value = _uiState.value.copy(isLoading = loadingOperations.isNotEmpty())
    }

    private fun setError(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }

    private fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private companion object {
        const val NOTES = "notes"
        const val USER = "user"
        const val ANALYSIS = "analysis"
    }
}

data class DashboardScreenUiState(
    val isLoading: Boolean = true,
    val note: ListNoteItem? = null,
    val error: String? = null
)

data class StreakInfo(
    val currentStreak: Int = 0,
    val lastEntryDate: LocalDate? = null
)

internal object StreakCalculator {
    fun calculate(dates: List<LocalDate>, today: LocalDate = LocalDate.now()): StreakInfo {
        val orderedDates = dates.distinct().sortedDescending()
        val lastEntryDate = orderedDates.firstOrNull() ?: return StreakInfo()
        if (lastEntryDate.isBefore(today.minusDays(1))) {
            return StreakInfo(lastEntryDate = lastEntryDate)
        }

        var streak = 1
        var previousDate = lastEntryDate
        orderedDates.drop(1).forEach { date ->
            if (date != previousDate.minusDays(1)) return@forEach
            streak += 1
            previousDate = date
        }
        return StreakInfo(currentStreak = streak, lastEntryDate = lastEntryDate)
    }
}
