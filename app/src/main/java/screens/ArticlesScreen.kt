package com.mtb.mtb.app.screens

import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.mtb.mtb.R
import com.mtb.mtb.app.MtbSpacing
import com.mtb.mtb.app.MtbTextSizes
import com.mtb.mtb.app.MtbWebColors
import com.mtb.mtb.app.ProductItem
import com.mtb.mtb.app.UserRole
import java.io.File
import java.io.FileOutputStream

@Composable
fun ArticlesScreen(
    products: List<ProductItem>,
    userRole: UserRole,
    onAddNewItemClick: () -> Unit,
    onDeleteClick: (String) -> Unit,
    onViewPhotoClick: (String) -> Unit = {},
    onEditClick: (String) -> Unit,
    showMessage: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var productPendingDelete by remember { mutableStateOf<ProductItem?>(null) }
    val context = LocalContext.current

    val filteredProducts = products.filter { product ->
        product.id.contains(searchQuery, ignoreCase = true) ||
                product.reference.contains(searchQuery, ignoreCase = true)
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
                    text = stringResource(R.string.articles_title),
                    color = MtbWebColors.HeadingBrown,
                    fontWeight = FontWeight.Bold,
                    fontSize = MtbTextSizes.title,
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

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MtbWebColors.PrimaryBrown, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .padding(horizontal = 4.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.common_ref), Modifier.weight(1.6f).padding(end = MtbSpacing.xs), color = Color.White, fontSize = MtbTextSizes.caption, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.common_article), Modifier.weight(1.3f).padding(end = MtbSpacing.xs), color = Color.White, fontSize = MtbTextSizes.caption, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.common_weight), Modifier.weight(0.7f).padding(end = MtbSpacing.xs), color = Color.White, fontSize = MtbTextSizes.caption, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(stringResource(R.string.field_photo_label), Modifier.width(42.dp), color = Color.White, fontSize = MtbTextSizes.caption, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)

                            if (userRole == UserRole.ADMIN) {
                                Spacer(Modifier.width(60.dp))
                            } else {
                                Spacer(Modifier.width(8.dp))
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                        ) {
                            if (filteredProducts.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
                                        Text(stringResource(R.string.articles_empty), color = MtbWebColors.MutedText, fontSize = MtbTextSizes.label)
                                    }
                                }
                            } else {
                                itemsIndexed(filteredProducts, key = { _, product -> product.id }) { rowIndex, product ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItem()
                                            .background(if (rowIndex % 2 == 1) MtbWebColors.HoverBg else MtbWebColors.CardWhite)
                                            .padding(horizontal = MtbSpacing.xs, vertical = MtbSpacing.sm),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1.6f).padding(end = MtbSpacing.xs)) {
                                            Text(
                                                text = product.id,
                                                modifier = Modifier
                                                    .background(MtbWebColors.IdBadgeBg, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 3.dp, vertical = 2.dp),
                                                color = MtbWebColors.HeadingBrown,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = MtbTextSizes.label,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Text(
                                            text = product.reference,
                                            modifier = Modifier.weight(1.3f).padding(end = MtbSpacing.xs),
                                            fontWeight = FontWeight.Medium,
                                            color = MtbWebColors.TextDark,
                                            fontSize = MtbTextSizes.body,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Text(
                                            text = product.weight,
                                            modifier = Modifier.weight(0.7f).padding(end = MtbSpacing.xs),
                                            color = MtbWebColors.TextDark,
                                            fontSize = MtbTextSizes.body,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Box(modifier = Modifier.width(42.dp), contentAlignment = Alignment.Center) {
                                            TextButton(
                                                onClick = {
                                                    openImageInGallery(context, product.photoUri, product.id, showMessage)
                                                    onViewPhotoClick(product.id)
                                                },
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Text(stringResource(R.string.articles_view_photo), color = MtbWebColors.SecondaryBrown, fontSize = MtbTextSizes.label, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        if (userRole == UserRole.ADMIN) {
                                            Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                                                IconButton(
                                                    onClick = { onEditClick(product.id) },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Text("✎", color = MtbWebColors.HeadingBrown, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                                                IconButton(
                                                    onClick = { productPendingDelete = product },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Text("×", color = MtbWebColors.DeleteRed, fontSize = 20.sp)
                                                }
                                            }
                                        } else {
                                            Spacer(Modifier.width(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (userRole == UserRole.ADMIN) {
                    Spacer(modifier = Modifier.height(16.dp))

                    MtbButton(
                        text = stringResource(R.string.articles_add_button),
                        onClick = onAddNewItemClick,
                        containerColor = MtbWebColors.SecondaryBrown,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    productPendingDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productPendingDelete = null },
            title = { Text(stringResource(R.string.articles_delete_title), color = MtbWebColors.HeadingBrown, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResource(R.string.articles_delete_confirm, product.reference, product.id),
                    fontSize = MtbTextSizes.body
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteClick(product.id)
                    productPendingDelete = null
                }) {
                    Text(stringResource(R.string.common_delete), color = MtbWebColors.DeleteRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { productPendingDelete = null }) {
                    Text(stringResource(R.string.common_cancel), color = MtbWebColors.HeadingBrown)
                }
            },
            containerColor = MtbWebColors.CardWhite
        )
    }
}

private fun openImageInGallery(
    context: Context,
    base64String: String?,
    productId: String,
    showMessage: (String) -> Unit
) {
    if (base64String.isNullOrBlank()) {
        showMessage(context.getString(R.string.gallery_no_photo))
        return
    }

    try {
        val cleanBase64 = if (base64String.contains(",")) base64String.substringAfter(",") else base64String
        val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)

        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "image_$productId.jpg")

        val fileOutputStream = FileOutputStream(file)
        fileOutputStream.write(bytes)
        fileOutputStream.flush()
        fileOutputStream.close()

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.gallery_chooser_title)))

    } catch (e: Exception) {
        e.printStackTrace()
        showMessage(context.getString(R.string.gallery_error))
    }
}