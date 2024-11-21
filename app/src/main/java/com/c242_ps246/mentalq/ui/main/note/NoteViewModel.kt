package com.c242_ps246.mentalq.ui.main.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.c242_ps246.mentalq.data.remote.response.ListNoteItem
import com.c242_ps246.mentalq.data.local.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.c242_ps246.mentalq.data.local.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class NoteScreenUiState(
    val isLoading: Boolean = true,
    val note: ListNoteItem? = null,
    val success: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(NoteScreenUiState())
    val uiState = _uiState.asStateFlow()

    private val _listNote = MutableStateFlow<List<ListNoteItem>>(emptyList())
    val listNote = _listNote.asStateFlow()

    private val _navigateToNoteDetail = MutableStateFlow<String?>(null)
    val navigateToNoteDetail: StateFlow<String?> = _navigateToNoteDetail.asStateFlow()

    init {
        loadAllNotes()
    }

    fun loadAllNotes() {
        viewModelScope.launch {
            noteRepository.getAllNotes().observeForever { result ->
                when (result) {
                    Result.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }

                    is Result.Success -> {
                        _uiState.value =
                            _uiState.value.copy(isLoading = false, error = null)
                        _listNote.value = result.data
                    }

                    is Result.Error -> {
                        _uiState.value =
                            _uiState.value.copy(isLoading = false, error = result.error)
                    }
                }
            }
        }
    }

    fun addNote(note: ListNoteItem) {
        viewModelScope.launch {
            noteRepository.insertNote(note).observeForever { result ->
                when (result) {
                    Result.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }

                    is Result.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = null,
                            success = true
                        )
                        _navigateToNoteDetail.value = result.data.id
                        loadAllNotes()
                    }

                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.error
                        )
                    }
                }
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            noteRepository.deleteNoteById(noteId).observeForever { result ->
                when (result) {
                    Result.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }

                    is Result.Success -> {
                        _uiState.value =
                            _uiState.value.copy(isLoading = false, error = null, success = true)
                        loadAllNotes()
                    }

                    is Result.Error -> {
                        _uiState.value =
                            _uiState.value.copy(isLoading = false, error = result.error)
                    }
                }
            }
        }
    }

    fun navigateToNoteDetailCompleted() {
        _navigateToNoteDetail.value = null
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
