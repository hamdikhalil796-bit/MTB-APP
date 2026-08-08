package com.mtb.mtb.app.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtb.mtb.R
import com.mtb.mtb.app.MtbTextSizes
import com.mtb.mtb.app.MtbWebColors
import com.mtb.mtb.app.SavedOrder
import com.mtb.mtb.app.UserRole

@Composable
fun OrdersHistoryScreen(
    orders: List<SavedOrder>,
    userRole: UserRole,
    onDeleteOrderClick: (String) -> Unit,
    onEditOrderClick: (SavedOrder) -> Unit,
    showMessage: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 900.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MtbWebColors.CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Text(
                    text = if (userRole == UserRole.ADMIN) {
                        stringResource(R.string.history_title_admin)
                    } else {
                        stringResource(R.string.history_title_readonly)
                    },
                    color = MtbWebColors.HeadingBrown,
                    fontSize = MtbTextSizes.heading,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                HorizontalDivider(color = MtbWebColors.BgBeige, thickness = 2.dp)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    placeholder = { Text(stringResource(R.string.history_search_placeholder), fontSize = MtbTextSizes.label) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = MtbTextSizes.body),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MtbWebColors.TextDark,
                        unfocusedTextColor = MtbWebColors.TextDark,
                        focusedBorderColor = MtbWebColors.PrimaryBrown,
                        unfocusedBorderColor = MtbWebColors.BorderGray
                    )
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
                                stringResource(R.string.history_empty),
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
                                    userRole = userRole,
                                    onEdit = { onEditOrderClick(order) },
                                    onDelete = { onDeleteOrderClick(order.id) },
                                    showMessage = showMessage,
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

@Composable
private fun OrderHistoryCard(
    order: SavedOrder,
    userRole: UserRole,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    showMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(0.5.dp, MtbWebColors.BorderGray, RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MtbWebColors.HoverBg)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.history_client_label, order.client),
                fontWeight = FontWeight.Bold,
                color = MtbWebColors.HeadingBrown,
                fontSize = MtbTextSizes.subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                        exportOrderToPdf(context, order.items, order.totalQty, order.totalWeight, order.client, showMessage)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Text(
                        text = "📄",
                        fontSize = 16.sp
                    )
                }

                if (userRole == UserRole.ADMIN) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text(
                            text = "✎",
                            color = MtbWebColors.HeadingBrown,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text(
                            text = "×",
                            color = MtbWebColors.DeleteRed,
                            fontSize = 20.sp
                        )
                    }
                }
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

                    order.items.forEach { item ->
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
                                    color = MtbWebColors.TextDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
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
}