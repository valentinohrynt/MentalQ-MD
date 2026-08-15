package com.c242_ps246.mentalq.ui.main.note.detail

import androidx.lifecycle.SavedStateHandle
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
import javax.inject.Inject

data class NoteDetailUiState(
    val isLoading: Boolean = false,
    val note: ListNoteItem? = null,
    val error: String? = null,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false
)

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState = _uiState.asStateFlow()

    val title = savedStateHandle.getStateFlow("title", "")
    val content = savedStateHandle.getStateFlow("content", "")
    val emotion = savedStateHandle.getStateFlow("emotion", "")
    val date = savedStateHandle.getStateFlow("date", "")

    private var currentNoteId: String? = savedStateHandle["noteId"]
    private var isDirty = false
    private var updateJob: Job? = null

    fun loadNote(noteId: String) {
        if (currentNoteId == noteId && _uiState.value.note != null) return
        currentNoteId = noteId
        savedStateHandle["noteId"] = noteId

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val note = noteRepository.getNoteById(noteId)
            if (note == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Note not found")
                return@launch
            }
            _uiState.value = NoteDetailUiState(note = note)
            savedStateHandle["title"] = note.title.orEmpty()
            savedStateHandle["content"] = note.content.orEmpty()
            savedStateHandle["date"] = note.createdAt.orEmpty()
            savedStateHandle["emotion"] = note.emotion.orEmpty()
            isDirty = false
        }
    }

    fun updateTitle(value: String) = updateDraft("title", value)

    fun updateContent(value: String) = updateDraft("content", value)

    fun updateEmotion(value: String) = updateDraft("emotion", value)

    private fun updateDraft(key: String, value: String) {
        savedStateHandle[key] = value
        isDirty = true
        _uiState.value.note?.let { note ->
            _uiState.value = _uiState.value.copy(
                note = note.copy(
                    title = title.value,
                    content = content.value,
                    emotion = emotion.value
                ),
                isSuccess = false
            )
        }
    }

    fun saveNoteImmediately() {
        updateJob?.cancel()
        val currentNote = _uiState.value.note ?: return
        if (!isDirty) {
            _uiState.value = _uiState.value.copy(isSuccess = true)
            return
        }

        updateJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val updatedNote = currentNote.copy(
                title = title.value,
                content = content.value,
                emotion = emotion.value
            )
            when (val result = noteRepository.updateNote(updatedNote)) {
                Result.Loading -> Unit
                is Result.Success -> {
                    isDirty = false
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        isSuccess = true,
                        note = result.data
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = result.error
                )
            }
        }
    }
}
