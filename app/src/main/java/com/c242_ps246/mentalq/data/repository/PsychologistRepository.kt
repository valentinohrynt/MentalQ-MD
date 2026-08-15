package com.c242_ps246.mentalq.data.repository

import com.c242_ps246.mentalq.data.remote.response.PsychologistItem
import com.c242_ps246.mentalq.data.remote.retrofit.PsychologistApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PsychologistRepository @Inject constructor(
    private val psychologistApiService: PsychologistApiService
) {

    suspend fun getPsychologists(): Result<List<PsychologistItem>> = try {
            val response = psychologistApiService.getPsychologists()
            if (response.error == true) {
                Result.Error(response.message ?: "Unable to fetch psychologists")
            } else {
                Result.Success(response.listPsychologists.orEmpty())
            }
        } catch (error: Exception) {
            Result.Error(error.toUserMessage("Unable to fetch psychologists"))
        }

    suspend fun getPsychologistById(psychologistId: String): Result<PsychologistItem> = try {
            val response = psychologistApiService.getPsychologistById(psychologistId)
            if (response.error == true || response.psychologist == null) {
                Result.Error(response.message ?: "Unable to fetch the psychologist")
            } else {
                Result.Success(response.psychologist)
            }
        } catch (error: Exception) {
            Result.Error(error.toUserMessage("Unable to fetch the psychologist"))
        }
}
