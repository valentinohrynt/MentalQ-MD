package com.c242_ps246.mentalq.data.local.room

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteDaoReconciliationTest {
    @Test
    fun `completed create replaces local id and keeps server diagnosis`() = runBlocking {
        val dao = FakeNoteDao()
        val local = note(
            id = "local-1",
            content = "Today was good",
            pendingAction = NotePendingAction.CREATE
        )
        val server = note(
            id = "42",
            content = "Today was good",
            predictedStatus = "Normal"
        )
        dao.insertNote(local)

        val result = dao.reconcileCreatedNote(local.id, local, server)

        assertEquals(NoteReconcileAction.COMPLETE, result.action)
        assertNull(dao.getNoteById(local.id))
        assertEquals("Normal", dao.getNoteById(server.id)?.predictedStatus)
        assertNull(dao.getNoteById(server.id)?.pendingAction)
    }

    @Test
    fun `late create response never overwrites a newer local edit`() = runBlocking {
        val dao = FakeNoteDao()
        val sent = note(
            id = "local-1",
            content = "First version",
            pendingAction = NotePendingAction.CREATE
        )
        dao.insertNote(
            sent.copy(
                content = "Edited while syncing",
                updatedAt = "2026-08-17T01:01:00Z"
            )
        )
        val server = note(
            id = "42",
            content = "First version",
            predictedStatus = "Normal"
        )

        val result = dao.reconcileCreatedNote(sent.id, sent, server)
        val pendingUpdate = dao.getNoteById("42")

        assertEquals(NoteReconcileAction.CONTINUE_UPDATE, result.action)
        assertEquals("Edited while syncing", pendingUpdate?.content)
        assertEquals(NotePendingAction.UPDATE, pendingUpdate?.pendingAction)
        assertNull(pendingUpdate?.predictedStatus)
    }

    @Test
    fun `remote refresh does not overwrite a pending local update`() = runBlocking {
        val dao = FakeNoteDao()
        dao.insertNote(
            note(
                id = "42",
                content = "Local edit",
                pendingAction = NotePendingAction.UPDATE
            )
        )

        dao.reconcileRemoteNotes(
            listOf(note(id = "42", content = "Older server value"))
        )

        assertEquals("Local edit", dao.getNoteById("42")?.content)
        assertEquals(NotePendingAction.UPDATE, dao.getNoteById("42")?.pendingAction)
    }

    private fun note(
        id: String,
        content: String,
        predictedStatus: String? = null,
        pendingAction: String? = null
    ) = NoteEntity(
        id = id,
        title = "Title",
        content = content,
        predictedStatus = predictedStatus,
        emotion = "Happy",
        updatedAt = "2026-08-17T01:00:00Z",
        createdAt = "2026-08-17T01:00:00Z",
        pendingAction = pendingAction
    )
}

private class FakeNoteDao : NoteDao {
    private val notes = linkedMapOf<String, NoteEntity>()

    override suspend fun insertAllNotes(notes: List<NoteEntity>) {
        notes.forEach { insertNote(it) }
    }

    override suspend fun insertNote(note: NoteEntity) {
        notes[note.id] = note
    }

    override suspend fun getAllNotes(): List<NoteEntity> = notes.values.toList()

    override fun observeAllNotes(): Flow<List<NoteEntity>> = flowOf(notes.values.toList())

    override suspend fun getPendingNotes(): List<NoteEntity> =
        notes.values.filter { it.pendingAction != null }

    override suspend fun getNoteById(id: String): NoteEntity? = notes[id]

    override suspend fun clearAllNotes() {
        notes.clear()
    }

    override suspend fun deleteNoteById(id: String) {
        notes.remove(id)
    }

    override suspend fun deleteEmptySyncedNotes() {
        notes.entries.removeAll { (_, note) ->
            note.pendingAction == null && note.content.isNullOrBlank()
        }
    }
}
