package com.personaltracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PersonalTrackerApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            listOf(
                NotificationChannel(
                    CHANNEL_REMINDERS,
                    getString(R.string.notification_channel_reminders),
                    NotificationManager.IMPORTANCE_DEFAULT
                ),
                NotificationChannel(
                    CHANNEL_EXPENSE,
                    getString(R.string.notification_channel_expense),
                    NotificationManager.IMPORTANCE_DEFAULT
                ),
                NotificationChannel(
                    CHANNEL_BACKUP,
                    getString(R.string.notification_channel_backup),
                    NotificationManager.IMPORTANCE_LOW
                )
            ).forEach { manager.createNotificationChannel(it) }
        }
    }

    companion object {
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_EXPENSE = "expense"
        const val CHANNEL_BACKUP = "backup"
    }
}
