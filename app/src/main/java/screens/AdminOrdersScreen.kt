package com.mtb.mtb.app.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.mtb.mtb.R
import com.mtb.mtb.app.MtbSpacing
import com.mtb.mtb.app.MtbTextSizes
import com.mtb.mtb.app.MtbWebColors
import com.mtb.mtb.app.OrderRowItem
import com.mtb.mtb.app.ProductItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .background(MtbWebColors.CardWhite, RoundedCornerShape(6.dp))
            .border(1.dp, MtbWebColors.BorderGray, RoundedCornerShape(6.dp))
            .padding(horizontal = MtbSpacing.xs, vertical = 10.dp),
        textStyle = LocalTextStyle.current.copy(fontSize = MtbTextSizes.body, color = MtbWebColors.TextDark),
        keyboardOptions = keyboardOptions,
        singleLine = true,
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(placeholder, fontSize = MtbTextSizes.label, color = MtbWebColors.MutedText, maxLines = 1)
                }
                innerTextField()
            }
        }
    )
}

@Composable
fun OrderFormScreen(
    products: List<ProductItem>,
    initialRows: List<OrderRowItem> = listOf(OrderRowItem()),
    onRowsChanged: (List<OrderRowItem>) -> Unit = {},
    onSaveOrderClick: (List<OrderRowItem>, Double, Double, String, (Boolean, String) -> Unit) -> Unit,
    showMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var rows by remember(initialRows) { mutableStateOf(initialRows) }
    var isSaving by remember { mutableStateOf(false) }

    val notFoundLabel = stringResource(R.string.order_not_found)
    val validationMessage = stringResource(R.string.order_validation_required)

    // Client Selection State
    var clientExpanded by remember { mutableStateOf(false) }
    var selectedClient by remember { mutableStateOf("FILIN") }
    val clients = listOf("FILIN", "LCN")

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
        onRowsChanged(rows)
    }

    val totalQty = rows.sumOf { it.qty.toDoubleOrNull() ?: 0.0 }
    val totalWeight = rows.sumOf { it.calculatedWeight.toDoubleOrNull() ?: 0.0 }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MtbWebColors.BgPage)
            .padding(4.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MtbWebColors.CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {

                // Top Bar
                Text(
                    text = stringResource(R.string.order_form_title),
                    color = MtbWebColors.HeadingBrown,
                    fontWeight = FontWeight.Bold,
                    fontSize = MtbTextSizes.title
                )

                HorizontalDivider(color = MtbWebColors.BgBeige, thickness = 2.dp)
                Spacer(modifier = Modifier.height(6.dp))

                // Client Selection Dropdown
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            Text(text = " ▼", fontSize = MtbTextSizes.caption, color = MtbWebColors.MutedText)
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
                }

                // Vertical Portrait Table
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
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.common_ref), Modifier.weight(1.5f), color = Color.White, fontSize = MtbTextSizes.caption, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.common_article), Modifier.weight(1.9f), color = Color.White, fontSize = MtbTextSizes.caption, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.common_qty), Modifier.weight(0.8f), color = Color.White, fontSize = MtbTextSizes.caption, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.common_weight), Modifier.weight(0.9f), color = Color.White, fontSize = MtbTextSizes.caption, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(28.dp))
                    }

                    // Dynamic Item Rows
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
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
                                        .padding(end = 4.dp),
                                    placeholder = "0",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                Text(
                                    text = row.calculatedWeight.ifEmpty { "0.00" },
                                    modifier = Modifier.weight(0.9f),
                                    color = MtbWebColors.TextDark,
                                    fontSize = MtbTextSizes.body,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                IconButton(
                                    onClick = {
                                        if (rows.size > 1) {
                                            rows = rows.toMutableList().apply { removeAt(index) }
                                            onRowsChanged(rows)
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Text("×", color = MtbWebColors.DeleteRed, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Cumulative Totals Display Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MtbWebColors.BgBeige, RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.common_total), Modifier.weight(3.4f), color = MtbWebColors.HeadingBrown, fontWeight = FontWeight.Bold, fontSize = MtbTextSizes.label)
                        Text(String.format(Locale.US, "%.1f", totalQty), Modifier.weight(0.8f), color = MtbWebColors.HeadingBrown, fontWeight = FontWeight.Bold, fontSize = MtbTextSizes.label)
                        Text(String.format(Locale.US, "%.2f kg", totalWeight), Modifier.weight(0.9f), color = MtbWebColors.HeadingBrown, fontWeight = FontWeight.Bold, fontSize = MtbTextSizes.label)
                        Spacer(Modifier.width(28.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MtbButton(
                        text = stringResource(R.string.common_add_line),
                        onClick = {
                            rows = rows + OrderRowItem()
                            onRowsChanged(rows)
                        },
                        containerColor = MtbWebColors.SecondaryBrown,
                        modifier = Modifier.weight(1f),
                        height = 44.dp,
                        fontSize = MtbTextSizes.label
                    )

                    MtbButton(
                        text = if (isSaving) "..." else stringResource(R.string.common_save_order),
                        onClick = {
                            if (!isSaving) {
                                val validRows = rows.filter { it.id.isNotBlank() && it.reference != notFoundLabel }
                                if (validRows.isNotEmpty()) {
                                    isSaving = true
                                    onSaveOrderClick(validRows, totalQty, totalWeight, selectedClient) { success, message ->
                                        isSaving = false
                                        if (!success) {
                                            showMessage(message)
                                        }
                                    }
                                } else {
                                    showMessage(validationMessage)
                                }
                            }
                        },
                        containerColor = MtbWebColors.SuccessGreen,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1.2f),
                        height = 44.dp,
                        fontSize = MtbTextSizes.label
                    )

                    MtbButton(
                        text = stringResource(R.string.order_export_pdf),
                        onClick = { exportOrderToPdf(context, rows, totalQty, totalWeight, selectedClient, showMessage) },
                        containerColor = MtbWebColors.PrimaryBrown,
                        modifier = Modifier.weight(1.2f),
                        height = 44.dp,
                        fontSize = MtbTextSizes.label
                    )
                }
            }
        }
    }
}

fun exportOrderToPdf(
    context: Context,
    rows: List<OrderRowItem>,
    totalQty: Double,
    totalWeight: Double,
    clientName: String,
    showMessage: (String) -> Unit = {}
) {
    try {
        val pdfDocument = PdfDocument()
        val paint = Paint().apply { isAntiAlias = true }
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val primaryBrown = android.graphics.Color.rgb(74, 49, 35)
        val bgBeige = android.graphics.Color.rgb(238, 232, 222)
        val borderGray = android.graphics.Color.rgb(200, 200, 200)

        val notFoundLabel = context.getString(R.string.order_not_found)
        val refHeader = context.getString(R.string.common_ref)
        val articleHeader = context.getString(R.string.common_article)
        val qtyHeader = context.getString(R.string.order_pdf_qty_header)
        val weightHeader = context.getString(R.string.order_pdf_weight_header)
        val totalLabel = context.getString(R.string.common_total)

        var yPos = 40f

        try {
            val originalBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.mtb_logo)
            if (originalBitmap != null) {
                val targetWidth = 130f
                val aspectRatio = originalBitmap.height.toFloat() / originalBitmap.width.toFloat()
                val targetHeight = targetWidth * aspectRatio

                val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(
                    originalBitmap, targetWidth.toInt(), targetHeight.toInt(), true
                )

                canvas.drawBitmap(scaledBitmap, 50f, yPos, paint)

                val textStartY = yPos + (targetHeight / 2f) - 2f
                paint.color = primaryBrown
                paint.textSize = 12f
                paint.isFakeBoldText = true
                canvas.drawText("MEDITERRANEE TRANSFORMATION DE BOIS", 195f, textStartY, paint)

                paint.textSize = 10f
                paint.isFakeBoldText = false
                val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                canvas.drawText(context.getString(R.string.order_pdf_date, currentDate), 470f, textStartY, paint)

                yPos += targetHeight + 15f
            } else {
                yPos += 45f
            }
        } catch (e: Exception) {
            paint.color = primaryBrown
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText("MEDITERRANEE TRANSFORMATION DE BOIS", 50f, yPos + 15f, paint)

            val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            canvas.drawText(context.getString(R.string.order_pdf_date, currentDate), 470f, yPos + 15f, paint)
            yPos += 40f
        }

        // Print Client Name
        paint.color = primaryBrown
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText(context.getString(R.string.order_pdf_client, clientName), 50f, yPos, paint)
        yPos += 20f

        // Table Header
        paint.color = primaryBrown
        canvas.drawRect(50f, yPos, 545f, yPos + 30f, paint)

        paint.color = android.graphics.Color.WHITE
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText(refHeader, 60f, yPos + 20f, paint)
        canvas.drawText(articleHeader, 160f, yPos + 20f, paint)
        canvas.drawText(qtyHeader, 380f, yPos + 20f, paint)
        canvas.drawText(weightHeader, 460f, yPos + 20f, paint)

        yPos += 30f

        // Table Rows
        paint.color = android.graphics.Color.BLACK
        paint.isFakeBoldText = false

        for (row in rows) {
            if (row.id.isNotBlank() && row.reference != notFoundLabel) {
                paint.color = borderGray
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRect(50f, yPos, 545f, yPos + 28f, paint)
                paint.style = Paint.Style.FILL

                paint.color = android.graphics.Color.BLACK
                canvas.drawText(row.id, 60f, yPos + 18f, paint)

                val safeRef = if (row.reference.length > 32) row.reference.substring(0, 29) + "..." else row.reference
                canvas.drawText(safeRef, 160f, yPos + 18f, paint)

                val qtyText = if (row.qty.isBlank()) "0" else row.qty
                canvas.drawText(qtyText, 380f, yPos + 18f, paint)

                val weightText = if (row.calculatedWeight.isBlank()) "0.00" else row.calculatedWeight
                canvas.drawText(weightText, 460f, yPos + 18f, paint)

                yPos += 28f
            }
        }

        // Total Cumulative Weight Row
        paint.color = bgBeige
        canvas.drawRect(50f, yPos, 545f, yPos + 35f, paint)

        paint.color = primaryBrown
        paint.isFakeBoldText = true
        paint.textSize = 13f
        canvas.drawText(totalLabel, 160f, yPos + 22f, paint)
        canvas.drawText(String.format(Locale.US, "%.1f", totalQty), 380f, yPos + 22f, paint)
        canvas.drawText(String.format(Locale.US, "%.2f kg", totalWeight), 460f, yPos + 22f, paint)

        pdfDocument.finishPage(page)

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, "MTB_Commande_${System.currentTimeMillis()}.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        showMessage(context.getString(R.string.order_pdf_success))

        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(viewIntent, context.getString(R.string.order_pdf_chooser_title)))
        } catch (e: Exception) {
            // The PDF is already saved at this point — a missing PDF viewer just means we
            // can't hand off to one, not that the export itself failed.
            e.printStackTrace()
            showMessage(context.getString(R.string.order_pdf_open_error))
        }

    } catch (e: Exception) {
        e.printStackTrace()
        showMessage(context.getString(R.string.order_pdf_failure, e.localizedMessage))
    }
}