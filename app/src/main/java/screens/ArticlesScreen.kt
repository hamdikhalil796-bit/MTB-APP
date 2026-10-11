package com.mtb.mtb.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtb.mtb.R
import com.mtb.mtb.app.MtbSpacing
import com.mtb.mtb.app.MtbTextSizes
import com.mtb.mtb.app.MtbWebColors
import com.mtb.mtb.app.ProductItem
import com.mtb.mtb.ui.theme.NeumorphicCard

@Composable
fun ArticlesScreen(
    products: List<ProductItem>,
    canEdit: Boolean,
    onAddNewItemClick: () -> Unit,
    onDeleteClick: (String) -> Unit,
    onViewInfoClick: (String) -> Unit = {},
    onEditClick: (String) -> Unit,
    showMessage: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var productPendingDelete by remember { mutableStateOf<ProductItem?>(null) }

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
        NeumorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 900.dp),
            shape = RoundedCornerShape(12.dp),
            containerColor = MtbWebColors.CardWhite,
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(8.dp)) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.articles_title),
                        color = MtbWebColors.HeadingBrown,
                        fontWeight = FontWeight.Bold,
                        fontSize = MtbTextSizes.title
                    )
                    Text(
                        text = stringResource(R.string.articles_count, products.size),
                        color = MtbWebColors.HeadingBrown,
                        fontWeight = FontWeight.Bold,
                        fontSize = MtbTextSizes.label,
                        modifier = Modifier
                            .background(MtbWebColors.IdBadgeBg, RoundedCornerShape(20.dp))
                            .padding(horizontal = MtbSpacing.md, vertical = 4.dp)
                    )
                }

                HorizontalDivider(color = MtbWebColors.BgBeige, thickness = 2.dp)
                Spacer(modifier = Modifier.height(12.dp))

                MtbTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.padding(bottom = 12.dp),
                    placeholder = stringResource(R.string.articles_search_placeholder)
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
                            Text(stringResource(R.string.field_info_label), Modifier.width(42.dp), color = Color.White, fontSize = MtbTextSizes.caption, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)

                            if (canEdit) {
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
                                                onClick = { onViewInfoClick(product.id) },
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Text(stringResource(R.string.articles_view_photo), color = MtbWebColors.SecondaryBrown, fontSize = MtbTextSizes.label, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        if (canEdit) {
                                            Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                                                IconButton(
                                                    onClick = { onEditClick(product.id) },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Filled.Edit, contentDescription = null, tint = MtbWebColors.HeadingBrown, modifier = Modifier.size(18.dp))
                                                }
                                            }

                                            Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                                                IconButton(
                                                    onClick = { productPendingDelete = product },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Filled.Cancel, contentDescription = null, tint = MtbWebColors.DeleteRed, modifier = Modifier.size(20.dp))
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

                if (canEdit) {
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