package com.mtb.mtb.app.screens

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mtb.mtb.R
import com.mtb.mtb.app.AppUpdateInfo
import com.mtb.mtb.app.AppUpdateManager
import com.mtb.mtb.app.MtbTextSizes
import com.mtb.mtb.app.MtbWebColors
import com.mtb.mtb.ui.theme.NeumorphicCard

private enum class UpdateStage { IDLE, DOWNLOADING, NEEDS_INSTALL_PERMISSION, ERROR }

/**
 * Optional updates behave like the other dialogs in the app (dismissible, "Later" always
 * available). Mandatory ones reuse NoNetworkScreen.kt's non-dismissible pattern instead —
 * no back-press, no tap-outside, no "Later" button — since staying on the old version
 * isn't safe for that release.
 */
@Composable
fun UpdateDialog(
    update: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var stage by remember { mutableStateOf(UpdateStage.IDLE) }
    var downloadId by remember { mutableStateOf<Long?>(null) }

    fun startInstall(id: Long) {
        if (AppUpdateManager.canInstallUnknownApps(context)) {
            AppUpdateManager.installApk(context, id)
        } else {
            stage = UpdateStage.NEEDS_INSTALL_PERMISSION
        }
    }

    // ACTION_DOWNLOAD_COMPLETE fires on failure too, not just success — must check the
    // download's actual status, otherwise a failed download leaves the dialog stuck on
    // "Downloading…" forever (getUriForDownloadedFile silently returns null for it).
    fun handleDownloadComplete(id: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(id))
        val succeeded = cursor.use {
            it.moveToFirst() &&
                it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
        }
        if (succeeded) startInstall(id) else stage = UpdateStage.ERROR
    }

    // DownloadManager's completion broadcast comes from the system (a different UID), so
    // it needs RECEIVER_EXPORTED — RECEIVER_NOT_EXPORTED would silently never deliver it.
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (completedId != -1L && completedId == downloadId) {
                    handleDownloadComplete(completedId)
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Re-check the install-unknown-apps permission on returning from Settings, so the
    // install intent fires automatically instead of making the user tap "Update now" again.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val id = downloadId
            if (event == Lifecycle.Event.ON_RESUME &&
                stage == UpdateStage.NEEDS_INSTALL_PERMISSION &&
                id != null &&
                AppUpdateManager.canInstallUnknownApps(context)
            ) {
                AppUpdateManager.installApk(context, id)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Dialog(
        onDismissRequest = { if (!update.mandatory) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !update.mandatory,
            dismissOnClickOutside = !update.mandatory,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedDialogSurface {
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
                shape = RoundedCornerShape(16.dp),
                containerColor = MtbWebColors.CardWhite
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.update_dialog_title),
                        color = MtbWebColors.HeadingBrown,
                        fontSize = MtbTextSizes.heading,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.update_dialog_message, update.versionName),
                        color = MtbWebColors.TextDark,
                        fontSize = MtbTextSizes.body
                    )

                    if (update.mandatory) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.update_mandatory_message),
                            color = MtbWebColors.DeleteRed,
                            fontSize = MtbTextSizes.label,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (update.releaseNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.update_release_notes_label),
                            color = MtbWebColors.HeadingBrown,
                            fontSize = MtbTextSizes.label,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = update.releaseNotes,
                            color = MtbWebColors.TextDark,
                            fontSize = MtbTextSizes.body
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MtbWebColors.BgBeige, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    when (stage) {
                        UpdateStage.IDLE -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!update.mandatory) {
                                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                                        Text(stringResource(R.string.update_later_button), color = MtbWebColors.HeadingBrown)
                                    }
                                }
                                MtbButton(
                                    text = stringResource(R.string.update_now_button),
                                    onClick = {
                                        stage = UpdateStage.DOWNLOADING
                                        downloadId = AppUpdateManager.enqueueDownload(context, update.apkUrl, update.versionName)
                                    },
                                    containerColor = MtbWebColors.SecondaryBrown,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        UpdateStage.DOWNLOADING -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MtbWebColors.SecondaryBrown
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    stringResource(R.string.update_downloading),
                                    color = MtbWebColors.TextDark,
                                    fontSize = MtbTextSizes.body
                                )
                            }
                        }

                        UpdateStage.NEEDS_INSTALL_PERMISSION -> {
                            Text(
                                text = stringResource(R.string.update_install_permission_message),
                                color = MtbWebColors.TextDark,
                                fontSize = MtbTextSizes.body
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            MtbButton(
                                text = stringResource(R.string.update_install_permission_button),
                                onClick = { context.startActivity(AppUpdateManager.unknownAppsSettingsIntent(context)) },
                                containerColor = MtbWebColors.SecondaryBrown,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        UpdateStage.ERROR -> {
                            Text(
                                text = stringResource(R.string.update_error),
                                color = MtbWebColors.DeleteRed,
                                fontSize = MtbTextSizes.body
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            MtbButton(
                                text = stringResource(R.string.update_now_button),
                                onClick = {
                                    stage = UpdateStage.DOWNLOADING
                                    downloadId = AppUpdateManager.enqueueDownload(context, update.apkUrl, update.versionName)
                                },
                                containerColor = MtbWebColors.SecondaryBrown,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            }
        }
    }
}
