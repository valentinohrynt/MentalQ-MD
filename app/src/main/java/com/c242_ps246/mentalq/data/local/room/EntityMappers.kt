package com.c242_ps246.mentalq.data.local.room

import com.c242_ps246.mentalq.data.remote.response.ListAnalysisItem
import com.c242_ps246.mentalq.data.remote.response.ListNoteItem
import com.c242_ps246.mentalq.data.remote.response.UserData

fun ListNoteItem.toEntity() = NoteEntity(
    id, title, content, contentNormalized, predictedStatus, confidenceScore,
    emotion, updatedAt, createdAt, pendingAction
)

fun NoteEntity.toModel() = ListNoteItem(
    id, title, content, contentNormalized, predictedStatus, confidenceScore,
    emotion, updatedAt, createdAt, pendingAction
)

fun UserData.toEntity() = UserEntity(id, name, email, birthday, profilePhotoUrl, role)

fun UserEntity.toModel() = UserData(id, name, email, birthday, profilePhotoUrl, role)

fun ListAnalysisItem.toEntity() = AnalysisEntity(
    id, noteId, predictedStatus, confidenceScore, updatedAt, createdAt
)

fun AnalysisEntity.toModel() = ListAnalysisItem(
    id, noteId, predictedStatus, confidenceScore, updatedAt, createdAt
)
