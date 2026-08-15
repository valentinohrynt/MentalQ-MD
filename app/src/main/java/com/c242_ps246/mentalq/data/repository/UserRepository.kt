package com.c242_ps246.mentalq.data.repository

import com.c242_ps246.mentalq.data.local.room.UserDao
import com.c242_ps246.mentalq.data.local.room.toEntity
import com.c242_ps246.mentalq.data.remote.response.UserData
import com.c242_ps246.mentalq.data.remote.retrofit.UserApiService
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userApiService: UserApiService,
    private val userDao: UserDao
) {
    suspend fun updateProfile(
        name: RequestBody,
        email: RequestBody,
        birthday: RequestBody,
        profileImage: MultipartBody.Part?
    ): Result<UserData?> = try {
            val response = userApiService.updateProfile(profileImage, name, email, birthday)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.error == true) {
                    Result.Error(body.message ?: "Unable to update the profile")
                } else {
                    userDao.clearUserData()
                    body?.user?.let { userDao.insertUser(it.toEntity()) }
                    Result.Success(body?.user)
                }
            } else {
                Result.Error(response.errorMessage("Unable to update the profile"))
            }
        } catch (error: Exception) {
            Result.Error(error.toUserMessage("Unable to update the profile"))
        }

    suspend fun getUserData(): Result<UserData?> = try {
            val response = userApiService.getUser()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.error == true) {
                    Result.Error(body.message ?: "Unable to fetch the user")
                } else {
                    Result.Success(body?.user)
                }
            } else {
                Result.Error(response.errorMessage("Unable to fetch the user"))
            }
        } catch (error: Exception) {
            Result.Error(error.toUserMessage("Unable to fetch the user"))
        }
}
