package com.mtb.mtb.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mtb.mtb.R
import com.mtb.mtb.app.MtbSpacing
import com.mtb.mtb.app.MtbTextSizes
import com.mtb.mtb.app.MtbWebColors
import com.mtb.mtb.app.ProductItem

/**
 * Stock inventory screen.
 * - Shows NO articles by default.
 * - "+ Ajouter un article" opens a picker to add an existing product to the stock list.
 * - Each row has a number box (with +/- steppers) to set the quantity.
 * - "Enregistrer le stock" saves the quantities back (via onSaveStockClick).
 */
@Composable
fun StockScreen(
    products: List<ProductItem>,
    onSaveStockClick: (Map<String, String>, (Boolean, String) -> Unit) -> Unit
) {
    // productId -> quantity, only for rows explicitly added in this session
    var stockRows by remember { mutableStateOf(linkedMapOf<String, String>()) }
    var showPicker by remember { mutableStateOf(false) }
    var hasPrefilled by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var productIdPendingRemoval by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredStockKeys = stockRows.keys.filter { productId ->
        val product = products.find { it.id == productId }
        productId.contains(searchQuery, ignoreCase = true) ||
            (product?.reference?.contains(searchQuery, ignoreCase = true) == true)
    }

    // Pre-fill once with any articles that already have a saved stock > 0
    LaunchedEffect(products) {
        if (!hasPrefilled && products.isNotEmpty()) {
            val prefilled = linkedMapOf<String, String>()
            products.forEach { p ->
                val qty = p.stock.toIntOrNull() ?: 0
                if (qty > 0) prefilled[p.id] = p.stock
            }
            stockRows = prefilled
            hasPrefilled = true
        }
    }

    fun updateQty(productId: String, qty: String) {
        stockRows = LinkedHashMap(stockRows).apply { put(productId, qty) }
    }

    fun removeRow(productId: String) {
        stockRows = LinkedHashMap(stockRows).apply { remove(productId) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MtbWebColors.BgPage)
            .padding(4.dp),
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
            Column(modifier = Modifier.padding(8.dp)) {

                Text(
                    stringResource(R.string.stock_title),
                    color = MtbWebColors.HeadingBrown,
                    fontWeight = FontWeight.Bold,
                    fontSize = MtbTextSizes.title
                )

                HorizontalDivider(color = MtbWebColors.BgBeige, thickness = 2.dp)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    placeholder = { Text(stringResource(R.string.articles_search_placeholder), fontSize = MtbTextSizes.label) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = MtbTextSizes.body),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MtbWebColors.TextDark,
                        unfocusedTextColor = MtbWebColors.TextDark,
                        focusedBorderColor = MtbWebColors.PrimaryBrown,
                        unfocusedBorderColor = MtbWebColors.BorderGray
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MtbWebColors.PrimaryBrown, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.common_ref), Modifier.weight(1.2f), color = Color.White, fontSize = MtbTextSizes.caption, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.common_article), Modifier.weight(1.8f), color = Color.White, fontSize = MtbTextSizes.caption, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.stock_header_label),
                            Modifier.weight(1.4f),
                            color = Color.White,
                            fontSize = MtbTextSizes.caption,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.width(32.dp))
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        if (stockRows.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(30.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        stringResource(R.string.stock_empty),
                                        color = MtbWebColors.MutedText,
                                        fontSize = MtbTextSizes.label,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else if (filteredStockKeys.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(30.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        stringResource(R.string.articles_empty),
                                        color = MtbWebColors.MutedText,
                                        fontSize = MtbTextSizes.label,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(filteredStockKeys, key = { _, productId -> productId }) { rowIndex, productId ->
                                val product = products.find { it.id == productId }
                                val qty = stockRows[productId] ?: "0"

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                        .background(if (rowIndex % 2 == 1) MtbWebColors.HoverBg else MtbWebColors.CardWhite)
                                        .padding(horizontal = MtbSpacing.xs, vertical = MtbSpacing.sm),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1.2f).padding(end = MtbSpacing.xs)) {
                                        Text(
                                            text = productId,
                                            modifier = Modifier
                                                .background(MtbWebColors.IdBadgeBg, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                            color = MtbWebColors.HeadingBrown,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = MtbTextSizes.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Text(
                                        text = product?.reference ?: stringResource(R.string.stock_product_not_found),
                                        modifier = Modifier.weight(1.8f).padding(end = MtbSpacing.xs),
                                        fontWeight = FontWeight.Medium,
                                        color = if (product == null) MtbWebColors.DeleteRed else MtbWebColors.TextDark,
                                        fontSize = MtbTextSizes.body,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Number box with +/- steppers
                                    Row(
                                        modifier = Modifier.weight(1.4f),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StepperButton(
                                            symbol = "−",
                                            onClick = {
                                                val current = qty.toIntOrNull() ?: 0
                                                if (current > 0) updateQty(productId, (current - 1).toString())
                                            }
                                        )

                                        StockNumberField(
                                            value = qty,
                                            onValueChange = { newVal ->
                                                updateQty(productId, newVal.filter { it.isDigit() })
                                            },
                                            modifier = Modifier
                                                .width(42.dp)
                                                .padding(horizontal = 4.dp)
                                        )

                                        StepperButton(
                                            symbol = "+",
                                            onClick = {
                                                val current = qty.toIntOrNull() ?: 0
                                                updateQty(productId, (current + 1).toString())
                                            }
                                        )
                                    }

                                    IconButton(
                                        onClick = { productIdPendingRemoval = productId },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Text("×", color = MtbWebColors.DeleteRed, fontSize = 20.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MtbButton(
                        text = stringResource(R.string.articles_add_button),
                        onClick = { showPicker = true },
                        containerColor = MtbWebColors.SecondaryBrown,
                        modifier = Modifier.fillMaxWidth()
                    )

                    MtbButton(
                        text = if (isSaving) stringResource(R.string.common_saving) else stringResource(R.string.stock_save_button),
                        onClick = {
                            if (!isSaving) {
                                isSaving = true
                                onSaveStockClick(stockRows) { _, _ ->
                                    isSaving = false
                                }
                            }
                        },
                        containerColor = MtbWebColors.SuccessGreen,
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showPicker) {
        ProductPickerDialog(
            products = products.filter { !stockRows.containsKey(it.id) },
            onDismiss = { showPicker = false },
            onProductSelected = { product ->
                updateQty(product.id, if (product.stock.toIntOrNull() != null && product.stock != "0") product.stock else "0")
                showPicker = false
            }
        )
    }

    productIdPendingRemoval?.let { productId ->
        val product = products.find { it.id == productId }
        AlertDialog(
            onDismissRequest = { productIdPendingRemoval = null },
            title = { Text(stringResource(R.string.stock_remove_title), color = MtbWebColors.HeadingBrown, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResource(R.string.stock_remove_confirm, product?.reference ?: productId),
                    fontSize = MtbTextSizes.body
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    removeRow(productId)
                    productIdPendingRemoval = null
                }) {
                    Text(stringResource(R.string.stock_remove_action), color = MtbWebColors.DeleteRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { productIdPendingRemoval = null }) {
                    Text(stringResource(R.string.common_cancel), color = MtbWebColors.HeadingBrown)
                }
            },
            containerColor = MtbWebColors.CardWhite
        )
    }
}

@Composable
private fun StepperButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(MtbWebColors.BgBeige, CircleShape)
            .border(1.dp, MtbWebColors.BorderGray, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = MtbWebColors.HeadingBrown, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StockNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .background(MtbWebColors.CardWhite, RoundedCornerShape(6.dp))
            .border(1.dp, MtbWebColors.BorderGray, RoundedCornerShape(6.dp))
            .padding(horizontal = MtbSpacing.xs, vertical = 10.dp),
        textStyle = LocalTextStyle.current.copy(
            fontSize = MtbTextSizes.body,
            color = MtbWebColors.TextDark,
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (value.isEmpty()) {
                    Text("0", fontSize = MtbTextSizes.label, color = MtbWebColors.MutedText)
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun ProductPickerDialog(
    products: List<ProductItem>,
    onDismiss: () -> Unit,
    onProductSelected: (ProductItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = products.filter {
        it.id.contains(searchQuery, ignoreCase = true) || it.reference.contains(searchQuery, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MtbWebColors.CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .heightIn(max = 480.dp)
                    .widthIn(max = 400.dp)
            ) {
                Text(stringResource(R.string.stock_picker_title), color = MtbWebColors.HeadingBrown, fontWeight = FontWeight.Bold, fontSize = MtbTextSizes.title)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.articles_search_placeholder), fontSize = MtbTextSizes.label) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = MtbTextSizes.body)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (products.isEmpty()) stringResource(R.string.stock_picker_empty_all_added) else stringResource(R.string.articles_empty),
                            color = MtbWebColors.MutedText,
                            fontSize = MtbTextSizes.label,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(filtered, key = { it.id }) { product ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                                    .padding(vertical = 3.dp)
                                    .border(0.5.dp, MtbWebColors.BorderGray, RoundedCornerShape(4.dp))
                                    .background(MtbWebColors.HoverBg, RoundedCornerShape(4.dp))
                                    .clickable { onProductSelected(product) }
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.id, color = MtbWebColors.HeadingBrown, fontWeight = FontWeight.Bold, fontSize = MtbTextSizes.label)
                                    Text(
                                        product.reference,
                                        color = MtbWebColors.TextDark,
                                        fontSize = MtbTextSizes.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.common_cancel), color = MtbWebColors.HeadingBrown, fontSize = MtbTextSizes.body)
                }
            }
        }
    }
}
