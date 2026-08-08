package com.mtb.mtb.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay

/** Online status plus a manual re-check, for a "Retry" button to call. */
class NetworkStatus(private val isOnlineState: State<Boolean>, val refresh: () -> Unit) {
    val isOnline: Boolean get() = isOnlineState.value
}

// How long the connection must stay down before we actually show the blocking screen.
// Android briefly reports NET_CAPABILITY_VALIDATED as false right after the device/app
// resumes (it's re-running its connectivity check) even though the connection is fine —
// without this grace period that shows up as a ~1s false "offline" flash on every reopen.
private const val OFFLINE_GRACE_PERIOD_MS = 1000L

/**
 * Live online/offline state, backed by ConnectivityManager's callback API so it reacts
 * to the network dropping or coming back at any point during the session, not just on
 * launch. "Online" requires an actual internet-validated network, not just any network
 * (e.g. a wifi with no internet still counts as offline) — but a drop only counts once
 * it's persisted past the grace period, to ride out momentary validation blips.
 */
@Composable
fun rememberNetworkStatus(): NetworkStatus {
    val context = LocalContext.current
    val connectivityManager = remember {
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }
    val rawIsOnline = remember { mutableStateOf(isCurrentlyOnline(connectivityManager)) }

    DisposableEffect(connectivityManager) {
        if (connectivityManager == null) {
            // Can't detect connectivity on this device: assume online rather than
            // permanently blocking the app behind a screen that can never clear.
            rawIsOnline.value = true
            return@DisposableEffect onDispose {}
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                rawIsOnline.value = isCurrentlyOnline(connectivityManager)
            }

            override fun onLost(network: Network) {
                rawIsOnline.value = isCurrentlyOnline(connectivityManager)
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                rawIsOnline.value = isCurrentlyOnline(connectivityManager)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)

        onDispose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    // NetworkCallback delivery can be suspended while the app is backgrounded/locked
    // (Doze, App Standby) — a network drop-and-recover that happens entirely while the
    // screen is off can be missed, leaving rawIsOnline stuck stale. Forcing a fresh
    // synchronous check on every resume closes that gap without waiting for a callback
    // that may never arrive.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, connectivityManager) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                rawIsOnline.value = isCurrentlyOnline(connectivityManager)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Debounced view: coming back online is reflected immediately, but going offline
    // only sticks if it's still down after the grace period — starts optimistic (true)
    // so a launch-time validation blip never flashes the screen in the first place.
    val debouncedIsOnline = remember { mutableStateOf(true) }
    LaunchedEffect(rawIsOnline.value) {
        if (rawIsOnline.value) {
            debouncedIsOnline.value = true
        } else {
            delay(OFFLINE_GRACE_PERIOD_MS)
            debouncedIsOnline.value = false
        }
    }

    return remember(connectivityManager) {
        NetworkStatus(debouncedIsOnline) { rawIsOnline.value = isCurrentlyOnline(connectivityManager) }
    }
}

private fun isCurrentlyOnline(connectivityManager: ConnectivityManager?): Boolean {
    val network = connectivityManager?.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
