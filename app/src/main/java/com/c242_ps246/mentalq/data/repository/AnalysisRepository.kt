package com.c242_ps246.mentalq.data.repository

import com.c242_ps246.mentalq.data.local.room.AnalysisDao
import com.c242_ps246.mentalq.data.local.room.toEntity
import com.c242_ps246.mentalq.data.local.room.toModel
import com.c242_ps246.mentalq.data.remote.response.ListAnalysisItem
import com.c242_ps246.mentalq.data.remote.retrofit.AnalysisApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalysisRepository @Inject constructor(
    private val analysisApiService: AnalysisApiService,
    private val analysisDao: AnalysisDao
) {
    fun getAnalysis(): Flow<Result<Triple<List<ListAnalysisItem>, Int, String?>>> = flow {
        emit(Result.Loading)
        val localData = try {
            analysisDao.getAllAnalysis().map { it.toModel() }.takeLast(MAX_ANALYSIS_ITEMS)
        } catch (error: Exception) {
            emit(Result.Error(error.toUserMessage("Unable to read saved analysis")))
            return@flow
        }

        if (localData.isNotEmpty()) emit(Result.Success(localData.asSummary()))

        try {
            val remoteData = analysisApiService.getAnalysis().listAnalysis
                .takeLast(MAX_ANALYSIS_ITEMS)
            if (remoteData != localData) {
                analysisDao.replaceAllAnalysis(remoteData.map { it.toEntity() })
            }
            emit(Result.Success(remoteData.asSummary()))
        } catch (error: Exception) {
            if (localData.isEmpty()) {
                emit(Result.Error(error.toUserMessage("Unable to fetch analysis")))
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun List<ListAnalysisItem>.asSummary(): Triple<List<ListAnalysisItem>, Int, String?> {
        val mode = asSequence()
            .map { it.predictedStatus }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
        return Triple(this, size, mode)
    }

    private companion object {
        const val MAX_ANALYSIS_ITEMS = 28
    }
}
