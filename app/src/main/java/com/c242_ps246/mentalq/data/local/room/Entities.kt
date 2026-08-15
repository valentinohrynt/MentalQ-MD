package com.c242_ps246.mentalq.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String? = null,
    val content: String? = null,
    val contentNormalized: String? = null,
    val predictedStatus: String? = null,
    val confidenceScore: Float? = null,
    val emotion: String? = null,
    val updatedAt: String? = null,
    val createdAt: String? = null
)

@Entity(tableName = "user_data")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val birthday: String? = null,
    val profilePhotoUrl: String? = null,
    val role: String? = null
)

@Entity(tableName = "analysis")
data class AnalysisEntity(
    @PrimaryKey val id: String,
    val noteId: String? = null,
    val predictedStatus: String,
    val confidenceScore: Float? = null,
    val updatedAt: String? = null,
    val createdAt: String? = null
)
