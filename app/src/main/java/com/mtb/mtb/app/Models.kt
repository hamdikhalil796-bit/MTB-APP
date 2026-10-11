package com.mtb.mtb.app

// What a profile (or the guest login) may see and do. Same keys and Firestore shape as the
// website's src/lib/permissions.ts: profiles/{uid}.permissions and settings/guest.permissions
// are maps of these names to booleans. Pages first, then actions.
data class Permissions(
    val newOrder: Boolean = false,
    val currentOrder: Boolean = false,
    val history: Boolean = false,
    val articles: Boolean = false,
    val stock: Boolean = false,
    val maintenance: Boolean = false,
    val statistics: Boolean = false,
    val editOrders: Boolean = false,
    val prepareOrders: Boolean = false,
    val editArticles: Boolean = false,
    val editStock: Boolean = false,
    val editMaintenance: Boolean = false
) {
    fun toMap(): Map<String, Boolean> = mapOf(
        "newOrder" to newOrder,
        "currentOrder" to currentOrder,
        "history" to history,
        "articles" to articles,
        "stock" to stock,
        "maintenance" to maintenance,
        "statistics" to statistics,
        "editOrders" to editOrders,
        "prepareOrders" to prepareOrders,
        "editArticles" to editArticles,
        "editStock" to editStock,
        "editMaintenance" to editMaintenance
    )

    operator fun get(key: String): Boolean = toMap()[key] == true

    fun with(key: String, value: Boolean): Permissions = fromMap(toMap() + (key to value))

    companion object {
        val PAGE_KEYS = listOf("newOrder", "currentOrder", "history", "articles", "stock", "maintenance", "statistics")
        val ACTION_KEYS = listOf("editOrders", "prepareOrders", "editArticles", "editStock", "editMaintenance")

        // What a guest could see before profiles existed; used until a supervisor saves guest settings.
        val DEFAULT_GUEST = Permissions(newOrder = true, currentOrder = true, articles = true)

        // Firestore data isn't type-checked, so anything missing or malformed reads as "not allowed".
        fun fromMap(raw: Any?): Permissions {
            val map = raw as? Map<*, *> ?: return Permissions()
            fun has(key: String) = map[key] == true
            return Permissions(
                newOrder = has("newOrder"),
                currentOrder = has("currentOrder"),
                history = has("history"),
                articles = has("articles"),
                stock = has("stock"),
                maintenance = has("maintenance"),
                statistics = has("statistics"),
                editOrders = has("editOrders"),
                prepareOrders = has("prepareOrders"),
                editArticles = has("editArticles"),
                editStock = has("editStock"),
                editMaintenance = has("editMaintenance")
            )
        }
    }
}

// A login profile; the username is also its Firestore document id (profiles/{username}).
data class Profile(
    val username: String,
    val displayName: String,
    val isSupervisor: Boolean,
    val active: Boolean,
    val permissions: Permissions
)

// The signed-in person: a profile, or the anonymous guest login (profile == null).
data class Session(
    val profile: Profile?,
    val guestPermissions: Permissions
) {
    val isGuest: Boolean get() = profile == null
    val isSupervisor: Boolean get() = profile?.isSupervisor == true
    val permissions: Permissions
        get() = when {
            profile == null -> guestPermissions
            // Supervisor accounts only manage profiles: no page/action permissions at all.
            profile.isSupervisor -> Permissions()
            else -> profile.permissions
        }
}

data class ProductItem(
    val id: String = "",
    val reference: String = "",
    val weight: String = "",
    val photoUri: String? = null,
    val stock: String = "0", // NEW: current stock quantity for this article
    val notes: String = "" // Free-form notes/info about this article, edited from ArticleInfoDialog
)

data class OrderRowItem(
    val id: String = "",
    val reference: String = "",
    val qty: String = "",
    val unitWeight: Double = 0.0,
    val calculatedWeight: String = "",
    // Picking-list checkbox, only meaningful on the Current Order screen: ticked once
    // that article has been gathered. Unused (stays false) on the order-creation form.
    val checked: Boolean = false,
    // Flags this line as needing priority attention while picking, only meaningful on the
    // Current Order screen. Set/cleared from the long-press popup on the article reference.
    val urgent: Boolean = false,
    // How many units of this line have been prepared so far, only meaningful on the
    // Current Order screen. Free text (like qty) so it can be typed in partially; tracked
    // separately from [checked], which stays a manual all-or-nothing tap.
    val readyQty: String = ""
)

object OrderStatus {
    const val CURRENT = "CURRENT"
    const val COMPLETED = "COMPLETED"
}

data class SavedOrder(
    val id: String = "",
    val client: String = "",
    // Free-form order number the admin types in manually on the New Order screen; purely
    // for their own reference/tracking, unrelated to the Firestore document id above.
    val orderNumber: String = "",
    val date: String = "",
    val totalQty: Double = 0.0,
    val totalWeight: Double = 0.0,
    val items: List<OrderRowItem> = emptyList(), // Safe default prevents crash
    val timestamp: Long = 0L,
    // Defaults to COMPLETED (not CURRENT) so existing Firestore docs saved before this
    // field existed — which are all genuinely finished orders — keep showing in History
    // instead of suddenly appearing as "current". New orders set CURRENT explicitly.
    val status: String = OrderStatus.COMPLETED
)

// A logged maintenance event — a blade/tool change, a repair, or anything else that
// needed fixing on the shop's equipment. Free-form title/notes rather than a fixed
// machine list, since the set of tools isn't tracked elsewhere in the app.
data class MaintenanceEntry(
    val id: String = "",
    val title: String = "",
    val date: String = "",
    val notes: String = "",
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