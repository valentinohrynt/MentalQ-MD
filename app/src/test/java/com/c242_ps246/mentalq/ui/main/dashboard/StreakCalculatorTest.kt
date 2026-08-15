package com.c242_ps246.mentalq.ui.main.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {
    private val today = LocalDate.of(2026, 8, 14)

    @Test
    fun emptyHistoryHasNoStreak() {
        assertEquals(StreakInfo(), StreakCalculator.calculate(emptyList(), today))
    }

    @Test
    fun consecutiveDaysAndDuplicatesCountOnce() {
        val result = StreakCalculator.calculate(
            listOf(today.minusDays(2), today, today.minusDays(1), today),
            today
        )

        assertEquals(3, result.currentStreak)
        assertEquals(today, result.lastEntryDate)
    }

    @Test
    fun yesterdayCanContinueCurrentStreak() {
        val result = StreakCalculator.calculate(
            listOf(today.minusDays(1), today.minusDays(2)),
            today
        )

        assertEquals(2, result.currentStreak)
        assertEquals(today.minusDays(1), result.lastEntryDate)
    }

    @Test
    fun staleHistoryResetsStreakButKeepsLastDate() {
        val lastEntry = today.minusDays(2)
        val result = StreakCalculator.calculate(listOf(lastEntry, today.minusDays(3)), today)

        assertEquals(0, result.currentStreak)
        assertEquals(lastEntry, result.lastEntryDate)
    }

    @Test
    fun gapStopsOlderDaysFromBeingCounted() {
        val result = StreakCalculator.calculate(
            listOf(today, today.minusDays(1), today.minusDays(3), today.minusDays(4)),
            today
        )

        assertEquals(2, result.currentStreak)
    }
}
