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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mtb.mtb.R
import com.mtb.mtb.app.MtbSpacing
import com.mtb.mtb.app.MtbTextSizes
import com.mtb.mtb.app.MtbWebColors
import com.mtb.mtb.app.ProductItem
import com.mtb.mtb.ui.theme.NeumorphicCard

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
    onSaveStockClick: (Map<String, String>, (Boolean, String) -> Unit) -> Unit,
    onRemoveClick: (String) -> Unit = {},
    // Without "change stock" the page is view-only: quantities as plain numbers, no add/remove/save.
    canEdit: Boolean = true
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

    // Sized like the website's auto-width table column: as wide as the longest ID in the list
    // (capped so ARTICLE keeps room; longer IDs wrap), so IDs show in full instead of "…".
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val idTextStyle = LocalTextStyle.current.merge(
        TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = MtbTextSizes.label)
    )
    val refColumnWidth = remember(stockRows.keys.toList(), idTextStyle, density) {
        val widestIdPx = stockRows.keys.maxOfOrNull { textMeasurer.measure(it, idTextStyle).size.width } ?: 0
        // + the badge's 4dp side padding on each side, the 4dp gap before ARTICLE, 2dp slack
        (with(density) { widestIdPx.toDp() } + 14.dp).coerceIn(REF_COLUMN_MIN_WIDTH, REF_COLUMN_MAX_WIDTH)
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
        NeumorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 900.dp),
            shape = RoundedCornerShape(12.dp),
            containerColor = MtbWebColors.CardWhite,
            elevation = 2.dp
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

                MtbTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.padding(bottom = 12.dp),
                    placeholder = stringResource(R.string.articles_search_placeholder)
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
                        Text(stringResource(R.string.common_ref), Modifier.width(refColumnWidth), color = Color.White, fontSize = MtbTextSizes.caption, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.common_article), Modifier.weight(1f), color = Color.White, fontSize = MtbTextSizes.caption, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.stock_header_label),
                            Modifier.width(STOCK_STEPPER_COLUMN_WIDTH),
                            color = Color.White,
                            fontSize = MtbTextSizes.caption,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.width(36.dp))
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
                                    Box(modifier = Modifier.width(refColumnWidth).padding(end = MtbSpacing.xs)) {
                                        Text(
                                            text = productId,
                                            modifier = Modifier
                                                .background(MtbWebColors.IdBadgeBg, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                            color = MtbWebColors.HeadingBrown,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = MtbTextSizes.label
                                        )
                                    }

                                    Text(
                                        text = product?.reference ?: stringResource(R.string.stock_product_not_found),
                                        modifier = Modifier.weight(1f).padding(end = MtbSpacing.xs),
                                        fontWeight = FontWeight.Medium,
                                        color = if (product == null) MtbWebColors.DeleteRed else MtbWebColors.TextDark,
                                        fontSize = MtbTextSizes.body
                                    )

                                    // Number box with +/- steppers — fixed width (not weighted) so it
                                    // never gets squeezed below its 106dp content minimum by the
                                    // Ref/Article text columns on narrow phone screens.
                                    Row(
                                        modifier = Modifier.width(STOCK_STEPPER_COLUMN_WIDTH),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!canEdit) {
                                            Text(
                                                text = qty.ifBlank { "0" },
                                                fontWeight = FontWeight.Bold,
                                                color = MtbWebColors.HeadingBrown,
                                                fontSize = MtbTextSizes.body
                                            )
                                        } else {
                                        StepperButton(
                                            icon = Icons.Filled.Remove,
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
                                                .width(56.dp)
                                                .padding(horizontal = 4.dp)
                                        )

                                        StepperButton(
                                            icon = Icons.Filled.Add,
                                            onClick = {
                                                val current = qty.toIntOrNull() ?: 0
                                                updateQty(productId, (current + 1).toString())
                                            }
                                        )
                                        }
                                    }

                                    if (canEdit) {
                                        IconButton(
                                            onClick = { productIdPendingRemoval = productId },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Filled.Cancel, contentDescription = null, tint = MtbWebColors.DeleteRed, modifier = Modifier.size(20.dp))
                                        }
                                    } else {
                                        Spacer(Modifier.width(36.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                if (canEdit) {
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
                    // Also clears the product's stock in Firestore — otherwise the prefill
                    // (which pulls in every product with stock > 0) brings this row right
                    // back the next time the screen loads, since nothing zeroed it there.
                    removeRow(productId)
                    onRemoveClick(productId)
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

// Two 28dp stepper buttons + an enlarged 56dp qty field (with 4dp side padding each).
private val STOCK_STEPPER_COLUMN_WIDTH = 128.dp
private val REF_COLUMN_MIN_WIDTH = 44.dp
private val REF_COLUMN_MAX_WIDTH = 120.dp

@Composable
private fun StepperButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(MtbWebColors.BgBeige, CircleShape)
            .border(1.dp, MtbWebColors.BorderGray, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = MtbWebColors.HeadingBrown, modifier = Modifier.size(14.dp))
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
            .padding(horizontal = MtbSpacing.xs, vertical = 6.dp),
        // lineHeight reset to Unspecified — see CompactTextField in AdminOrdersScreen.kt for
        // why: otherwise it inherits the app's 24sp base line height regardless of fontSize.
        textStyle = LocalTextStyle.current.copy(
            fontSize = MtbTextSizes.body,
            lineHeight = TextUnit.Unspecified,
            color = MtbWebColors.TextDark,
            textAlign = TextAlign.Center
        ),
        // NumberPassword (not Number) — Number triggers the predictive-text suggestion
        // strip on Samsung keyboards, which duplicates/garbles digits mid-type (e.g. "6"
        // becomes "600"). NumberPassword keeps the numeric keypad but has no suggestions,
        // and stays unmasked since no PasswordVisualTransformation is applied here.
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
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
        AnimatedDialogSurface {
        NeumorphicCard(
            shape = RoundedCornerShape(16.dp),
            containerColor = MtbWebColors.CardWhite,
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .heightIn(max = 480.dp)
                    .widthIn(max = 400.dp)
            ) {
                Text(stringResource(R.string.stock_picker_title), color = MtbWebColors.HeadingBrown, fontWeight = FontWeight.Bold, fontSize = MtbTextSizes.title)
                Spacer(modifier = Modifier.height(8.dp))

                MtbTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = stringResource(R.string.articles_search_placeholder)
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
                                        fontSize = MtbTextSizes.label
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
}
