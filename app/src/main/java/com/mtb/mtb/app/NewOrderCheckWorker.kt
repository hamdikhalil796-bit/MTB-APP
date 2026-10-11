package com.mtb.mtb.app

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

/**
 * Runs about every 15 minutes while the app is closed (scheduled by MainApp for profiles that
 * prepare orders): looks for current orders this phone hasn't seen yet and shows the
 * "new order" notification. Re-checks the saved login each time, so logging out, a disabled
 * profile or a removed permission stops the notifications.
 */
class NewOrderCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val session = applicationContext.getSharedPreferences(MTB_PREFS, Context.MODE_PRIVATE)
            .getString(SESSION_PREF, null)
        if (session == null || !session.startsWith(SESSION_PROFILE_PREFIX)) return Result.success()
        val username = session.removePrefix(SESSION_PROFILE_PREFIX)

        return try {
            val db = FirebaseFirestore.getInstance()
            val profile = Tasks.await(db.collection("profiles").document(username).get(), 30, TimeUnit.SECONDS)
            // Supervisor accounts only manage profiles, so they don't get order notifications.
            val allowed = profile.exists() &&
                profile.getBoolean("active") != false &&
                profile.getBoolean("isSupervisor") != true &&
                Permissions.fromMap(profile.get("permissions")).prepareOrders
            if (!allowed) return Result.success()

            val currentOrders = Tasks.await(
                db.collection("orders").whereEqualTo("status", OrderStatus.CURRENT).get(),
                30,
                TimeUnit.SECONDS
            )
            val count = NewOrderNotifier.recordCurrentOrders(applicationContext, currentOrders.documents.map { it.id }.toSet())
            NewOrderNotifier.show(applicationContext, count)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
