package com.mtb.mtb.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.PopupProperties
import com.mtb.mtb.R
import com.mtb.mtb.app.MtbSpacing
import com.mtb.mtb.app.MtbTextSizes
import com.mtb.mtb.app.MtbWebColors
import com.mtb.mtb.app.OrderRowItem
import com.mtb.mtb.app.ProductItem
import com.mtb.mtb.app.SavedOrder
import com.mtb.mtb.ui.theme.NeumorphicCard
import java.util.Locale

/**
 * Real edit dialog for an existing order — lets you change the client and
 * add/remove/edit line items (id + qty, same logic as OrderFormScreen),
 * then calls onConfirm with the updated SavedOrder to persist.
 */
@Composable
fun EditOrderDialog(
    order: SavedOrder,
    products: List<ProductItem>,
    onDismiss: () -> Unit,
    onConfirm: (SavedOrder) -> Unit
) {
    var rows by remember(order.id) { mutableStateOf(order.items.ifEmpty { listOf(OrderRowItem()) }) }
    var clientExpanded by remember { mutableStateOf(false) }
    var selectedClient by remember(order.id) { mutableStateOf(order.client) }
    // Starts blank rather than pre-filled from order.orderNumber — editing an order
    // shouldn't surface whatever was typed in before as if it still applies; leaving
    // this blank and saving clears the stored number, typing a new one replaces it.
    var orderNumber by remember(order.id) { mutableStateOf("") }
    val clients = listOf("FILIN", "LCN")
    val notFoundLabel = stringResource(R.string.order_not_found)

    fun updateRow(index: Int, newId: String? = null, newQty: String? = null) {
        val currentRow = rows[index]
        val idToUse = newId ?: currentRow.id
        val qtyToUse = (newQty ?: currentRow.qty).replace(",", ".")

        val product = products.find { it.id.equals(idToUse, ignoreCase = true) }
        val unitWeight = product?.weight?.replace(",", ".")?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull() ?: 0.0
        val qtyDouble = qtyToUse.toDoubleOrNull() ?: 0.0

        val calcWeight = if (product != null && qtyDouble > 0) {
            String.format(Locale.US, "%.2f", unitWeight * qtyDouble)
        } else {
            ""
        }

        val updatedRow = currentRow.copy(
            id = product?.id ?: idToUse,
            qty = qtyToUse,
            reference = product?.reference ?: if (idToUse.isNotEmpty()) notFoundLabel else "",
            unitWeight = unitWeight,
            calculatedWeight = calcWeight
        )

        rows = rows.toMutableList().apply { set(index, updatedRow) }
    }

    val totalQty = rows.sumOf { it.qty.toDoubleOrNull() ?: 0.0 }
    val totalWeight = rows.sumOf { it.calculatedWeight.toDoubleOrNull() ?: 0.0 }

    Dialog(onDismissRequest = onDismiss) {
        AnimatedDialogSurface {
        NeumorphicCard(
            shape = RoundedCornerShape(16.dp),
            containerColor = MtbWebColors.CardWhite,
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(max = 560.dp)
                    .widthIn(max = 480.dp)
            ) {
                Text(
                    stringResource(R.string.order_edit_title),
                    color = MtbWebColors.HeadingBrown,
                    fontWeight = FontWeight.Bold,
                    fontSize = MtbTextSizes.title
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    stringResource(R.string.order_edit_number_date, order.id, order.date.substringBefore(" ")),
                    color = MtbWebColors.MutedText,
                    fontSize = MtbTextSizes.caption
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Client Selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.order_client_label), fontWeight = FontWeight.Bold, fontSize = MtbTextSizes.label, color = MtbWebColors.HeadingBrown)
                    Spacer(modifier = Modifier.width(MtbSpacing.sm))
                    Box {
                        OutlinedButton(
                            onClick = { clientExpanded = true },
                            modifier = Modifier.height(40.dp),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = MtbSpacing.md, vertical = 0.dp)
                        ) {
                            Text(text = selectedClient, color = MtbWebColors.TextDark, fontSize = MtbTextSizes.label)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MtbWebColors.MutedText)
                        }
                        DropdownMenu(
                            expanded = clientExpanded,
                            onDismissRequest = { clientExpanded = false },
                            shape = RoundedCornerShape(10.dp),
                            containerColor = MtbWebColors.CardWhite,
                            tonalElevation = 0.dp,
                            shadowElevation = 6.dp,
                            border = BorderStroke(1.dp, MtbWebColors.BorderGray)
                        ) {
                            clients.forEachIndexed { index, client ->
                                DropdownMenuItem(
                                    text = { Text(client, fontSize = MtbTextSizes.body, color = MtbWebColors.TextDark) },
                                    onClick = {
                                        selectedClient = client
                                        clientExpanded = false
                                    },
                                    contentPadding = PaddingValues(horizontal = MtbSpacing.md, vertical = MtbSpacing.xs)
                                )
                                if (index < clients.lastIndex) {
                                    HorizontalDivider(color = MtbWebColors.BgBeige, thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(MtbSpacing.md))
                    CompactTextField(
                        value = orderNumber,
                        onValueChange = { orderNumber = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        placeholder = stringResource(R.string.order_number_placeholder)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MtbWebColors.PrimaryBrown, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.common_ref), Modifier.weight(1.5f), color = Color.White, fontSize = MtbTextSizes.caption, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.common_article), Modifier.weight(1.9f), color = Color.White, fontSize = MtbTextSizes.caption, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.common_qty), Modifier.weight(0.8f), color = Color.White, fontSize = MtbTextSizes.caption, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(28.dp))
                }

                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    itemsIndexed(rows) { index, row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .background(if (index % 2 == 1) MtbWebColors.HoverBg else MtbWebColors.CardWhite)
                                .padding(horizontal = MtbSpacing.xs, vertical = MtbSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var idSuggestionsDismissed by remember { mutableStateOf(false) }
                            var idFieldWidthPx by remember { mutableStateOf(0) }
                            var idFieldHeightPx by remember { mutableStateOf(0) }
                            val density = LocalDensity.current
                            val idSuggestions = if (row.id.isBlank() || idSuggestionsDismissed) {
                                emptyList()
                            } else {
                                products.filter { p ->
                                    !p.id.equals(row.id, ignoreCase = true) &&
                                        (p.id.contains(row.id, ignoreCase = true) || p.reference.contains(row.id, ignoreCase = true))
                                }.take(5)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .padding(end = MtbSpacing.xs)
                            ) {
                                CompactTextField(
                                    value = row.id,
                                    onValueChange = {
                                        idSuggestionsDismissed = false
                                        updateRow(index, newId = it)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onSizeChanged {
                                            idFieldWidthPx = it.width
                                            idFieldHeightPx = it.height
                                        },
                                    placeholder = "ID"
                                )
                                DropdownMenu(
                                    expanded = idSuggestions.isNotEmpty(),
                                    onDismissRequest = { idSuggestionsDismissed = true },
                                    modifier = Modifier.width(with(density) { idFieldWidthPx.toDp() }),
                                    offset = DpOffset(0.dp, with(density) { idFieldHeightPx.toDp() }),
                                    properties = PopupProperties(focusable = false),
                                    shape = RoundedCornerShape(10.dp),
                                    containerColor = MtbWebColors.CardWhite,
                                    tonalElevation = 0.dp,
                                    shadowElevation = 6.dp,
                                    border = BorderStroke(1.dp, MtbWebColors.BorderGray)
                                ) {
                                    idSuggestions.forEachIndexed { suggestionIndex, product ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    product.id,
                                                    fontSize = MtbTextSizes.label,
                                                    color = MtbWebColors.TextDark,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            onClick = {
                                                idSuggestionsDismissed = true
                                                updateRow(index, newId = product.id)
                                            },
                                            contentPadding = PaddingValues(horizontal = MtbSpacing.sm, vertical = MtbSpacing.xs)
                                        )
                                        if (suggestionIndex < idSuggestions.lastIndex) {
                                            HorizontalDivider(color = MtbWebColors.BgBeige, thickness = 0.5.dp)
                                        }
                                    }
                                }
                            }

                            Text(
                                text = row.reference.ifEmpty { "-" },
                                modifier = Modifier
                                    .weight(1.9f)
                                    .padding(end = MtbSpacing.xs),
                                color = if (row.reference == notFoundLabel) MtbWebColors.DeleteRed else MtbWebColors.TextDark,
                                fontSize = MtbTextSizes.label,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            CompactTextField(
                                value = row.qty,
                                onValueChange = { updateRow(index, newQty = it) },
                                modifier = Modifier
                                    .weight(0.8f)
                                    .padding(end = MtbSpacing.xs),
                                placeholder = "0",
                                // NumberPassword (not Number) — Number triggers the predictive-text suggestion
                                // strip on Samsung keyboards, which duplicates/garbles digits mid-type (e.g. "6"
                                // becomes "600"). NumberPassword keeps the numeric keypad but has no suggestions,
                                // and stays unmasked since no PasswordVisualTransformation is applied here.
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                            )

                            IconButton(
                                onClick = {
                                    if (rows.size > 1) {
                                        rows = rows.toMutableList().apply { removeAt(index) }
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.Cancel, contentDescription = null, tint = MtbWebColors.DeleteRed, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MtbWebColors.BgBeige, RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text(stringResource(R.string.common_total), Modifier.weight(2.1f), color = MtbWebColors.HeadingBrown, fontWeight = FontWeight.Bold, fontSize = MtbTextSizes.label)
                    Text(
                        String.format(Locale.US, "%.1f", totalQty),
                        Modifier.weight(0.8f),
                        color = MtbWebColors.HeadingBrown,
                        fontWeight = FontWeight.Bold,
                        fontSize = MtbTextSizes.label
                    )
                    Text(
                        String.format(Locale.US, "%.2f kg", totalWeight),
                        color = MtbWebColors.HeadingBrown,
                        fontWeight = FontWeight.Bold,
                        fontSize = MtbTextSizes.label
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                MtbButton(
                    text = stringResource(R.string.common_add_line),
                    onClick = { rows = rows + OrderRowItem() },
                    containerColor = MtbWebColors.SecondaryBrown,
                    modifier = Modifier.fillMaxWidth(),
                    height = 44.dp,
                    fontSize = MtbTextSizes.label
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MtbOutlinedButton(
                        text = stringResource(R.string.common_cancel),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )

                    MtbButton(
                        text = stringResource(R.string.common_save_order),
                        onClick = {
                            val validRows = rows.filter { it.id.isNotBlank() && it.reference != notFoundLabel }
                            if (validRows.isNotEmpty()) {
                                onConfirm(
                                    order.copy(
                                        client = selectedClient,
                                        orderNumber = orderNumber,
                                        items = validRows,
                                        totalQty = validRows.sumOf { it.qty.toDoubleOrNull() ?: 0.0 },
                                        totalWeight = validRows.sumOf { it.calculatedWeight.toDoubleOrNull() ?: 0.0 }
                                    )
                                )
                            }
                        },
                        containerColor = MtbWebColors.SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        }
    }
}
