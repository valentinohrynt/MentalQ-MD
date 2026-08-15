package com.c242_ps246.mentalq.ui.notification.dailyreminder

import com.c242_ps246.mentalq.data.manager.MentalQAppPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DailyReminderWorkerEntryPoint {
    fun injectDailyReminderWorker(worker: DailyReminderWorker)
    fun preferences(): MentalQAppPreferences
}
