package com.mtb.mtb.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.mtb.mtb.MainActivity
import com.mtb.mtb.R
import java.util.concurrent.TimeUnit

const val MTB_PREFS = "MTB_PREFS"

/**
 * "New order" notifications without any server code (Cloud Functions aren't enabled on the
 * Firebase project): the app notices new orders itself — instantly through the live orders
 * listener while it runs (MainApp), and every ~15 minutes through [NewOrderCheckWorker] while
 * it's closed. A new order is a *current* order whose id this phone hasn't seen yet; ids rather
 * than timestamps, because each order's timestamp comes from the clock of whichever device
 * placed it, and a computer running a few minutes behind would make its orders look old.
 * The notification only says that a new order was placed — no client, quantity or price.
 */
object NewOrderNotifier {
    private const val KNOWN_IDS_KEY = "KNOWN_CURRENT_ORDER_IDS"
    // Orders placed from this phone, kept until they show up in a snapshot so they never notify here.
    private const val OWN_IDS_KEY = "OWN_PENDING_ORDER_IDS"
    private const val WORK_NAME = "new-order-check"
    // One notification that updates in place instead of piling up.
    private const val NOTIFICATION_ID = 4201

    private fun prefs(context: Context) = context.getSharedPreferences(MTB_PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun markOwnOrder(context: Context, orderId: String) {
        val prefs = prefs(context)
        val own = prefs.getStringSet(OWN_IDS_KEY, emptySet()).orEmpty() + orderId
        prefs.edit().putStringSet(OWN_IDS_KEY, own.toSet()).apply()
    }

    /**
     * Takes the ids of all current orders, remembers them, and returns how many weren't known
     * before. The very first call on a phone only records them, so existing orders aren't announced.
     */
    @Synchronized
    fun recordCurrentOrders(context: Context, currentIds: Set<String>): Int {
        val prefs = prefs(context)
        val known = prefs.getStringSet(KNOWN_IDS_KEY, null)?.toSet()
        val own = prefs.getStringSet(OWN_IDS_KEY, emptySet()).orEmpty().toSet()
        prefs.edit()
            .putStringSet(KNOWN_IDS_KEY, currentIds)
            .putStringSet(OWN_IDS_KEY, own - currentIds)
            .apply()
        if (known == null) return 0
        return (currentIds - known - own).size
    }

    /** False when Android or the user blocks this app's notifications (or the "new orders" channel). */
    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        val channel = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(NEW_ORDER_NOTIFICATION_CHANNEL_ID)
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun openNotificationSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun show(context: Context, count: Int) {
        if (count <= 0) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                NEW_ORDER_NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.notif_channel_new_orders),
                NotificationManager.IMPORTANCE_HIGH
            )
        )

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = if (count == 1) {
            context.getString(R.string.notif_new_order_body)
        } else {
            context.getString(R.string.notif_new_orders_body, count)
        }
        val notification = NotificationCompat.Builder(context, NEW_ORDER_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_order)
            .setContentTitle(context.getString(R.string.notif_new_order_title))
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun scheduleBackgroundCheck(context: Context) {
        val request = PeriodicWorkRequest.Builder(NewOrderCheckWorker::class.java, 15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancelBackgroundCheck(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
