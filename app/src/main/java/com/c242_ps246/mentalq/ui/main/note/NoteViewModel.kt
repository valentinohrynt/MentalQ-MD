package com.c242_ps246.mentalq.ui.main.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.c242_ps246.mentalq.data.remote.response.ListNoteItem
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

data class NoteScreenUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val canAddNewNote: Boolean = true
)

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(NoteScreenUiState())
    val uiState = _uiState.asStateFlow()

    private val _listNote = MutableStateFlow<List<ListNoteItem>>(emptyList())
    val listNote = _listNote.asStateFlow()

    private var notesJob: Job? = null

    private fun isNoteTodayAlreadyAdded(): Boolean {
        val today = LocalDate.now(APP_ZONE)
        return _listNote.value.any { note ->
            note.createdAt?.let { createdAt ->
                runCatching {
                    Instant.parse(createdAt).atZone(APP_ZONE).toLocalDate() == today
                }.getOrDefault(false)
            } ?: false
        }
    }

    fun loadAllNotes() {
        notesJob?.cancel()
        notesJob = viewModelScope.launch {
            noteRepository.getAllNotes().collect { result ->
                when (result) {
                    Result.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
                    is Result.Success -> {
                        _listNote.value = result.data
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = null,
                            canAddNewNote = !isNoteTodayAlreadyAdded()
                        )
                    }
                    is Result.Error -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error
                    )
                }
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            when (val result = noteRepository.deleteNoteById(noteId)) {
                Result.Loading -> Unit
                is Result.Success -> {
                    _listNote.value = _listNote.value.filterNot { it.id == noteId }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null,
                        canAddNewNote = !isNoteTodayAlreadyAdded()
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.error
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private companion object {
        val APP_ZONE: ZoneId = ZoneId.of("Asia/Jakarta")
    }
}
