package com.c242_ps246.mentalq.data.repository

import com.c242_ps246.mentalq.data.local.room.NoteDao
import com.c242_ps246.mentalq.data.local.room.toEntity
import com.c242_ps246.mentalq.data.local.room.toModel
import com.c242_ps246.mentalq.data.remote.response.ListNoteItem
import com.c242_ps246.mentalq.data.remote.retrofit.NoteApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val noteApiService: NoteApiService
) {
    fun getAllNotes(): Flow<Result<List<ListNoteItem>>> = flow {
        emit(Result.Loading)
        var hasDiscardedLocalDrafts = false
        val localNotes = try {
            val storedNotes = noteDao.getAllNotes().map { it.toModel() }
            val usableNotes = storedNotes
                .filter(ListNoteItem::hasUsableContent)
                .sortedByDescending { it.createdAt }
            hasDiscardedLocalDrafts = usableNotes.size != storedNotes.size
            usableNotes
        } catch (error: Exception) {
            emit(Result.Error(error.toUserMessage("Unable to read saved notes")))
            return@flow
        }

        if (localNotes.isNotEmpty()) emit(Result.Success(localNotes))

        try {
            val remoteNotes = noteApiService.getNotes().listNote
                .orEmpty()
                .filter(ListNoteItem::hasUsableContent)
                .sortedByDescending { it.createdAt }
            if (remoteNotes != localNotes || hasDiscardedLocalDrafts) {
                noteDao.replaceAllNotes(remoteNotes.map { it.toEntity() })
            }
            emit(Result.Success(remoteNotes))
        } catch (error: Exception) {
            if (localNotes.isEmpty()) {
                emit(Result.Error(error.toUserMessage("Unable to fetch notes")))
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getNoteById(noteId: String): ListNoteItem? =
        runCatching { noteDao.getNoteById(noteId)?.toModel() }.getOrNull()

    suspend fun insertNote(note: ListNoteItem): Result<ListNoteItem> =
        if (!note.hasUsableContent()) {
            Result.Error("Please write something before saving the note")
        } else try {
            val response = noteApiService.createNote(
                title = note.title.orEmpty().trim(),
                content = note.content.orEmpty().trim(),
                emotion = note.emotion.orEmpty().trim()
            )
            val createdNote = response.note
            if (response.error == true || createdNote == null) {
                Result.Error(response.message ?: "Unable to create the note")
            } else {
                noteDao.insertNote(createdNote.toEntity())
                Result.Success(createdNote)
            }
        } catch (error: Exception) {
            Result.Error(error.toUserMessage("Unable to create the note"))
        }

    suspend fun updateNote(note: ListNoteItem): Result<ListNoteItem> =
        if (!note.hasUsableContent()) {
            Result.Error("Please write something before saving the note")
        } else try {
            val response = noteApiService.updateNote(
                id = note.id,
                title = note.title.orEmpty().trim(),
                content = note.content.orEmpty().trim(),
                emotion = note.emotion.orEmpty().trim()
            )

            if (response.error == true) {
                Result.Error(response.message ?: "Unable to update the note")
            } else {
                val updatedNote = response.note ?: note.copy(contentNormalized = null)
                noteDao.updateNote(updatedNote.toEntity())
                Result.Success(updatedNote)
            }
        } catch (error: Exception) {
            Result.Error(error.toUserMessage("Unable to update the note"))
        }

    suspend fun deleteNoteById(noteId: String): Result<String> = try {
        val response = noteApiService.deleteNote(noteId)
        if (response.error == true) {
            Result.Error(response.message ?: "Unable to delete the note")
        } else {
            noteDao.deleteNoteById(noteId)
            Result.Success(response.message ?: "Note deleted")
        }
    } catch (error: Exception) {
        Result.Error(error.toUserMessage("Unable to delete the note"))
    }

    suspend fun getLastNote(): ListNoteItem? = runCatching {
        noteDao.getAllNotes()
            .asSequence()
            .map { it.toModel() }
            .filter(ListNoteItem::hasUsableContent)
            .maxByOrNull { it.createdAt.orEmpty() }
    }.getOrNull()
}

private fun ListNoteItem.hasUsableContent(): Boolean = !content.isNullOrBlank()
