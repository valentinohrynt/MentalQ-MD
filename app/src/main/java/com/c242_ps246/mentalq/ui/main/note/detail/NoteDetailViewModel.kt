package com.c242_ps246.mentalq.ui.main.note.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.c242_ps246.mentalq.data.remote.response.ListNoteItem
import com.c242_ps246.mentalq.data.local.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteDetailUiState(
    val isLoading: Boolean = true,
    val note: ListNoteItem? = null,
    val error: String? = null
)

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()

    private val _content = MutableStateFlow("")
    val content = _content.asStateFlow()

    private val _date = MutableStateFlow("")
    val date = _date.asStateFlow()

    private val _emotion = MutableStateFlow<String?>(null)
    val emotion = _emotion.asStateFlow()

    fun loadNote(noteId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val note = noteRepository.getNoteById(noteId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    note = note
                )
                _title.value = note.title.toString()
                _content.value = note.content.toString()
                _date.value = note.createdAt.toString()
                _emotion.value = note.emotion
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _title.value = newTitle
        updateLocalNote()
    }

    fun updateContent(newContent: String) {
        _content.value = newContent
        updateLocalNote()
    }

    fun updateEmotion(newEmotion: String) {
        _emotion.value = newEmotion
        updateLocalNote()
    }

    private fun updateLocalNote() {
        viewModelScope.launch {
            try {
                _uiState.value.note?.let { note ->
                    noteRepository.updateLocalNote(
                        note.copy(
                            title = _title.value,
                            content = _content.value,
                            emotion = _emotion.value
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateRemoteNote() {
        viewModelScope.launch {
            try {
                _uiState.value.note?.let { note ->
                    noteRepository.updateRemoteNote(
                        note.copy(
                            title = _title.value,
                            content = _content.value,
                            emotion = _emotion.value
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
