package com.c242_ps246.mentalq.data.remote.response

import com.google.gson.annotations.SerializedName

data class TransactionResponse(
    @field:SerializedName("message")
    val message: String,

    @field:SerializedName("error")
    val error: Boolean? = null,

    @field:SerializedName("data")
    val dataTransaction: DataTransaction
)

data class DataTransaction(
    @field:SerializedName("token")
    val token: String,

    @field:SerializedName("redirect_url")
    val redirectUrl: String,

    @field:SerializedName("order_id")
    val orderId: String
)

data class TransactionStatusResponse(
    @field:SerializedName("message")
    val message: String,

    @field:SerializedName("error")
    val error: Boolean? = null,

    @field:SerializedName("data")
    val data: DataTransactionStatus
)

data class DataTransactionStatus(
    @field:SerializedName("transaction_status")
    val transactionStatus: String,

    @field:SerializedName("status_message")
    val statusMessage: String,

    @field:SerializedName("chat_id")
    val chatId: String? = null,

    @field:SerializedName("chat_error")
    val chatError: String? = null,
)
