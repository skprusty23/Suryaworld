package com.personaltracker.util

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.personaltracker.PersonalTrackerApp

object NotificationUtils {

    fun showEmiReminder(context: Context, emiName: String, amount: Double, daysLeft: Int) {
        val notification = NotificationCompat.Builder(context, PersonalTrackerApp.CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("EMI Due Soon: $emiName")
            .setContentText("₹${String.format("%.0f", amount)} due in $daysLeft days")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(emiName.hashCode(), notification)
    }

    fun showDocumentExpiryReminder(context: Context, docName: String, daysLeft: Int) {
        val notification = NotificationCompat.Builder(context, PersonalTrackerApp.CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Document Expiring: $docName")
            .setContentText("Expires in $daysLeft days - please renew")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(docName.hashCode(), notification)
    }

    fun showInvestmentMaturityReminder(context: Context, investmentName: String, daysLeft: Int) {
        val notification = NotificationCompat.Builder(context, PersonalTrackerApp.CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Investment Maturing: $investmentName")
            .setContentText("Matures in $daysLeft days")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(investmentName.hashCode(), notification)
    }
}
