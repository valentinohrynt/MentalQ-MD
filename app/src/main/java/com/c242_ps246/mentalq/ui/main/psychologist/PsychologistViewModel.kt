package com.c242_ps246.mentalq.ui.main.psychologist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.c242_ps246.mentalq.data.remote.response.PsychologistItem
import com.c242_ps246.mentalq.data.repository.AuthRepository
import com.c242_ps246.mentalq.data.repository.PsychologistRepository
import com.c242_ps246.mentalq.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PsychologistScreenUiState(
    val isLoading: Boolean = true,
    val success: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PsychologistViewModel @Inject constructor(
    private val psychologistRepository: PsychologistRepository,
    authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PsychologistScreenUiState())
    val uiState = _uiState.asStateFlow()

    private val _psychologists = MutableStateFlow<List<PsychologistItem>>(emptyList())
    val psychologists = _psychologists.asStateFlow()

    val userId = authRepository.getUserId()
        .map { it.ifBlank { null } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        loadPsychologists()
    }

    fun loadPsychologists() {
        viewModelScope.launch {
            _uiState.value = PsychologistScreenUiState(isLoading = true)
            when (val result = psychologistRepository.getPsychologists()) {
                Result.Loading -> Unit
                is Result.Success -> {
                    _psychologists.value = result.data
                    _uiState.value = PsychologistScreenUiState(isLoading = false, success = true)
                }
                is Result.Error -> _uiState.value = PsychologistScreenUiState(
                    isLoading = false,
                    error = result.error
                )
            }
        }
    }
}
