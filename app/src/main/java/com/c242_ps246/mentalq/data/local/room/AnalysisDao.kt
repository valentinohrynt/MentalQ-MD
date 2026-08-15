package com.c242_ps246.mentalq.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface AnalysisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAnalysis(analysis: List<AnalysisEntity>)

    @Query("SELECT * FROM analysis")
    suspend fun getAllAnalysis(): List<AnalysisEntity>

    @Query("DELETE FROM analysis")
    suspend fun clearAllAnalysis()

    @Transaction
    suspend fun replaceAllAnalysis(analysis: List<AnalysisEntity>) {
        clearAllAnalysis()
        insertAllAnalysis(analysis)
    }
}
