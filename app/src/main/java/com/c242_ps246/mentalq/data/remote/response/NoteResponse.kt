package com.c242_ps246.mentalq.data.remote.response

import com.google.gson.annotations.SerializedName

data class NoteResponse(
    @field:SerializedName("listNote")
    val listNote: List<ListNoteItem>? = null,

    @field:SerializedName("error")
    val error: Boolean? = null,

    @field:SerializedName("message")
    val message: String? = null
)

data class DeleteNoteResponse(
    @field:SerializedName("error")
    val error: Boolean? = null,

    @field:SerializedName("message")
    val message: String? = null
)

data class DetailNoteResponse(
    @field:SerializedName("note")
    val note: ListNoteItem? = null,

    @field:SerializedName("error")
    val error: Boolean? = null,

    @field:SerializedName("message")
    val message: String? = null
)

data class ListNoteItem(
    @field:SerializedName("note_id")
    val id: String,

    @field:SerializedName("title")
    val title: String? = null,

    @field:SerializedName("content")
    val content: String? = null,

    @field:SerializedName("content_normalized")
    val contentNormalized: String? = null,

    @field:SerializedName("predicted_status")
    val predictedStatus: String? = null,

    @field:SerializedName("confidence_score")
    val confidenceScore: Float? = null,

    @field:SerializedName("emotion")
    val emotion: String? = null,

    @field:SerializedName("updatedAt")
    val updatedAt: String? = null,

    @field:SerializedName("createdAt")
    val createdAt: String? = null,

    // Local-only state. The backend never reads this field because note writes
    // use explicit form fields in NoteApiService.
    val pendingAction: String? = null
)
