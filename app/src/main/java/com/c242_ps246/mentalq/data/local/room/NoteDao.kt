package com.c242_ps246.mentalq.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

enum class NoteReconcileAction {
    COMPLETE,
    CONTINUE_UPDATE,
    DELETE_REMOTE
}

data class NoteReconcileResult(
    val action: NoteReconcileAction,
    val nextNoteId: String? = null
)

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNotes(notes: List<NoteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Query("SELECT * FROM note ORDER BY createdAt DESC")
    suspend fun getAllNotes(): List<NoteEntity>

    @Query("SELECT * FROM note ORDER BY createdAt DESC")
    fun observeAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE pendingAction IS NOT NULL")
    suspend fun getPendingNotes(): List<NoteEntity>

    @Query("SELECT * FROM note WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query("DELETE FROM note")
    suspend fun clearAllNotes()

    @Query("DELETE FROM note WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    @Query(
        "DELETE FROM note WHERE pendingAction IS NULL " +
            "AND (content IS NULL OR TRIM(content) = '')"
    )
    suspend fun deleteEmptySyncedNotes()

    @Transaction
    suspend fun reconcileRemoteNotes(remoteNotes: List<NoteEntity>) {
        remoteNotes.forEach { remoteNote ->
            val localNote = getNoteById(remoteNote.id)
            if (localNote?.pendingAction != null) return@forEach

            val localUpdatedAt = localNote?.updatedAt.orEmpty()
            val remoteUpdatedAt = remoteNote.updatedAt.orEmpty()
            if (localNote == null || localUpdatedAt.isEmpty() || remoteUpdatedAt >= localUpdatedAt) {
                insertNote(remoteNote.copy(pendingAction = null))
            }
        }
        deleteEmptySyncedNotes()
    }

    @Transaction
    suspend fun reconcileCreatedNote(
        localId: String,
        sentNote: NoteEntity,
        serverNote: NoteEntity
    ): NoteReconcileResult {
        val currentNote = getNoteById(localId) ?: return NoteReconcileResult(
            NoteReconcileAction.COMPLETE
        )

        deleteNoteById(localId)
        if (currentNote.pendingAction == NotePendingAction.DELETE) {
            val deleteTombstone = serverNote.copy(pendingAction = NotePendingAction.DELETE)
            insertNote(deleteTombstone)
            return NoteReconcileResult(
                NoteReconcileAction.DELETE_REMOTE,
                deleteTombstone.id
            )
        }

        if (currentNote.hasDifferentEditableContent(sentNote)) {
            val pendingUpdate = currentNote.copy(
                id = serverNote.id,
                contentNormalized = null,
                predictedStatus = null,
                confidenceScore = null,
                createdAt = serverNote.createdAt ?: currentNote.createdAt,
                pendingAction = NotePendingAction.UPDATE
            )
            insertNote(pendingUpdate)
            return NoteReconcileResult(
                NoteReconcileAction.CONTINUE_UPDATE,
                pendingUpdate.id
            )
        }

        insertNote(serverNote.copy(pendingAction = null))
        return NoteReconcileResult(NoteReconcileAction.COMPLETE)
    }

    @Transaction
    suspend fun reconcileUpdatedNote(
        noteId: String,
        sentNote: NoteEntity,
        serverNote: NoteEntity
    ): NoteReconcileResult {
        val currentNote = getNoteById(noteId) ?: return NoteReconcileResult(
            NoteReconcileAction.COMPLETE
        )

        if (currentNote.pendingAction == NotePendingAction.DELETE) {
            return NoteReconcileResult(
                NoteReconcileAction.DELETE_REMOTE,
                currentNote.id
            )
        }

        if (currentNote.hasDifferentEditableContent(sentNote)) {
            return NoteReconcileResult(
                NoteReconcileAction.CONTINUE_UPDATE,
                currentNote.id
            )
        }

        insertNote(serverNote.copy(pendingAction = null))
        return NoteReconcileResult(NoteReconcileAction.COMPLETE)
    }

    @Transaction
    suspend fun deleteIfPending(noteId: String) {
        if (getNoteById(noteId)?.pendingAction == NotePendingAction.DELETE) {
            deleteNoteById(noteId)
        }
    }
}

private fun NoteEntity.hasDifferentEditableContent(other: NoteEntity): Boolean =
    title != other.title || content != other.content || emotion != other.emotion
