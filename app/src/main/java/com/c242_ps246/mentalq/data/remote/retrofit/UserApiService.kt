package com.c242_ps246.mentalq.data.remote.retrofit

import com.c242_ps246.mentalq.data.remote.response.UpdateProfileResponse
import com.c242_ps246.mentalq.data.remote.response.FirebaseTokenResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.POST

interface UserApiService {
    @POST("firebase-token")
    suspend fun createFirebaseToken(): Response<FirebaseTokenResponse>

    @Multipart
    @PUT("users/update")
    suspend fun updateProfile(
        @Part profileImage: MultipartBody.Part?,
        @Part("name") name: RequestBody,
        @Part("email") email: RequestBody,
        @Part("birthday") birthday: RequestBody
    ): Response<UpdateProfileResponse>

    @GET("user")
    suspend fun getUser(): Response<UpdateProfileResponse>
}
