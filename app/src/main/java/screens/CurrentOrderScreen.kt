package com.mtb.mtb.app.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mtb.mtb.R
import com.mtb.mtb.app.ProductItem
import com.mtb.mtb.app.SavedOrder
import com.mtb.mtb.app.Permissions

/**
 * Orders that have been saved but not yet marked finished — the landing screen right
 * after saving a new order. Each article in a card has a checkbox for the admin to tick
 * off as it's picked; "Terminer la commande" only unlocks once every item is checked,
 * then moves the order into OrdersHistoryScreen. Shares its list UI (search, cards, empty
 * state) via OrderListScaffold, just with a different title/empty message and both the
 * finish and checkbox actions wired.
 */
@Composable
fun CurrentOrderScreen(
    orders: List<SavedOrder>,
    products: List<ProductItem>,
    permissions: Permissions,
    onDeleteOrderClick: (String) -> Unit,
    onEditOrderClick: (SavedOrder) -> Unit,
    onFinishOrderClick: (SavedOrder) -> Unit,
    onToggleItemChecked: (SavedOrder, Int, Boolean) -> Unit,
    onToggleItemUrgent: (SavedOrder, Int, Boolean) -> Unit,
    onUpdateReadyQty: (SavedOrder, Int, String) -> Unit,
    onViewArticleInfoClick: (String) -> Unit,
    showMessage: (String) -> Unit = {}
) {
    OrderListScaffold(
        title = stringResource(R.string.current_order_title),
        emptyMessage = stringResource(R.string.current_order_empty),
        orders = orders,
        products = products,
        canEditOrders = permissions.editOrders,
        onDeleteOrderClick = onDeleteOrderClick,
        onEditOrderClick = onEditOrderClick,
        // Without "prepare orders" the list is read-only: no checkboxes, urgent flag, ready qty or finish.
        onFinishOrderClick = onFinishOrderClick.takeIf { permissions.prepareOrders },
        onToggleItemChecked = onToggleItemChecked.takeIf { permissions.prepareOrders },
        onToggleItemUrgent = onToggleItemUrgent.takeIf { permissions.prepareOrders },
        onUpdateReadyQty = onUpdateReadyQty.takeIf { permissions.prepareOrders },
        onViewArticleInfoClick = onViewArticleInfoClick,
        showMessage = showMessage
    )
}
