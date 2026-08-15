package com.c242_ps246.mentalq.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNotes(notes: List<NoteEntity>)

    @Transaction
    suspend fun replaceAllNotes(notes: List<NoteEntity>) {
        clearAllNotes()
        insertAllNotes(notes)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("SELECT * FROM note")
    suspend fun getAllNotes(): List<NoteEntity>

    @Query("SELECT * FROM note WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query("DELETE FROM note")
    suspend fun clearAllNotes()

    @Query("DELETE FROM note WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    @Query("SELECT * FROM note ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastNote(): NoteEntity?
}
