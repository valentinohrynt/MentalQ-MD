package com.c242_ps246.mentalq.ui.main.psychologist.midtrans

import android.util.Log
import androidx.lifecycle.ViewModel
import com.c242_ps246.mentalq.data.remote.response.PsychologistItem
import com.c242_ps246.mentalq.data.repository.MidtransRepository
import com.c242_ps246.mentalq.data.repository.PsychologistRepository
import com.c242_ps246.mentalq.data.repository.Result
import com.c242_ps246.mentalq.ui.main.psychologist.PsychologistViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject


data class MidtransScreenUiState(
    val isLoading: Boolean = true,
    val success: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class MidtransViewModel @Inject constructor(
    private val midtransRepository: MidtransRepository,
    private val psychologistRepository: PsychologistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MidtransScreenUiState())
    val uiState = _uiState.asStateFlow()

    private val _orderId = MutableStateFlow<String?>(null)
    val orderId = _orderId.asStateFlow()

    private val _redirectUrl = MutableStateFlow<String?>(null)
    val redirectUrl = _redirectUrl.asStateFlow()

    private val _transactionStatus = MutableStateFlow<String?>(null)
    val transactionStatus = _transactionStatus.asStateFlow()

    private val _transactionMessage = MutableStateFlow<String?>(null)
    val transactionMessage = _transactionMessage.asStateFlow()

    private val _psychologistData = MutableStateFlow<PsychologistItem?>(null)
    val psychologistData = _psychologistData.asStateFlow()


    fun getPsychologistData(psychologistId: String) {
        Log.e("MidtransViewModel", "getPsychologistData: $psychologistId")
        psychologistRepository.getPsychologistById(psychologistId).observeForever { result ->
            when (result) {
                is Result.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }

                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _psychologistData.value = result.data
                }

                is Result.Error -> {
                    Log.e("MidtransViewModel", "fetchPsychologistData: ${result.error} ")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }


    fun createTransaction(price: Int, itemId: String) {
        midtransRepository.createTransaction(price, itemId).observeForever { result ->
            when (result) {
                is Result.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }

                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _orderId.value = result.data.orderId
                    _redirectUrl.value = result.data.redirectUrl
                }

                is Result.Error -> {
                    Log.e("MidtransViewModel", "createTransaction: ${result.error} ")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }

        }
    }

    fun getTransactionStatus(orderId: String) {
        midtransRepository.getTransactionStatus(orderId).observeForever { result ->
            when (result) {
                is Result.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }

                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _transactionStatus.value = result.data.transactionStatus
                    _transactionMessage.value = result.data.statusMessage

                    midtransRepository.getTransactionStatus(orderId).removeObserver { this }
                }

                is Result.Error -> {
                    Log.e("MidtransViewModel", "createTransaction: ${result.error} ")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun cancelTransaction(orderId: String) {
        midtransRepository.cancelTransaction(orderId).observeForever { result ->
            when (result) {
                is Result.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }

                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _transactionStatus.value = result.data.transactionStatus
                    _transactionMessage.value = result.data.statusMessage
                }

                is Result.Error -> {
                    Log.e("MidtransViewModel", "createTransaction: ${result.error} ")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }
}