package com.c242_ps246.mentalq.data.remote.retrofit

import com.c242_ps246.mentalq.data.remote.response.TransactionResponse
import com.c242_ps246.mentalq.data.remote.response.TransactionStatusResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MidtransApiService {

    @GET("transaction/{id}")
    suspend fun getTransactionStatus(
        @Path("id") id: String,
    ): TransactionStatusResponse

    @FormUrlEncoded
    @POST("transaction")
    suspend fun createTransaction(
        @Field("item_id") itemId: String,
    ): TransactionResponse

    @POST("transaction/{id}/cancel")
    suspend fun cancelTransaction(
        @Path("id") id: String
    ): TransactionStatusResponse
}
