package com.personaltracker.workers

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.personaltracker.PersonalTrackerApp
import com.personaltracker.domain.repository.EmiRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * A periodic WorkManager worker that runs once per day and notifies the user about any EMI
 * payments due within the next 3 days.
 *
 * Hilt injects [EmiRepository] (a singleton) via [AssistedInject]; the [Assisted]-annotated
 * [Context] and [WorkerParameters] are provided by the WorkManager runtime.
 */
@HiltWorker
class EmiReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val emiRepository: EmiRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            checkAndNotifyUpcomingEmis()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun checkAndNotifyUpcomingEmis() {
        val today = LocalDate.now()
        val todayDay = today.dayOfMonth

        // Collect active EMIs whose due day falls within the next 3 days (inclusive).
        val upcomingEmis = emiRepository.getAllActiveEmis().first().filter { emi ->
            val daysUntilDue = when {
                emi.dueDay >= todayDay -> emi.dueDay - todayDay
                else -> {
                    // Due day already passed this month — compute days until next month's due.
                    val daysInMonth = today.lengthOfMonth()
                    (daysInMonth - todayDay) + emi.dueDay
                }
            }
            daysUntilDue in 0..3
        }

        if (upcomingEmis.isEmpty()) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (upcomingEmis.size == 1) {
            val emi = upcomingEmis.first()
            val diff = emi.dueDay - todayDay
            val daysLabel = when {
                diff <= 0 -> "today"
                diff == 1 -> "tomorrow"
                else -> "in $diff days"
            }
            val notification = NotificationCompat.Builder(context, PersonalTrackerApp.CHANNEL_REMINDERS)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("EMI Due Soon")
                .setContentText("${emi.name} EMI of ₹${emi.emiAmount.toLong()} is due $daysLabel")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            manager.notify(NOTIFICATION_ID, notification)
        } else {
            val notification = NotificationCompat.Builder(context, PersonalTrackerApp.CHANNEL_REMINDERS)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("${upcomingEmis.size} EMIs Due Soon")
                .setContentText(upcomingEmis.joinToString(", ") { it.name })
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            upcomingEmis.joinToString("\n") { emi ->
                                "• ${emi.name} — ₹${emi.emiAmount.toLong()} on day ${emi.dueDay}"
                            }
                        )
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1002
        const val WORK_NAME = "emi_reminder"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<EmiReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /**
         * Returns the milliseconds until 09:00 AM (next occurrence), giving users a morning
         * heads-up about EMI due dates.
         */
        private fun calculateInitialDelay(): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }
    }
}
