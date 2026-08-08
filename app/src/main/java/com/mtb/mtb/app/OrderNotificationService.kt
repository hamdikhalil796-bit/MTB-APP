package com.mtb.mtb.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mtb.mtb.MainActivity
import com.mtb.mtb.R

const val NEW_ORDER_NOTIFICATION_CHANNEL_ID = "new_orders"
const val NEW_ORDER_TOPIC = "admin_orders"

/**
 * Receives the push sent by the "notifyAdminsOnNewOrder" Cloud Function whenever a new
 * document is created in the "orders" Firestore collection. Only devices subscribed to
 * [NEW_ORDER_TOPIC] (i.e. logged-in admins, see MainApp) receive it.
 */
class OrderNotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.notif_new_order_title)
        val body = message.notification?.body ?: message.data["body"] ?: getString(R.string.notif_new_order_body)

        ensureChannel()

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NEW_ORDER_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_order)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NEW_ORDER_NOTIFICATION_CHANNEL_ID,
                getString(R.string.notif_channel_new_orders),
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // No per-device token storage needed: notifications are sent via the "admin_orders"
        // FCM topic, and the SDK re-applies existing topic subscriptions to new tokens itself.
    }
}
