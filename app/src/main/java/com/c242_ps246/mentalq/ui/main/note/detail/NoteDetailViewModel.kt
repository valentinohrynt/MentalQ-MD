package com.c242_ps246.mentalq.ui.main.note.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.c242_ps246.mentalq.data.remote.response.ListNoteItem
import com.c242_ps246.mentalq.data.repository.NoteRepository
import com.c242_ps246.mentalq.data.repository.Result
import com.c242_ps246.mentalq.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
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
    private val _uiState = MutableStateFlow(NoteDetailUiState(isLoading = true))
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
        val shouldRestoreDraft = savedStateHandle.get<String>(DRAFT_NOTE_ID) == noteId

        if (noteId == Routes.NEW_NOTE_ID) {
            val restoredTitle = if (shouldRestoreDraft) title.value else ""
            val restoredContent = if (shouldRestoreDraft) content.value else ""
            val restoredEmotion = if (shouldRestoreDraft) emotion.value else ""
            val restoredDate = if (shouldRestoreDraft) {
                date.value.ifBlank { Instant.now().toString() }
            } else {
                Instant.now().toString()
            }
            val draft = ListNoteItem(
                id = Routes.NEW_NOTE_ID,
                title = restoredTitle,
                content = restoredContent,
                emotion = restoredEmotion,
                createdAt = restoredDate
            )
            _uiState.value = NoteDetailUiState(note = draft)
            savedStateHandle["title"] = restoredTitle
            savedStateHandle["content"] = restoredContent
            savedStateHandle["date"] = restoredDate
            savedStateHandle["emotion"] = restoredEmotion
            savedStateHandle[DRAFT_NOTE_ID] = noteId
            isDirty = restoredTitle.isNotBlank() || restoredContent.isNotBlank() ||
                restoredEmotion.isNotBlank()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val note = noteRepository.getNoteById(noteId)
            if (note == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Note not found")
                return@launch
            }
            val visibleNote = if (shouldRestoreDraft) {
                note.copy(
                    title = title.value,
                    content = content.value,
                    emotion = emotion.value
                )
            } else {
                note
            }
            _uiState.value = NoteDetailUiState(note = visibleNote)
            savedStateHandle["title"] = visibleNote.title.orEmpty()
            savedStateHandle["content"] = visibleNote.content.orEmpty()
            savedStateHandle["date"] = note.createdAt.orEmpty()
            savedStateHandle["emotion"] = visibleNote.emotion.orEmpty()
            savedStateHandle[DRAFT_NOTE_ID] = noteId
            isDirty = visibleNote.title != note.title || visibleNote.content != note.content ||
                visibleNote.emotion != note.emotion
        }
    }

    fun updateTitle(value: String) = updateDraft("title", value)

    fun updateContent(value: String) = updateDraft("content", value)

    fun updateEmotion(value: String) = updateDraft("emotion", value)

    private fun updateDraft(key: String, value: String) {
        savedStateHandle[key] = value
        isDirty = true
        _uiState.value.note?.let { note ->
            val updatedNote = when (key) {
                "title" -> note.copy(title = value)
                "content" -> note.copy(content = value)
                "emotion" -> note.copy(emotion = value)
                else -> note
            }
            _uiState.value = _uiState.value.copy(
                note = updatedNote,
                error = null,
                isSuccess = false
            )
        }
    }

    fun saveNoteImmediately() {
        updateJob?.cancel()
        val currentNote = _uiState.value.note ?: return
        val titleValue = title.value.trim()
        val contentValue = content.value.trim()
        val emotionValue = emotion.value.trim()
        val isNewNote = currentNote.id == Routes.NEW_NOTE_ID
        val isEmptyDraft = titleValue.isEmpty() &&
            contentValue.isEmpty() && emotionValue.isEmpty()

        if (isNewNote && isEmptyDraft) {
            _uiState.value = _uiState.value.copy(isSuccess = true)
            return
        }

        if (contentValue.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = "Please write something before saving the note",
                isSuccess = false
            )
            return
        }

        if (!isDirty && !isNewNote) {
            _uiState.value = _uiState.value.copy(isSuccess = true)
            return
        }

        updateJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val updatedNote = currentNote.copy(
                title = titleValue,
                content = contentValue,
                emotion = emotionValue
            )
            val result = if (isNewNote) {
                noteRepository.insertNote(updatedNote)
            } else {
                noteRepository.updateNote(updatedNote)
            }
            when (result) {
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private companion object {
        const val DRAFT_NOTE_ID = "draft_note_id"
    }
}
