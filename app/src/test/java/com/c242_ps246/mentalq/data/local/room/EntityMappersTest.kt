package com.c242_ps246.mentalq.data.local.room

import com.c242_ps246.mentalq.data.remote.response.ListAnalysisItem
import com.c242_ps246.mentalq.data.remote.response.ListNoteItem
import com.c242_ps246.mentalq.data.remote.response.UserData
import org.junit.Assert.assertEquals
import org.junit.Test

class EntityMappersTest {
    @Test
    fun noteRoundTripPreservesFields() {
        val note = ListNoteItem(
            id = "note-1",
            title = "Title",
            content = "Content",
            contentNormalized = "content",
            predictedStatus = "Normal",
            confidenceScore = 0.91f,
            emotion = "Happy",
            updatedAt = "2026-08-14T10:00:00Z",
            createdAt = "2026-08-14T09:00:00Z"
        )
        assertEquals(note, note.toEntity().toModel())
    }

    @Test
    fun userRoundTripPreservesFields() {
        val user = UserData("user-1", "Ayu", "ayu@example.com", "2000-01-01", null, "user")
        assertEquals(user, user.toEntity().toModel())
    }

    @Test
    fun analysisRoundTripPreservesFields() {
        val analysis = ListAnalysisItem("analysis-1", "note-1", "Normal", 0.88f, null, null)
        assertEquals(analysis, analysis.toEntity().toModel())
    }
}
