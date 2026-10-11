package com.mtb.mtb.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import com.mtb.mtb.R
import com.mtb.mtb.app.screens.*
import com.mtb.mtb.ui.theme.MTBTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Saved login on this phone: "guest" or "profile:<username>" (see ProfileRepository).
internal const val SESSION_PREF = "MTB_SESSION"
internal const val SESSION_GUEST = "guest"
internal const val SESSION_PROFILE_PREFIX = "profile:"

@Composable
fun MainApp(db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    val context = LocalContext.current
    val networkStatus = rememberNetworkStatus()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message ->
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }

    // Firestore feedback messages resolved up front (stringResource is @Composable and
    // can't be called inside the non-composable Firestore success/failure lambdas below).
    val errorTemplate = stringResource(R.string.error_with_message)
    val msgOrderSaved = stringResource(R.string.msg_order_saved)
    val msgOrderDeleted = stringResource(R.string.msg_order_deleted)
    val msgOrderIdMissing = stringResource(R.string.msg_order_id_missing)
    val msgOrderUpdated = stringResource(R.string.msg_order_updated)
    val msgOrderFinished = stringResource(R.string.msg_order_finished)
    val msgProductDeleted = stringResource(R.string.msg_product_deleted)
    val msgProductDuplicateId = stringResource(R.string.msg_product_duplicate_id)
    val msgProductAdded = stringResource(R.string.msg_product_added)
    val msgProductUpdated = stringResource(R.string.msg_product_updated)
    val duplicateIdTemplate = stringResource(R.string.edit_product_duplicate_id)
    val msgProductInfoUpdated = stringResource(R.string.msg_product_info_updated)
    val msgStockNothingToSave = stringResource(R.string.msg_stock_nothing_to_save)
    val msgStockSavedWithErrors = stringResource(R.string.msg_stock_saved_with_errors)
    val msgStockUpdated = stringResource(R.string.msg_stock_updated)
    val msgMaintenanceAdded = stringResource(R.string.msg_maintenance_added)
    val msgMaintenanceDeleted = stringResource(R.string.msg_maintenance_deleted)
    val msgProfileDisabled = stringResource(R.string.login_error_disabled)
    val msgNotificationsBlocked = stringResource(R.string.msg_notifications_blocked)
    val guestLabel = stringResource(R.string.role_guest)

    // 1. Initialize SharedPreferences
    val sharedPreferences = context.getSharedPreferences(MTB_PREFS, Context.MODE_PRIVATE)

    // Force French on first launch (before any language preference is saved), regardless
    // of the device's system locale; afterwards respect whatever was explicitly chosen.
    LaunchedEffect(Unit) {
        val savedLanguage = sharedPreferences.getString("APP_LANGUAGE", LocaleHelper.FRENCH) ?: LocaleHelper.FRENCH
        if (LocaleHelper.currentLanguageCode(context) != savedLanguage) {
            LocaleHelper.setLanguage(context, savedLanguage)
        }
        if (!sharedPreferences.contains("APP_LANGUAGE")) {
            sharedPreferences.edit().putString("APP_LANGUAGE", LocaleHelper.FRENCH).apply()
        }
    }

    // Manual theme override — defaults to the system light/dark theme the first time, then
    // persists. Falls back to the legacy boolean DARK_MODE key so existing users' choice
    // carries over instead of silently resetting to light after this update.
    val systemDark = isSystemInDarkTheme()
    var appTheme by remember {
        mutableStateOf(
            when (sharedPreferences.getString("APP_THEME", null)) {
                AppTheme.LIGHT.name -> AppTheme.LIGHT
                AppTheme.DARK.name -> AppTheme.DARK
                else -> if (sharedPreferences.contains("DARK_MODE")) {
                    if (sharedPreferences.getBoolean("DARK_MODE", systemDark)) AppTheme.DARK else AppTheme.LIGHT
                } else if (systemDark) AppTheme.DARK else AppTheme.LIGHT
            }
        )
    }
    var showSettings by remember { mutableStateOf(false) }

    // The pre-profiles login stored just "ADMIN"/"GUEST" here; it grants nothing anymore.
    LaunchedEffect(Unit) { sharedPreferences.edit().remove("USER_ROLE").apply() }

    // --- Login session: who is logged in on this phone + their profile (or guest permissions) --
    var sessionKey by remember { mutableStateOf(sharedPreferences.getString(SESSION_PREF, null)) }
    fun setSessionKey(key: String?) {
        sharedPreferences.edit().apply {
            if (key == null) remove(SESSION_PREF) else putString(SESSION_PREF, key)
        }.apply()
        sessionKey = key
    }

    var session by remember { mutableStateOf<Session?>(null) }
    // False while the profile/guest permissions for the saved login are still loading.
    var sessionResolved by remember { mutableStateOf(false) }
    var sessionEndedMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(sessionKey) {
        val key = sessionKey
        session = null
        sessionResolved = key == null
        if (key == null) return@DisposableEffect onDispose { }
        val registration = if (key == SESSION_GUEST) {
            ProfileRepository.listenGuestPermissions { perms ->
                session = Session(profile = null, guestPermissions = perms)
                sessionResolved = true
            }
        } else {
            ProfileRepository.listenProfile(key.removePrefix(SESSION_PROFILE_PREFIX)) { profile ->
                if (profile == null || !profile.active) {
                    // Disabled or deleted by a supervisor: end the session right away.
                    sessionEndedMessage = msgProfileDisabled
                    setSessionKey(null)
                } else {
                    session = Session(profile = profile, guestPermissions = Permissions())
                    sessionResolved = true
                }
            }
        }
        onDispose { registration.remove() }
    }
    val permissions = session?.permissions ?: Permissions()
    val isSupervisor = session?.isSupervisor == true

    // Navigation back-stack: last entry is the visible screen. Hardware/gesture back
    // pops one level instead of exiting the app (only exits once back on "HOME"/"LOGIN").
    // Every cold launch starts on the splash screen, which then hands off to whichever
    // screen the saved session actually resolves to.
    var screenStack by remember { mutableStateOf(listOf("SPLASH")) }
    val currentScreen = screenStack.last()

    fun navigateTo(screen: String) {
        if (screenStack.last() != screen) {
            screenStack = screenStack + screen
        }
    }

    fun replaceTop(screen: String) {
        screenStack = screenStack.dropLast(1) + screen
    }

    // Bottom nav taps always resolve to a clean two-level stack (Home + destination,
    // or just Home), so hardware/gesture back from any tab predictably returns to Home
    // rather than replaying whatever deeper stack was built up beforehand.
    fun navigateToTab(screen: String) {
        screenStack = if (screen == "HOME") listOf("HOME") else listOf("HOME", screen)
    }

    BackHandler(enabled = screenStack.size > 1) {
        screenStack = screenStack.dropLast(1)
    }

    var splashDone by remember { mutableStateOf(false) }

    // Moves between LOGIN and the app whenever a session starts or ends (login, logout, or a
    // supervisor disabling the profile).
    LaunchedEffect(splashDone, sessionResolved, session != null) {
        if (!splashDone || !sessionResolved) return@LaunchedEffect
        if (session != null) {
            if (screenStack.last() == "LOGIN" || screenStack.last() == "SPLASH") {
                screenStack = listOf(if (session?.isSupervisor == true) "SUPERVISOR" else "HOME")
            }
        } else if (screenStack.last() != "LOGIN") {
            screenStack = listOf("LOGIN")
        }
    }

    // Supervisor accounts only manage profiles: the profiles screen is all they get.
    fun isScreenAllowed(screen: String): Boolean = if (isSupervisor) screen == "SUPERVISOR" else when (screen) {
        "NEW_ORDER" -> permissions.newOrder
        "CURRENT_ORDER" -> permissions.currentOrder
        "HISTORY" -> permissions.history
        "ARTICLES" -> permissions.articles
        "ADD_PRODUCT" -> permissions.editArticles
        "STOCK" -> permissions.stock
        "MAINTENANCE" -> permissions.maintenance
        "STATISTICS" -> permissions.statistics
        "SUPERVISOR" -> false
        else -> true
    }

    // Permissions can change live (a supervisor edits the profile): leave a screen as soon as
    // it's no longer allowed.
    LaunchedEffect(permissions, isSupervisor, currentScreen) {
        if (session != null && !isScreenAllowed(currentScreen)) {
            screenStack = listOf(if (isSupervisor) "SUPERVISOR" else "HOME")
        }
    }

    // Push notifications for new orders go to profiles that prepare orders, via the topic the
    // "notifyAdminsOnNewOrder" Cloud Function publishes to (see OrderNotificationService).
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op: if denied, the profile simply won't see the system notification */ }

    val wantsOrderNotifications = session?.isGuest == false && permissions.prepareOrders
    LaunchedEffect(wantsOrderNotifications) {
        if (wantsOrderNotifications) {
            NewOrderNotifier.scheduleBackgroundCheck(context)
            FirebaseMessaging.getInstance().subscribeToTopic(NEW_ORDER_TOPIC)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            NewOrderNotifier.cancelBackgroundCheck(context)
            FirebaseMessaging.getInstance().unsubscribeFromTopic(NEW_ORDER_TOPIC)
        }
    }
    // Read inside the long-lived orders listener below, which would otherwise keep the value
    // from when it was attached.
    val notifyNewOrders by rememberUpdatedState(wantsOrderNotifications)

    var products by remember { mutableStateOf(listOf<ProductItem>()) }
    var orders by remember { mutableStateOf(listOf<SavedOrder>()) }
    var maintenanceEntries by remember { mutableStateOf(listOf<MaintenanceEntry>()) }
    val currentOrders = orders.filter { it.status == OrderStatus.CURRENT }
    val completedOrders = orders.filter { it.status == OrderStatus.COMPLETED }

    var editingProductId by remember { mutableStateOf<String?>(null) }
    val productToEdit = products.find { it.id == editingProductId }

    var infoProductId by remember { mutableStateOf<String?>(null) }
    val productForInfo = products.find { it.id == infoProductId }

    // New state to handle order editing safely
    var editingOrder by remember { mutableStateOf<SavedOrder?>(null) }

    var availableUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
    LaunchedEffect(Unit) {
        AppUpdateManager.checkForUpdate(db, AppUpdateManager.installedVersionCode(context)) { update ->
            availableUpdate = update
        }
    }

    DisposableEffect(sessionKey) {
        if (sessionKey == null) {
            products = emptyList()
            orders = emptyList()
            maintenanceEntries = emptyList()
            return@DisposableEffect onDispose { }
        }
        val registrations = mutableListOf<ListenerRegistration>()

        // Fetch products from Firebase in real-time
        registrations += db.collection("products").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            products = snapshot.documents.mapNotNull { document ->
                document.toObject(ProductItem::class.java)
            }
        }

        // Fetch orders from Firebase in real-time
        registrations += db.collection("orders").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            // Always record what's been seen (so getting the permission later doesn't replay
            // old orders); only notify profiles that prepare orders.
            if (!snapshot.metadata.isFromCache) {
                val currentIds = snapshot.documents
                    .filter { it.getString("status") == OrderStatus.CURRENT }
                    .map { it.id }
                    .toSet()
                val newCount = NewOrderNotifier.recordCurrentOrders(context, currentIds)
                if (notifyNewOrders) NewOrderNotifier.show(context, newCount)
            }

            orders = snapshot.documents.mapNotNull { doc ->
                val order = doc.toObject(SavedOrder::class.java)
                // Fallback: If order.id is empty in Firestore, use the actual document ID to prevent crash
                order?.copy(id = if (order.id.isNotBlank()) order.id else doc.id)
            }
        }

        // Fetch maintenance log entries from Firebase in real-time
        registrations += db.collection("maintenance").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            maintenanceEntries = snapshot.documents.mapNotNull { doc ->
                val entry = doc.toObject(MaintenanceEntry::class.java)
                entry?.copy(id = if (entry.id.isNotBlank()) entry.id else doc.id)
            }
        }

        onDispose { registrations.forEach { it.remove() } }
    }

    MTBTheme(appTheme = appTheme) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (currentScreen != "LOGIN" && currentScreen != "SPLASH" && !isSupervisor) {
                    MtbBottomNavigationBar(
                        currentScreen = currentScreen,
                        permissions = permissions,
                        onNavigate = { screen -> navigateToTab(screen) }
                    )
                }
            }
        ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                        slideInVertically(animationSpec = tween(220, delayMillis = 90)) { height -> height / 8 })
                        .togetherWith(fadeOut(animationSpec = tween(90)))
                },
                label = "screenTransition"
            ) { screen ->
            when (screen) {
                "SPLASH" -> {
                    SplashScreen(onFinished = { splashDone = true })
                }

                "LOGIN" -> {
                    // null = still checking whether a supervisor exists yet (first setup).
                    var setupRequired by remember { mutableStateOf<Boolean?>(null) }
                    LaunchedEffect(Unit) {
                        ProfileRepository.checkFirstSetupDone { done -> setupRequired = !done }
                    }
                    // Success needs no handling here: the session effect above leaves LOGIN.
                    LoginScreen(
                        setupRequired = setupRequired,
                        sessionEndedMessage = sessionEndedMessage,
                        onLogin = { username, password, onError ->
                            sessionEndedMessage = null
                            ProfileRepository.checkLogin(username, password) { profile, e ->
                                if (profile != null) setSessionKey(SESSION_PROFILE_PREFIX + profile.username)
                                else onError(e ?: ProfileError.OTHER)
                            }
                        },
                        onGuest = { _ ->
                            sessionEndedMessage = null
                            setSessionKey(SESSION_GUEST)
                        },
                        onFirstSetup = { name, username, password, onError ->
                            sessionEndedMessage = null
                            ProfileRepository.createFirstSupervisor(name, username, password) { e ->
                                if (e != null) onError(e)
                                else setSessionKey(SESSION_PROFILE_PREFIX + ProfileRepository.normalizeUsername(username))
                            }
                        }
                    )
                }

                "HOME" -> {
                    HomeScreen(
                        permissions = permissions,
                        welcomeName = session?.profile?.let { it.displayName.ifBlank { it.username } } ?: guestLabel,
                        onNewOrderClick = { navigateTo("NEW_ORDER") },
                        onViewCurrentOrderClick = { navigateTo("CURRENT_ORDER") },
                        onViewHistoryClick = { navigateTo("HISTORY") },
                        onViewArticlesClick = { navigateTo("ARTICLES") },
                        onAddProductClick = { navigateTo("ADD_PRODUCT") },
                        onViewStockClick = { navigateTo("STOCK") },
                        onViewMaintenanceClick = { navigateTo("MAINTENANCE") },
                        onViewStatisticsClick = { navigateTo("STATISTICS") },
                        onSettingsClick = { showSettings = true }
                    )
                }

                "NEW_ORDER" -> {
                    OrderFormScreen(
                        products = products,
                        onSaveOrderClick = { rows, totalQty, totalWeight, client, orderNumber, onComplete ->
                            val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                            val orderId = "CMD_${System.currentTimeMillis()}"
                            val newOrder = SavedOrder(
                                id = orderId,
                                client = client,
                                orderNumber = orderNumber,
                                date = currentDate,
                                totalQty = totalQty,
                                totalWeight = totalWeight,
                                items = rows,
                                timestamp = System.currentTimeMillis(),
                                status = OrderStatus.CURRENT
                            )
                            NewOrderNotifier.markOwnOrder(context, orderId)

                            db.collection("orders").document(orderId)
                                .set(newOrder)
                                .addOnSuccessListener {
                                    showMessage(msgOrderSaved)
                                    replaceTop("CURRENT_ORDER")
                                    onComplete(true, "")
                                }
                                .addOnFailureListener { e ->
                                    val formatted = String.format(errorTemplate, e.message)
                                    showMessage(formatted)
                                    onComplete(false, formatted)
                                }
                        },
                        showMessage = showMessage
                    )
                }

                "HISTORY" -> {
                    OrdersHistoryScreen(
                        orders = completedOrders,
                        canEditOrders = permissions.editOrders,
                        onDeleteOrderClick = { orderId ->
                            // Safe check to prevent IllegalArgumentException crash
                            if (orderId.isNotBlank()) {
                                db.collection("orders").document(orderId).delete()
                                    .addOnSuccessListener {
                                        showMessage(msgOrderDeleted)
                                    }
                                    .addOnFailureListener { e ->
                                        showMessage(String.format(errorTemplate, e.message))
                                    }
                            } else {
                                showMessage(msgOrderIdMissing)
                            }
                        },
                        onEditOrderClick = { order ->
                            // Sets the state to show the edit dialog safely
                            editingOrder = order
                        },
                        showMessage = showMessage
                    )
                }

                "CURRENT_ORDER" -> {
                    CurrentOrderScreen(
                        orders = currentOrders,
                        products = products,
                        permissions = permissions,
                        onDeleteOrderClick = { orderId ->
                            if (orderId.isNotBlank()) {
                                db.collection("orders").document(orderId).delete()
                                    .addOnSuccessListener {
                                        showMessage(msgOrderDeleted)
                                    }
                                    .addOnFailureListener { e ->
                                        showMessage(String.format(errorTemplate, e.message))
                                    }
                            } else {
                                showMessage(msgOrderIdMissing)
                            }
                        },
                        onEditOrderClick = { order ->
                            editingOrder = order
                        },
                        onFinishOrderClick = { order ->
                            db.collection("orders").document(order.id)
                                .update("status", OrderStatus.COMPLETED)
                                .addOnSuccessListener {
                                    showMessage(msgOrderFinished)
                                }
                                .addOnFailureListener { e ->
                                    showMessage(String.format(errorTemplate, e.message))
                                }
                        },
                        onToggleItemChecked = { order, itemIndex, checked ->
                            if (itemIndex in order.items.indices) {
                                val updatedItems = order.items.toMutableList().apply {
                                    set(itemIndex, this[itemIndex].copy(checked = checked))
                                }
                                db.collection("orders").document(order.id)
                                    .update("items", updatedItems)
                                    .addOnFailureListener { e ->
                                        showMessage(String.format(errorTemplate, e.message))
                                    }
                            }
                        },
                        onToggleItemUrgent = { order, itemIndex, urgent ->
                            if (itemIndex in order.items.indices) {
                                val updatedItems = order.items.toMutableList().apply {
                                    set(itemIndex, this[itemIndex].copy(urgent = urgent))
                                }
                                db.collection("orders").document(order.id)
                                    .update("items", updatedItems)
                                    .addOnFailureListener { e ->
                                        showMessage(String.format(errorTemplate, e.message))
                                    }
                            }
                        },
                        onUpdateReadyQty = { order, itemIndex, readyQty ->
                            if (itemIndex in order.items.indices) {
                                val updatedItems = order.items.toMutableList().apply {
                                    set(itemIndex, this[itemIndex].copy(readyQty = readyQty))
                                }
                                db.collection("orders").document(order.id)
                                    .update("items", updatedItems)
                                    .addOnFailureListener { e ->
                                        showMessage(String.format(errorTemplate, e.message))
                                    }
                            }
                        },
                        onViewArticleInfoClick = { productId -> infoProductId = productId },
                        showMessage = showMessage
                    )
                }

                "ARTICLES" -> {
                    ArticlesScreen(
                        products = products,
                        canEdit = permissions.editArticles,
                        onAddNewItemClick = { navigateTo("ADD_PRODUCT") },
                        onDeleteClick = { productId ->
                            if (productId.isNotBlank()) {
                                db.collection("products").document(productId).delete()
                                    .addOnSuccessListener {
                                        showMessage(msgProductDeleted)
                                    }
                            }
                        },
                        onViewInfoClick = { productId -> infoProductId = productId },
                        onEditClick = { productId ->
                            editingProductId = productId
                        },
                        showMessage = showMessage
                    )
                }

                "ADD_PRODUCT" -> {
                    AddProductScreen(
                        onViewAllClick = { navigateTo("ARTICLES") },
                        onSaveProduct = { id, reference, weight, photoUri, onResult ->
                            if (products.any { it.id == id }) {
                                onResult(false, msgProductDuplicateId)
                            } else {
                                val newProduct = ProductItem(
                                    id = id,
                                    reference = reference,
                                    weight = weight,
                                    photoUri = photoUri
                                )
                                db.collection("products").document(id).set(newProduct)
                                    .addOnSuccessListener {
                                        onResult(true, msgProductAdded)
                                    }
                                    .addOnFailureListener { e ->
                                        onResult(false, String.format(errorTemplate, e.message))
                                    }
                            }
                        }
                    )
                }

                "STOCK" -> {
                    StockScreen(
                        products = products,
                        onSaveStockClick = { stockMap, onComplete ->
                            if (stockMap.isEmpty()) {
                                showMessage(msgStockNothingToSave)
                                onComplete(true, "")
                            } else {
                                var remaining = stockMap.size
                                var hadError = false
                                stockMap.forEach { (productId, qty) ->
                                    db.collection("products").document(productId)
                                        .update("stock", qty)
                                        .addOnCompleteListener {
                                            remaining -= 1
                                            if (!it.isSuccessful) hadError = true
                                            if (remaining == 0) {
                                                val msg = if (hadError) msgStockSavedWithErrors else msgStockUpdated
                                                showMessage(msg)
                                                onComplete(!hadError, msg)
                                            }
                                        }
                                }
                            }
                        },
                        onRemoveClick = { productId ->
                            db.collection("products").document(productId)
                                .update("stock", "0")
                                .addOnSuccessListener {
                                    showMessage(msgStockUpdated)
                                }
                        },
                        canEdit = permissions.editStock
                    )
                }

                "STATISTICS" -> {
                    StatisticsScreen(orders = orders)
                }

                "SUPERVISOR" -> {
                    SupervisorScreen(
                        currentUsername = session?.profile?.username,
                        showMessage = showMessage,
                        onSettingsClick = { showSettings = true }
                    )
                }

                "MAINTENANCE" -> {
                    MaintenanceScreen(
                        entries = maintenanceEntries,
                        onAddEntryClick = { title, date, notes ->
                            val entryId = "MAINT_${System.currentTimeMillis()}"
                            val newEntry = MaintenanceEntry(
                                id = entryId,
                                title = title,
                                date = date,
                                notes = notes,
                                timestamp = System.currentTimeMillis()
                            )
                            db.collection("maintenance").document(entryId)
                                .set(newEntry)
                                .addOnSuccessListener {
                                    showMessage(msgMaintenanceAdded)
                                }
                                .addOnFailureListener { e ->
                                    showMessage(String.format(errorTemplate, e.message))
                                }
                        },
                        onDeleteEntryClick = { entryId ->
                            if (entryId.isNotBlank()) {
                                db.collection("maintenance").document(entryId).delete()
                                    .addOnSuccessListener {
                                        showMessage(msgMaintenanceDeleted)
                                    }
                                    .addOnFailureListener { e ->
                                        showMessage(String.format(errorTemplate, e.message))
                                    }
                            }
                        },
                        canEdit = permissions.editMaintenance
                    )
                }
            }
            }

            // Edit Product Dialog
            if (productToEdit != null) {
                EditProductDialog(
                    product = productToEdit,
                    onDismiss = { editingProductId = null },
                    onConfirm = { newId, newReference, newWeight, newPhotoUri ->
                        if (newId == productToEdit.id) {
                            val updatedMap = mapOf(
                                "reference" to newReference,
                                "weight" to newWeight,
                                "photoUri" to newPhotoUri
                            )
                            db.collection("products").document(productToEdit.id)
                                .update(updatedMap)
                                .addOnSuccessListener {
                                    showMessage(msgProductUpdated)
                                }
                            editingProductId = null
                        } else {
                            // Firestore document IDs are immutable, so renaming moves the
                            // data to a new document and drops the old one. The transaction
                            // keeps the duplicate-ID check and the move atomic.
                            val oldRef = db.collection("products").document(productToEdit.id)
                            val newRef = db.collection("products").document(newId)
                            db.runTransaction { tx ->
                                val newSnap = tx.get(newRef)
                                if (newSnap.exists()) {
                                    throw FirebaseFirestoreException(
                                        "duplicate product id",
                                        FirebaseFirestoreException.Code.ALREADY_EXISTS
                                    )
                                }
                                val oldSnap = tx.get(oldRef)
                                val oldProduct = oldSnap.toObject(ProductItem::class.java) ?: ProductItem()
                                val newProduct = oldProduct.copy(
                                    id = newId,
                                    reference = newReference,
                                    weight = newWeight,
                                    photoUri = newPhotoUri
                                )
                                tx.set(newRef, newProduct)
                                tx.delete(oldRef)
                            }.addOnSuccessListener {
                                showMessage(msgProductUpdated)
                                editingProductId = null
                            }.addOnFailureListener { e ->
                                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.ALREADY_EXISTS) {
                                    showMessage(String.format(duplicateIdTemplate, newId))
                                } else {
                                    showMessage(String.format(errorTemplate, e.message))
                                    editingProductId = null
                                }
                            }
                        }
                    },
                    showMessage = showMessage
                )
            }

            // Article Info Dialog (photo + notes, opened from ArticlesScreen's "Voir")
            if (productForInfo != null) {
                ArticleInfoDialog(
                    product = productForInfo,
                    canEdit = permissions.editArticles,
                    onDismiss = { infoProductId = null },
                    onSave = { newPhotoUri, newNotes ->
                        val updatedMap = mapOf(
                            "photoUri" to newPhotoUri,
                            "notes" to newNotes
                        )
                        db.collection("products").document(productForInfo.id)
                            .update(updatedMap)
                            .addOnSuccessListener {
                                showMessage(msgProductInfoUpdated)
                            }
                            .addOnFailureListener { e ->
                                showMessage(String.format(errorTemplate, e.message))
                            }
                        infoProductId = null
                    },
                    showMessage = showMessage
                )
            }

            // Edit Order Dialog
            if (editingOrder != null) {
                EditOrderDialog(
                    order = editingOrder!!,
                    products = products,
                    onDismiss = { editingOrder = null },
                    onConfirm = { updatedOrder ->
                        db.collection("orders").document(updatedOrder.id)
                            .set(updatedOrder)
                            .addOnSuccessListener {
                                showMessage(msgOrderUpdated)
                            }
                            .addOnFailureListener { e ->
                                showMessage(String.format(errorTemplate, e.message))
                            }
                        editingOrder = null
                    }
                )
            }

            // Settings Dialog
            if (showSettings) {
                SettingsDialog(
                    appTheme = appTheme,
                    onThemeChanged = { newTheme ->
                        appTheme = newTheme
                        sharedPreferences.edit().putString("APP_THEME", newTheme.name).apply()
                    },
                    onLanguageChanged = { code ->
                        sharedPreferences.edit().putString("APP_LANGUAGE", code).apply()
                    },
                    onTestNotificationClick = if (wantsOrderNotifications) {
                        {
                            if (NewOrderNotifier.canNotify(context)) {
                                NewOrderNotifier.show(context, 1)
                            } else {
                                showMessage(msgNotificationsBlocked)
                                NewOrderNotifier.openNotificationSettings(context)
                            }
                        }
                    } else null,
                    onLogoutClick = {
                        // The session effect sends the app back to LOGIN.
                        setSessionKey(null)
                        showSettings = false
                    },
                    onDismiss = { showSettings = false }
                )
            }

            // Update available (see AppUpdateManager.kt / public/README.md for how a
            // release gets published). Dismissible unless the published release is
            // flagged mandatory.
            availableUpdate?.let { update ->
                UpdateDialog(
                    update = update,
                    onDismiss = { availableUpdate = null }
                )
            }

            // Blocking overlay: no internet, either on launch or dropped mid-session.
            // Rendered on top without unmounting the screen underneath, so in-progress
            // form state (e.g. a half-filled order) survives the interruption.
            if (!networkStatus.isOnline) {
                NoNetworkScreen(onRetryClick = { networkStatus.refresh() })
            }
        }
        }
    }
    }
}