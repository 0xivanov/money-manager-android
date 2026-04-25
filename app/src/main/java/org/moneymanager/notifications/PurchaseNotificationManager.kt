package org.moneymanager.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import org.moneymanager.MainActivity

const val ACTION_TRACK_PURCHASE = "org.moneymanager.action.TRACK_PURCHASE"

class PurchaseNotificationManager(private val context: Context) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Purchase reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Notifications for physical purchases detected by a wallet device"
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun showPurchaseDetectedNotification() {
        if (!canPostNotifications()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_TRACK_PURCHASE
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            TRACK_PURCHASE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Physical purchase detected")
            .setContentText("Track this expense in Money Manager?")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(TRACK_PURCHASE_NOTIFICATION_ID, notification)
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val CHANNEL_ID = "purchase_reminders"
        const val TRACK_PURCHASE_REQUEST_CODE = 1001
        const val TRACK_PURCHASE_NOTIFICATION_ID = 2001
    }
}
