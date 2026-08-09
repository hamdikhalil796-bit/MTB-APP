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
import com.google.firebase.messaging.FirebaseMessaging
import com.mtb.mtb.R
import com.mtb.mtb.app.screens.*
import com.mtb.mtb.ui.theme.MTBTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val msgProductDeleted = stringResource(R.string.msg_product_deleted)
    val msgProductDuplicateId = stringResource(R.string.msg_product_duplicate_id)
    val msgProductAdded = stringResource(R.string.msg_product_added)
    val msgProductUpdated = stringResource(R.string.msg_product_updated)
    val msgStockNothingToSave = stringResource(R.string.msg_stock_nothing_to_save)
    val msgStockSavedWithErrors = stringResource(R.string.msg_stock_saved_with_errors)
    val msgStockUpdated = stringResource(R.string.msg_stock_updated)

    // 1. Initialize SharedPreferences
    val sharedPreferences = context.getSharedPreferences("MTB_PREFS", Context.MODE_PRIVATE)

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

    // Manual dark/light override — defaults to the system theme the first time, then persists.
    val systemDark = isSystemInDarkTheme()
    var isDarkMode by remember { mutableStateOf(sharedPreferences.getBoolean("DARK_MODE", systemDark)) }
    var showSettings by remember { mutableStateOf(false) }

    // 2. Read saved role on startup
    val savedRoleName = sharedPreferences.getString("USER_ROLE", null)

    // 3. Determine initial state based on saved data
    val initialScreen = if (savedRoleName != null) "HOME" else "LOGIN"
    val initialRole = when (savedRoleName) {
        UserRole.ADMIN.name -> UserRole.ADMIN
        UserRole.GUEST.name -> UserRole.GUEST
        else -> UserRole.GUEST
    }

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

    var currentUserRole by remember { mutableStateOf(initialRole) }

    // Push notifications for new orders: only admins are subscribed to the topic the
    // "notifyAdminsOnNewOrder" Cloud Function publishes to (see OrderNotificationService).
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op: if denied, the admin simply won't see the system notification */ }

    LaunchedEffect(currentUserRole) {
        if (currentUserRole == UserRole.ADMIN) {
            FirebaseMessaging.getInstance().subscribeToTopic(NEW_ORDER_TOPIC)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(NEW_ORDER_TOPIC)
        }
    }

    var products by remember { mutableStateOf(listOf<ProductItem>()) }
    var orders by remember { mutableStateOf(listOf<SavedOrder>()) }

    var editingProductId by remember { mutableStateOf<String?>(null) }
    val productToEdit = products.find { it.id == editingProductId }

    // New state to handle order editing safely
    var editingOrder by remember { mutableStateOf<SavedOrder?>(null) }

    var availableUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
    LaunchedEffect(Unit) {
        AppUpdateManager.checkForUpdate(db, AppUpdateManager.installedVersionCode(context)) { update ->
            availableUpdate = update
        }
    }

    LaunchedEffect(Unit) {
        // Fetch products from Firebase in real-time
        db.collection("products").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            products = snapshot.documents.mapNotNull { document ->
                document.toObject(ProductItem::class.java)
            }
        }

        // Fetch orders from Firebase in real-time
        db.collection("orders").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            orders = snapshot.documents.mapNotNull { doc ->
                val order = doc.toObject(SavedOrder::class.java)
                // Fallback: If order.id is empty in Firestore, use the actual document ID to prevent crash
                order?.copy(id = if (order.id.isNotBlank()) order.id else doc.id)
            }
        }
    }

    MTBTheme(darkTheme = isDarkMode) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (currentScreen != "LOGIN" && currentScreen != "SPLASH") {
                    MtbBottomNavigationBar(
                        currentScreen = currentScreen,
                        userRole = currentUserRole,
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
                    SplashScreen(onFinished = { screenStack = listOf(initialScreen) })
                }

                "LOGIN" -> {
                    LoginScreen(
                        onLoginSuccess = { role ->
                            sharedPreferences.edit().putString("USER_ROLE", role.name).apply()
                            currentUserRole = role
                            screenStack = listOf("HOME")
                        }
                    )
                }

                "HOME" -> {
                    HomeScreen(
                        userRole = currentUserRole,
                        onNewOrderClick = { navigateTo("NEW_ORDER") },
                        onViewHistoryClick = { navigateTo("HISTORY") },
                        onViewArticlesClick = { navigateTo("ARTICLES") },
                        onAddProductClick = { navigateTo("ADD_PRODUCT") },
                        onViewStockClick = { navigateTo("STOCK") },
                        onSettingsClick = { showSettings = true }
                    )
                }

                "NEW_ORDER" -> {
                    OrderFormScreen(
                        products = products,
                        onSaveOrderClick = { rows, totalQty, totalWeight, client, onComplete ->
                            val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                            val orderId = "CMD_${System.currentTimeMillis()}"
                            val newOrder = SavedOrder(
                                id = orderId,
                                client = client,
                                date = currentDate,
                                totalQty = totalQty,
                                totalWeight = totalWeight,
                                items = rows,
                                timestamp = System.currentTimeMillis()
                            )

                            db.collection("orders").document(orderId)
                                .set(newOrder)
                                .addOnSuccessListener {
                                    showMessage(msgOrderSaved)
                                    replaceTop("HISTORY")
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
                        orders = orders,
                        userRole = currentUserRole,
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

                "ARTICLES" -> {
                    ArticlesScreen(
                        products = products,
                        userRole = currentUserRole,
                        onAddNewItemClick = { navigateTo("ADD_PRODUCT") },
                        onDeleteClick = { productId ->
                            if (productId.isNotBlank()) {
                                db.collection("products").document(productId).delete()
                                    .addOnSuccessListener {
                                        showMessage(msgProductDeleted)
                                    }
                            }
                        },
                        onViewPhotoClick = { productId -> },
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
                        }
                    )
                }
            }
            }

            // Edit Product Dialog
            if (productToEdit != null) {
                EditProductDialog(
                    product = productToEdit,
                    onDismiss = { editingProductId = null },
                    onConfirm = { newReference, newWeight, newPhotoUri ->
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
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = {
                        isDarkMode = !isDarkMode
                        sharedPreferences.edit().putBoolean("DARK_MODE", isDarkMode).apply()
                    },
                    onLanguageChanged = { code ->
                        sharedPreferences.edit().putString("APP_LANGUAGE", code).apply()
                    },
                    onLogoutClick = {
                        sharedPreferences.edit().remove("USER_ROLE").apply()
                        currentUserRole = UserRole.GUEST
                        screenStack = listOf("LOGIN")
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