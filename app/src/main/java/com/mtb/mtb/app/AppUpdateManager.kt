package com.mtb.mtb.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import com.google.firebase.firestore.FirebaseFirestore
import com.mtb.mtb.R

// No Play Store presence, so the standard Play Core in-app-update API doesn't apply here —
// this is a from-scratch check/download/install flow. Release metadata is published by
// hand via the Firebase Console into a single app_meta/latest_release document (see
// public/README.md for the release process); the APK itself is hosted on Firebase Hosting.
object AppUpdateManager {
    private const val COLLECTION = "app_meta"
    private const val DOCUMENT = "latest_release"
    private const val DOWNLOAD_FILE_NAME = "mtb-update.apk"

    fun checkForUpdate(
        db: FirebaseFirestore,
        currentVersionCode: Long,
        onResult: (AppUpdateInfo?) -> Unit
    ) {
        db.collection(COLLECTION).document(DOCUMENT).get()
            .addOnSuccessListener { snapshot ->
                val versionCode = snapshot.getLong("versionCode")
                val apkUrl = snapshot.getString("apkUrl")
                if (versionCode == null || apkUrl.isNullOrBlank() || versionCode <= currentVersionCode) {
                    onResult(null)
                    return@addOnSuccessListener
                }
                onResult(
                    AppUpdateInfo(
                        versionCode = versionCode,
                        versionName = snapshot.getString("versionName") ?: "",
                        apkUrl = apkUrl,
                        releaseNotes = snapshot.getString("releaseNotes") ?: "",
                        mandatory = snapshot.getBoolean("mandatory") ?: false
                    )
                )
            }
            .addOnFailureListener { onResult(null) }
    }

    fun installedVersionCode(context: Context): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            // Fail closed: if we can't even read our own version, never claim an update exists.
            Long.MAX_VALUE
        }
    }

    fun canInstallUnknownApps(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun unknownAppsSettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        )

    fun enqueueDownload(context: Context, apkUrl: String, versionName: String): Long {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle(context.getString(R.string.app_name))
            .setDescription(versionName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, DOWNLOAD_FILE_NAME)
            .setMimeType("application/vnd.android.package-archive")
        return downloadManager.enqueue(request)
    }

    fun installApk(context: Context, downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = downloadManager.getUriForDownloadedFile(downloadId) ?: return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
