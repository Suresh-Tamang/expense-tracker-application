package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object NotificationHelper {
    private const val CHANNEL_ID = "budget_alerts_channel"
    private const val CHANNEL_NAME = "Budget Alerts"
    private const val CHANNEL_DESC = "Notifications for monthly budget limits"
    private const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun triggerBudgetAlert(context: Context, monthKey: String, totalSpent: Double, budget: Double, currencySymbol: String = "$") {
        // Create the notification channel first
        createNotificationChannel(context)

        // Check POST_NOTIFICATIONS permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Cannot post notification due to missing runtime permission
                return
            }
        }

        val percentage = (totalSpent / budget) * 100
        val isExceeded = totalSpent > budget
        val isNearLimit = totalSpent >= (budget * 0.8) && totalSpent <= budget

        if (!isExceeded && !isNearLimit) return

        val title = if (isExceeded) {
            "Budget Exceeded! ⚠️"
        } else {
            "Budget Limit Warning! 📊"
        }

        val contentText = if (isExceeded) {
            "You have spent $currencySymbol${String.format("%.2f", totalSpent)} which is over your $currencySymbol${String.format("%.2f", budget)} limit for $monthKey."
        } else {
            "You have used ${String.format("%.1f", percentage)}% of your monthly budget for $monthKey. Remaining: $currencySymbol${String.format("%.2f", budget - totalSpent)}"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Built-in android system icon
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
