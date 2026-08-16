package com.c242_ps246.mentalq.data.repository

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.c242_ps246.mentalq.data.local.room.NoteDao
import com.c242_ps246.mentalq.data.local.room.NoteEntity
import com.c242_ps246.mentalq.data.local.room.NotePendingAction
import com.c242_ps246.mentalq.data.local.room.NoteReconcileAction
import com.c242_ps246.mentalq.data.local.room.toEntity
import com.c242_ps246.mentalq.data.local.room.toModel
import com.c242_ps246.mentalq.data.remote.response.ListNoteItem
import com.c242_ps246.mentalq.data.remote.retrofit.NoteApiService
import com.c242_ps246.mentalq.data.sync.NoteSyncWorker
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

enum class NoteSyncResult {
    SUCCESS,
    RETRY
}

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val noteApiService: NoteApiService,
    private val workManager: WorkManager
) {
    fun getAllNotes(): Flow<Result<List<ListNoteItem>>> = callbackFlow {
        trySend(Result.Loading)

        val localJob = launch {
            noteDao.observeAllNotes().collect { storedNotes ->
                trySend(Result.Success(storedNotes.toVisibleModels()))
            }
        }

        val refreshJob = launch(Dispatchers.IO) {
            noteDao.getPendingNotes().forEach { pendingNote ->
                enqueueNoteSync(pendingNote.id, ExistingWorkPolicy.KEEP)
            }

            try {
                val remoteNotes = noteApiService.getNotes().listNote
                    .orEmpty()
                    .filter(ListNoteItem::hasUsableContent)
                    .map { it.copy(pendingAction = null).toEntity() }
                noteDao.reconcileRemoteNotes(remoteNotes)
            } catch (error: Exception) {
                if (noteDao.getAllNotes().toVisibleModels().isEmpty()) {
                    trySend(Result.Error(error.toUserMessage("Unable to fetch notes")))
                }
            }
        }

        awaitClose {
            localJob.cancel()
            refreshJob.cancel()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getNoteById(noteId: String): ListNoteItem? =
        runCatching { noteDao.getNoteById(noteId)?.toModel() }.getOrNull()

    suspend fun insertNote(note: ListNoteItem): Result<ListNoteItem> {
        if (!note.hasUsableContent()) {
            return Result.Error("Please write something before saving the note")
        }

        return try {
            val now = Instant.now().toString()
            val localNote = note.copy(
                id = "$LOCAL_ID_PREFIX${UUID.randomUUID()}",
                title = note.title.orEmpty().trim(),
                content = note.content.orEmpty().trim(),
                contentNormalized = null,
                predictedStatus = null,
                confidenceScore = null,
                emotion = note.emotion.orEmpty().trim(),
                updatedAt = now,
                createdAt = note.createdAt ?: now,
                pendingAction = NotePendingAction.CREATE
            )
            noteDao.insertNote(localNote.toEntity())
            enqueueNoteSync(localNote.id)
            Result.Success(localNote)
        } catch (error: Exception) {
            Result.Error(error.toUserMessage("Unable to save the note locally"))
        }
    }

    suspend fun updateNote(note: ListNoteItem): Result<ListNoteItem> {
        if (!note.hasUsableContent()) {
            return Result.Error("Please write something before saving the note")
        }

        return try {
            val storedNote = noteDao.getNoteById(note.id)
                ?: return Result.Error("Note not found")
            val normalizedContent = note.content.orEmpty().trim()
            val contentChanged = normalizedContent != storedNote.content
            val pendingAction = if (
                storedNote.pendingAction == NotePendingAction.CREATE ||
                note.id.startsWith(LOCAL_ID_PREFIX)
            ) {
                NotePendingAction.CREATE
            } else {
                NotePendingAction.UPDATE
            }
            val localNote = note.copy(
                title = note.title.orEmpty().trim(),
                content = normalizedContent,
                contentNormalized = if (contentChanged) null else storedNote.contentNormalized,
                predictedStatus = if (contentChanged) null else storedNote.predictedStatus,
                confidenceScore = if (contentChanged) null else storedNote.confidenceScore,
                emotion = note.emotion.orEmpty().trim(),
                updatedAt = Instant.now().toString(),
                createdAt = storedNote.createdAt,
                pendingAction = pendingAction
            )
            noteDao.insertNote(localNote.toEntity())
            enqueueNoteSync(localNote.id)
            Result.Success(localNote)
        } catch (error: Exception) {
            Result.Error(error.toUserMessage("Unable to save the note locally"))
        }
    }

    suspend fun deleteNoteById(noteId: String): Result<String> = try {
        val note = noteDao.getNoteById(noteId) ?: return Result.Success("Note deleted")
        noteDao.insertNote(
            note.copy(
                updatedAt = Instant.now().toString(),
                pendingAction = NotePendingAction.DELETE
            )
        )
        enqueueNoteSync(noteId)
        Result.Success("Note deleted")
    } catch (error: Exception) {
        Result.Error(error.toUserMessage("Unable to delete the note locally"))
    }

    suspend fun syncPendingNote(initialNoteId: String): NoteSyncResult {
        var noteId = initialNoteId

        return try {
            repeat(MAX_IMMEDIATE_SYNC_STEPS) {
                val pendingNote = noteDao.getNoteById(noteId) ?: return NoteSyncResult.SUCCESS
                when (pendingNote.pendingAction) {
                    NotePendingAction.CREATE -> {
                        val response = noteApiService.createNote(
                            title = pendingNote.title.orEmpty(),
                            content = pendingNote.content.orEmpty(),
                            emotion = pendingNote.emotion.orEmpty()
                        )
                        if (response.error == true) return NoteSyncResult.RETRY
                        val serverNote = response.note
                            ?.copy(pendingAction = null)
                            ?.toEntity()
                            ?: return NoteSyncResult.RETRY
                        val reconciliation = noteDao.reconcileCreatedNote(
                            localId = pendingNote.id,
                            sentNote = pendingNote,
                            serverNote = serverNote
                        )
                        when (reconciliation.action) {
                            NoteReconcileAction.COMPLETE -> return NoteSyncResult.SUCCESS
                            NoteReconcileAction.CONTINUE_UPDATE,
                            NoteReconcileAction.DELETE_REMOTE -> {
                                noteId = reconciliation.nextNoteId ?: serverNote.id
                            }
                        }
                    }

                    NotePendingAction.UPDATE -> {
                        val response = noteApiService.updateNote(
                            id = pendingNote.id,
                            title = pendingNote.title.orEmpty(),
                            content = pendingNote.content.orEmpty(),
                            emotion = pendingNote.emotion.orEmpty()
                        )
                        if (response.error == true) return NoteSyncResult.RETRY
                        val serverNote = response.note
                            ?.copy(pendingAction = null)
                            ?.toEntity()
                            ?: return NoteSyncResult.RETRY
                        val reconciliation = noteDao.reconcileUpdatedNote(
                            noteId = pendingNote.id,
                            sentNote = pendingNote,
                            serverNote = serverNote
                        )
                        when (reconciliation.action) {
                            NoteReconcileAction.COMPLETE -> return NoteSyncResult.SUCCESS
                            NoteReconcileAction.CONTINUE_UPDATE,
                            NoteReconcileAction.DELETE_REMOTE -> {
                                noteId = reconciliation.nextNoteId ?: pendingNote.id
                            }
                        }
                    }

                    NotePendingAction.DELETE -> {
                        if (!pendingNote.id.startsWith(LOCAL_ID_PREFIX)) {
                            val response = noteApiService.deleteNote(pendingNote.id)
                            if (response.error == true) return NoteSyncResult.RETRY
                        }
                        noteDao.deleteIfPending(pendingNote.id)
                        return NoteSyncResult.SUCCESS
                    }

                    else -> return NoteSyncResult.SUCCESS
                }
            }

            enqueueNoteSync(noteId, ExistingWorkPolicy.APPEND_OR_REPLACE)
            NoteSyncResult.SUCCESS
        } catch (_: Exception) {
            if (noteId != initialNoteId) {
                enqueueNoteSync(noteId, ExistingWorkPolicy.APPEND_OR_REPLACE)
                NoteSyncResult.SUCCESS
            } else {
                NoteSyncResult.RETRY
            }
        }
    }

    suspend fun getLastNote(): ListNoteItem? = runCatching {
        noteDao.getAllNotes()
            .toVisibleModels()
            .maxByOrNull { it.createdAt.orEmpty() }
    }.getOrNull()

    private fun enqueueNoteSync(
        noteId: String,
        policy: ExistingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE
    ) {
        val request = OneTimeWorkRequestBuilder<NoteSyncWorker>()
            .setInputData(
                Data.Builder()
                    .putString(NoteSyncWorker.INPUT_NOTE_ID, noteId)
                    .build()
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(
            "${NoteSyncWorker.UNIQUE_WORK_PREFIX}$noteId",
            policy,
            request
        )
    }

    private companion object {
        const val LOCAL_ID_PREFIX = "local-"
        const val MAX_IMMEDIATE_SYNC_STEPS = 4
    }
}

private fun List<NoteEntity>.toVisibleModels(): List<ListNoteItem> =
    asSequence()
        .filter { it.pendingAction != NotePendingAction.DELETE }
        .map { it.toModel() }
        .filter(ListNoteItem::hasUsableContent)
        .sortedByDescending { it.createdAt }
        .toList()

private fun ListNoteItem.hasUsableContent(): Boolean = !content.isNullOrBlank()
