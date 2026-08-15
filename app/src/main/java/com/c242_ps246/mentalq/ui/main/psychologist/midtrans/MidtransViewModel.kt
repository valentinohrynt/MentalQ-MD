package com.c242_ps246.mentalq.ui.main.psychologist.midtrans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.c242_ps246.mentalq.data.repository.MidtransRepository
import com.c242_ps246.mentalq.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MidtransScreenUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MidtransViewModel @Inject constructor(
    private val midtransRepository: MidtransRepository
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

    private val _chatId = MutableStateFlow<String?>(null)
    val chatId = _chatId.asStateFlow()

    fun loadPaymentResult(orderId: String) {
        viewModelScope.launch {
            setLoading()
            when (val result = midtransRepository.getTransactionStatus(orderId)) {
                Result.Loading -> Unit
                is Result.Error -> setError(result.error)
                is Result.Success -> {
                    _transactionStatus.value = result.data.transactionStatus
                    _transactionMessage.value = result.data.statusMessage
                    _chatId.value = result.data.chatId
                    if (result.data.transactionStatus in SUCCESSFUL_STATUSES
                        && result.data.chatId.isNullOrBlank()
                    ) {
                        setError(
                            result.data.chatError
                                ?: "Payment is confirmed, but the chat is not ready. Check again."
                        )
                    } else {
                        setSuccess()
                    }
                }
            }
        }
    }

    fun createTransaction(itemId: String) {
        viewModelScope.launch {
            setLoading()
            when (val result = midtransRepository.createTransaction(itemId)) {
                Result.Loading -> Unit
                is Result.Success -> {
                    _orderId.value = result.data.orderId
                    _redirectUrl.value = result.data.redirectUrl
                    setSuccess()
                }
                is Result.Error -> setError(result.error)
            }
        }
    }

    fun cancelTransaction(orderId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            setLoading()
            when (val result = midtransRepository.cancelTransaction(orderId)) {
                Result.Loading -> Unit
                is Result.Success -> {
                    _transactionStatus.value = result.data.transactionStatus
                    _transactionMessage.value = result.data.statusMessage
                    setSuccess()
                    onComplete()
                }
                is Result.Error -> setError(result.error)
            }
        }
    }

    private fun setLoading() {
        _uiState.value = MidtransScreenUiState(isLoading = true)
    }

    private fun setSuccess() {
        _uiState.value = MidtransScreenUiState(success = true)
    }

    private fun setError(message: String) {
        _uiState.value = MidtransScreenUiState(error = message)
    }

    private companion object {
        val SUCCESSFUL_STATUSES = setOf("settlement", "capture")
    }
}
