package com.c242_ps246.mentalq.data.remote.response

import com.google.gson.annotations.SerializedName

data class AnalysisResponse(
    @field:SerializedName("error")
    val error: Boolean? = null,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("listAnalysis")
    val listAnalysis: List<ListAnalysisItem>
)

data class ListAnalysisItem(
    @field:SerializedName("analysis_id")
    val id: String,

    @field:SerializedName("note_id")
    val noteId: String? = null,

    @field:SerializedName("predicted_status")
    val predictedStatus: String,

    @field:SerializedName("confidence_score")
    val confidenceScore: Float? = null,

    @field:SerializedName("updatedAt")
    val updatedAt: String? = null,

    @field:SerializedName("createdAt")
    val createdAt: String? = null
)
