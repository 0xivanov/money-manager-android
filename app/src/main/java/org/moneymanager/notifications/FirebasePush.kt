package org.moneymanager.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.moneymanager.BuildConfig
import org.moneymanager.MainActivity
import org.moneymanager.data.ApiClient
import org.moneymanager.data.TokenStore

const val EXTRA_PUSH_EVENT = "org.moneymanager.extra.PUSH_EVENT"

object FirebasePushConfiguration {
    fun initialize(context: Context, onToken: (String) -> Unit): Boolean {
        val configured = listOf(
            BuildConfig.FIREBASE_PROJECT_ID,
            BuildConfig.FIREBASE_APPLICATION_ID,
            BuildConfig.FIREBASE_API_KEY,
            BuildConfig.FIREBASE_SENDER_ID,
        ).all(String::isNotBlank)
        if (!configured) return false
        createMoneyAlertsChannel(context)
        if (FirebaseApp.getApps(context).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .setApplicationId(BuildConfig.FIREBASE_APPLICATION_ID)
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
                .build()
            FirebaseApp.initializeApp(context, options)
        }
        FirebaseMessaging.getInstance().register()
        FirebaseInstallations.getInstance().id.addOnSuccessListener(onToken)
        return true
    }
}

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MoneyManagerMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createMoneyAlertsChannel(this)
    }

    override fun onRegistered(token: String) {
        val accessToken = TokenStore(this).getToken() ?: return
        serviceScope.launch {
            runCatching { ApiClient(BuildConfig.API_BASE_URL).registerPushDevice(accessToken, token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "Money Manager"
        val body = message.notification?.body ?: message.data["body"] ?: return
        val eventType = message.data["event_type"].orEmpty()
        showMoneyAlert(this, title, body, eventType)
    }
}

fun createMoneyAlertsChannel(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(
        NotificationChannel(CHANNEL_ID, "Money alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Bank spending, budgets, scheduled money, and investment reminders"
        },
    )
}

private fun showMoneyAlert(context: Context, title: String, body: String, eventType: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return
    createMoneyAlertsChannel(context)
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_PUSH_EVENT, eventType)
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        eventType.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify((System.currentTimeMillis() and 0x7fffffff).toInt(), notification)
}

private const val CHANNEL_ID = "money_alerts"
