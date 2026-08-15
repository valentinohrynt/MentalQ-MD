package com.c242_ps246.mentalq.data.repository

import com.c242_ps246.mentalq.data.remote.response.DataTransaction
import com.c242_ps246.mentalq.data.remote.response.DataTransactionStatus
import com.c242_ps246.mentalq.data.remote.retrofit.MidtransApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MidtransRepository @Inject constructor(
    private val midtransApiService: MidtransApiService
) {
    suspend fun createTransaction(itemId: String): Result<DataTransaction> =
            try {
                val response = midtransApiService.createTransaction(itemId)
                if (response.error == true) {
                    Result.Error(response.message)
                } else {
                    Result.Success(response.dataTransaction)
                }
            } catch (error: Exception) {
                Result.Error(error.toUserMessage("Unable to create the transaction"))
            }

    suspend fun getTransactionStatus(orderId: String): Result<DataTransactionStatus> = try {
            val response = midtransApiService.getTransactionStatus(orderId)
            if (response.error == true) Result.Error(response.message)
            else Result.Success(response.data)
        } catch (error: Exception) {
            Result.Error(error.toUserMessage("Unable to check the transaction"))
        }

    suspend fun cancelTransaction(orderId: String): Result<DataTransactionStatus> = try {
            val response = midtransApiService.cancelTransaction(orderId)
            if (response.error == true) Result.Error(response.message)
            else Result.Success(response.data)
        } catch (error: Exception) {
            Result.Error(error.toUserMessage("Unable to cancel the transaction"))
        }
}
