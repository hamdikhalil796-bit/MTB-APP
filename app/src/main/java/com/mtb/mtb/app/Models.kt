package com.mtb.mtb.app

enum class UserRole {
    ADMIN,
    GUEST
}

data class ProductItem(
    val id: String = "",
    val reference: String = "",
    val weight: String = "",
    val photoUri: String? = null,
    val stock: String = "0" // NEW: current stock quantity for this article
)

data class OrderRowItem(
    val id: String = "",
    val reference: String = "",
    val qty: String = "",
    val unitWeight: Double = 0.0,
    val calculatedWeight: String = ""
)

data class SavedOrder(
    val id: String = "",
    val client: String = "",
    val date: String = "",
    val totalQty: Double = 0.0,
    val totalWeight: Double = 0.0,
    val items: List<OrderRowItem> = emptyList(), // Safe default prevents crash
    val timestamp: Long = 0L
)

// Parsed from the app_meta/latest_release Firestore doc — see AppUpdateManager.kt.
data class AppUpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String,
    val mandatory: Boolean
)