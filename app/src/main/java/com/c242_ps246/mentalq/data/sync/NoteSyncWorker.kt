package com.c242_ps246.mentalq.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.c242_ps246.mentalq.data.repository.NoteRepository
import com.c242_ps246.mentalq.data.repository.NoteSyncResult
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class NoteSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val noteRepository: NoteRepository by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            NoteSyncWorkerEntryPoint::class.java
        ).noteRepository()
    }

    override suspend fun doWork(): Result {
        val noteId = inputData.getString(INPUT_NOTE_ID) ?: return Result.failure()
        return when (noteRepository.syncPendingNote(noteId)) {
            NoteSyncResult.SUCCESS -> Result.success()
            NoteSyncResult.RETRY -> Result.retry()
        }
    }

    companion object {
        const val INPUT_NOTE_ID = "note_id"
        const val UNIQUE_WORK_PREFIX = "mentalq-note-sync-"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NoteSyncWorkerEntryPoint {
    fun noteRepository(): NoteRepository
}
