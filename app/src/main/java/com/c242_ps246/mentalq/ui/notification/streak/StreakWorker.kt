package com.c242_ps246.mentalq.ui.notification.streak

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.c242_ps246.mentalq.data.manager.MentalQAppPreferences
import com.c242_ps246.mentalq.ui.notification.dailyreminder.DailyReminderWorkerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class StreakWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val preferences = EntryPointAccessors.fromApplication(
                applicationContext,
                DailyReminderWorkerEntryPoint::class.java
            ).preferences()
            if (!preferences.getNotificationsState().first()) return Result.success()
            val streakInfo = preferences.getStreakInfo().first()

            streakInfo.let { (lastEntryDate, streakCount) ->
                val today = LocalDate.now()
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE

                if (lastEntryDate.isNotEmpty()) {
                    val lastEntryLocalDate = LocalDate.parse(lastEntryDate, formatter)
                    if (lastEntryLocalDate != today) {
                        showNotification(streakCount)
                    }
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(streakCount: Int) {
        val streakNotificationHelper = StreakNotificationHelper(applicationContext)
        streakNotificationHelper.showStreakNotification(streakCount)
    }

    companion object {
        fun scheduleNextNotification(context: Context) {
            val now = LocalDateTime.now()
            val nextMidnight = now.plusDays(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

            val initialDelay = Duration.between(now, nextMidnight).toMillis()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val dailyWorkRequest = PeriodicWorkRequestBuilder<StreakWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    StreakNotificationHelper.WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    dailyWorkRequest
                )
        }
    }
}
