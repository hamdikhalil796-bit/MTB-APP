package com.mtb.mtb.app.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtb.mtb.R
import com.mtb.mtb.app.MtbTextSizes
import com.mtb.mtb.app.MtbWebColors
import com.mtb.mtb.app.ProductItem
import com.mtb.mtb.app.SavedOrder
import com.mtb.mtb.ui.theme.NeumorphicCard

@Composable
fun OrdersHistoryScreen(
    orders: List<SavedOrder>,
    canEditOrders: Boolean,
    onDeleteOrderClick: (String) -> Unit,
    onEditOrderClick: (SavedOrder) -> Unit,
    showMessage: (String) -> Unit = {}
) {
    OrderListScaffold(
        title = if (canEditOrders) {
            stringResource(R.string.history_title_admin)
        } else {
            stringResource(R.string.history_title_readonly)
        },
        emptyMessage = stringResource(R.string.history_empty),
        orders = orders,
        canEditOrders = canEditOrders,
        onDeleteOrderClick = onDeleteOrderClick,
        onEditOrderClick = onEditOrderClick,
        onFinishOrderClick = null,
        onToggleItemChecked = null,
        onToggleItemUrgent = null,
        onUpdateReadyQty = null,
        onViewArticleInfoClick = null,
        showMessage = showMessage
    )
}

/**
 * Shared shell (title, search, card list, empty state) behind both OrdersHistoryScreen
 * and CurrentOrderScreen — they only differ in title/empty copy and whether a "finish"
 * action is offered on each card (null hides it, matching History's read-only nature).
 */
@Composable
internal fun OrderListScaffold(
    title: String,
    emptyMessage: String,
    orders: List<SavedOrder>,
    canEditOrders: Boolean,
    onDeleteOrderClick: (String) -> Unit,
    onEditOrderClick: (SavedOrder) -> Unit,
    onFinishOrderClick: ((SavedOrder) -> Unit)?,
    onToggleItemChecked: ((SavedOrder, Int, Boolean) -> Unit)?,
    onToggleItemUrgent: ((SavedOrder, Int, Boolean) -> Unit)?,
    onUpdateReadyQty: ((SavedOrder, Int, String) -> Unit)?,
    onViewArticleInfoClick: ((String) -> Unit)?,
    showMessage: (String) -> Unit = {},
    products: List<ProductItem> = emptyList()
) {
    var searchQuery by remember { mutableStateOf("") }
    // Rebuilt whenever the live products snapshot changes, so a Stock page edit is reflected here.
    val productsById = remember(products) { products.associateBy { it.id } }

    val filteredOrders = orders.filter { order ->
        order.id.contains(searchQuery, ignoreCase = true) ||
                order.client.contains(searchQuery, ignoreCase = true) ||
                order.date.contains(searchQuery, ignoreCase = true)
    }.sortedByDescending { it.timestamp }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MtbWebColors.BgPage)
            .padding(8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        NeumorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 900.dp),
            shape = RoundedCornerShape(12.dp),
            containerColor = MtbWebColors.CardWhite,
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Text(
                    text = title,
                    color = MtbWebColors.HeadingBrown,
                    fontSize = MtbTextSizes.heading,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                HorizontalDivider(color = MtbWebColors.BgBeige, thickness = 2.dp)
                Spacer(modifier = Modifier.height(12.dp))

                MtbTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.padding(bottom = 12.dp),
                    placeholder = stringResource(R.string.history_search_placeholder)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (filteredOrders.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                emptyMessage,
                                color = MtbWebColors.MutedText,
                                fontSize = MtbTextSizes.body
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredOrders, key = { it.id }) { order ->
                                OrderHistoryCard(
                                    order = order,
                                    canEditOrders = canEditOrders,
                                    onEdit = { onEditOrderClick(order) },
                                    onDelete = { onDeleteOrderClick(order.id) },
                                    onFinish = onFinishOrderClick?.let { callback -> { callback(order) } },
                                    onToggleItemChecked = onToggleItemChecked?.let { callback ->
                                        { itemIndex, checked -> callback(order, itemIndex, checked) }
                                    },
                                    onToggleItemUrgent = onToggleItemUrgent?.let { callback ->
                                        { itemIndex, urgent -> callback(order, itemIndex, urgent) }
                                    },
                                    onUpdateReadyQty = onUpdateReadyQty?.let { callback ->
                                        { itemIndex, readyQty -> callback(order, itemIndex, readyQty) }
                                    },
                                    onViewArticleInfoClick = onViewArticleInfoClick,
                                    showMessage = showMessage,
                                    productsById = productsById,
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OrderHistoryCard(
    order: SavedOrder,
    canEditOrders: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onFinish: (() -> Unit)? = null,
    onToggleItemChecked: ((Int, Boolean) -> Unit)? = null,
    onToggleItemUrgent: ((Int, Boolean) -> Unit)? = null,
    onUpdateReadyQty: ((Int, String) -> Unit)? = null,
    onViewArticleInfoClick: ((String) -> Unit)? = null,
    showMessage: (String) -> Unit = {},
    productsById: Map<String, ProductItem> = emptyMap(),
    modifier: Modifier = Modifier
) {
    // Current Order cards start expanded since the item checkboxes are the whole point;
    // History cards stay collapsed since their details are secondary/reference-only.
    var expanded by remember { mutableStateOf(onToggleItemChecked != null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    // Index of the item whose long-press "Urgent / Voir" popup is open, or null if none.
    var actionsForItemIndex by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    NeumorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .border(0.5.dp, MtbWebColors.BorderGray, RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(8.dp),
        containerColor = MtbWebColors.HoverBg
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.history_client_label, order.client),
                    fontWeight = FontWeight.Bold,
                    color = MtbWebColors.HeadingBrown,
                    fontSize = MtbTextSizes.subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (order.orderNumber.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.history_order_number_label, order.orderNumber),
                        color = MtbWebColors.MutedText,
                        fontSize = MtbTextSizes.caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = stringResource(R.string.history_date_label, order.date.substringBefore(" ")),
                color = MtbWebColors.MutedText,
                fontSize = MtbTextSizes.caption,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.history_summary, order.totalQty, order.totalWeight),
                    fontWeight = FontWeight.SemiBold,
                    color = MtbWebColors.TextDark,
                    fontSize = MtbTextSizes.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )

                IconButton(
                    onClick = {
                        exportOrderToPdf(
                            context, order.items, order.totalQty, order.totalWeight, order.client, showMessage,
                            showChecklist = onToggleItemChecked != null,
                            orderNumber = order.orderNumber
                        )
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Filled.Description, contentDescription = null, tint = MtbWebColors.HeadingBrown, modifier = Modifier.size(18.dp))
                }

                if (canEditOrders) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = MtbWebColors.HeadingBrown, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Filled.Cancel, contentDescription = null, tint = MtbWebColors.DeleteRed, modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (onFinish != null) {
                val allItemsChecked = order.items.isNotEmpty() && order.items.all { it.checked }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        R.string.current_order_progress,
                        order.items.count { it.checked },
                        order.items.size
                    ),
                    fontSize = MtbTextSizes.caption,
                    color = MtbWebColors.MutedText
                )
                Spacer(modifier = Modifier.height(4.dp))
                MtbButton(
                    text = stringResource(R.string.current_order_finish_button),
                    onClick = onFinish,
                    containerColor = MtbWebColors.SuccessGreen,
                    enabled = allItemsChecked,
                    modifier = Modifier.fillMaxWidth(),
                    height = 40.dp,
                    fontSize = MtbTextSizes.label
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    HorizontalDivider(color = MtbWebColors.BgBeige, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.history_details_label),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = MtbTextSizes.label,
                        color = MtbWebColors.SecondaryBrown
                    )

                    order.items.forEachIndexed { itemIndex, item ->
                        // Buffered locally so typing multiple digits doesn't stutter waiting on
                        // the Firestore round-trip; re-seeded only when this row identity changes.
                        var readyQtyInput by remember(order.id, itemIndex) { mutableStateOf(item.readyQty) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (onToggleItemChecked != null) {
                                    Checkbox(
                                        checked = item.checked,
                                        onCheckedChange = { checked -> onToggleItemChecked(itemIndex, checked) },
                                        colors = CheckboxDefaults.colors(checkedColor = MtbWebColors.SuccessGreen),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = item.id,
                                    modifier = Modifier
                                        .background(MtbWebColors.IdBadgeBg, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                    color = MtbWebColors.HeadingBrown,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = MtbTextSizes.label,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.reference,
                                    fontSize = MtbTextSizes.body,
                                    fontWeight = if (item.urgent) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        item.urgent -> MtbWebColors.DeleteRed
                                        item.checked -> MtbWebColors.MutedText
                                        else -> MtbWebColors.TextDark
                                    },
                                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .let { base ->
                                            if (onToggleItemUrgent != null || onViewArticleInfoClick != null) {
                                                base.combinedClickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = LocalIndication.current,
                                                    onClick = {},
                                                    onLongClick = { actionsForItemIndex = itemIndex }
                                                )
                                            } else {
                                                base
                                            }
                                        }
                                )
                            }
                            if (onUpdateReadyQty != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.qty,
                                        fontSize = MtbTextSizes.label,
                                        fontWeight = FontWeight.Bold,
                                        color = MtbWebColors.HeadingBrown
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    CompactTextField(
                                        value = readyQtyInput,
                                        onValueChange = {
                                            readyQtyInput = it
                                            onUpdateReadyQty(itemIndex, it)
                                        },
                                        modifier = Modifier.widthIn(min = 34.dp, max = 56.dp),
                                        // Shows this article's current stock (live from the Stock page's data) as
                                        // the hint while the box is empty; typing a value replaces it.
                                        placeholder = productsById[item.id]?.stock?.trim()?.toIntOrNull()?.toString() ?: "0",
                                        // NumberPassword (not Number) — Number triggers the predictive-text suggestion
                                        // strip on Samsung keyboards, which duplicates/garbles digits mid-type (e.g. "6"
                                        // becomes "600"). NumberPassword keeps the numeric keypad but has no suggestions,
                                        // and stays unmasked since no PasswordVisualTransformation is applied here.
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        verticalPadding = 4.dp
                                    )
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.history_qty_label, item.qty),
                                    fontSize = MtbTextSizes.label,
                                    fontWeight = FontWeight.Bold,
                                    color = MtbWebColors.HeadingBrown
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.history_delete_title), color = MtbWebColors.HeadingBrown, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResource(R.string.history_delete_confirm, order.client, order.date.substringBefore(" ")),
                    fontSize = MtbTextSizes.body
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text(stringResource(R.string.common_delete), color = MtbWebColors.DeleteRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.common_cancel), color = MtbWebColors.HeadingBrown)
                }
            },
            containerColor = MtbWebColors.CardWhite
        )
    }

    val urgentItemIndex = actionsForItemIndex
    if (urgentItemIndex != null && urgentItemIndex in order.items.indices) {
        val targetItem = order.items[urgentItemIndex]
        AlertDialog(
            onDismissRequest = { actionsForItemIndex = null },
            title = {
                Text(
                    targetItem.reference,
                    color = MtbWebColors.HeadingBrown,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column {
                    if (onToggleItemUrgent != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onToggleItemUrgent(urgentItemIndex, !targetItem.urgent)
                                    actionsForItemIndex = null
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.PriorityHigh,
                                contentDescription = null,
                                tint = MtbWebColors.DeleteRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                stringResource(
                                    if (targetItem.urgent) R.string.current_order_unmark_urgent else R.string.current_order_mark_urgent
                                ),
                                fontSize = MtbTextSizes.body,
                                color = MtbWebColors.TextDark
                            )
                        }
                    }
                    if (onViewArticleInfoClick != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onViewArticleInfoClick(targetItem.id)
                                    actionsForItemIndex = null
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Visibility,
                                contentDescription = null,
                                tint = MtbWebColors.HeadingBrown,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.articles_view_photo),
                                fontSize = MtbTextSizes.body,
                                color = MtbWebColors.TextDark
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { actionsForItemIndex = null }) {
                    Text(stringResource(R.string.common_cancel), color = MtbWebColors.HeadingBrown)
                }
            },
            containerColor = MtbWebColors.CardWhite
        )
    }
}