package com.mtb.mtb.app.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mtb.mtb.R
import com.mtb.mtb.app.ImageUtils
import com.mtb.mtb.app.MtbTextSizes
import com.mtb.mtb.app.MtbWebColors
import com.mtb.mtb.app.ProductItem
import com.mtb.mtb.ui.theme.NeumorphicCard

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 5f
private const val ZOOM_STEP = 0.5f

/**
 * Photo + notes popup opened from ArticlesScreen's "Voir"/"View" button. Admins can change
 * the photo and edit notes; guests get the same view read-only. The photo supports pinch-to-zoom
 * (and +/- buttons for parity with the equivalent website dialog), drag-to-pan once zoomed in,
 * and a fullscreen view with the same controls.
 */
@Composable
fun ArticleInfoDialog(
    product: ProductItem,
    canEdit: Boolean,
    onDismiss: () -> Unit,
    onSave: (photoUri: String?, notes: String) -> Unit,
    showMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val isAdmin = canEdit
    var photoBase64 by remember(product.id, product.photoUri) { mutableStateOf(product.photoUri) }
    var notes by remember(product.id, product.notes) { mutableStateOf(product.notes) }
    var scale by remember { mutableFloatStateOf(MIN_ZOOM) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isFullscreen by remember { mutableStateOf(false) }
    val photoInvalidMessage = stringResource(R.string.add_product_photo_invalid)

    fun zoomIn() {
        scale = (scale + ZOOM_STEP).coerceAtMost(MAX_ZOOM)
    }
    fun zoomOut() {
        scale = (scale - ZOOM_STEP).coerceAtLeast(MIN_ZOOM)
        if (scale == MIN_ZOOM) offset = Offset.Zero
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val base64 = ImageUtils.pickedImageToBase64(context, uri)
                if (base64 == null) {
                    showMessage(photoInvalidMessage)
                } else {
                    photoBase64 = base64
                    scale = MIN_ZOOM
                    offset = Offset.Zero
                }
            } catch (e: Exception) {
                showMessage(photoInvalidMessage)
            }
        }
    }

    val bitmap = remember(photoBase64) { decodeBase64ToBitmap(photoBase64) }

    Dialog(onDismissRequest = onDismiss) {
        AnimatedDialogSurface {
        NeumorphicCard(
            shape = RoundedCornerShape(16.dp),
            containerColor = MtbWebColors.CardWhite,
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .widthIn(max = 420.dp)
            ) {
                Text(
                    stringResource(R.string.article_info_title),
                    color = MtbWebColors.HeadingBrown,
                    fontWeight = FontWeight.Bold,
                    fontSize = MtbTextSizes.heading
                )
                Text(
                    "${product.id} — ${product.reference}",
                    color = MtbWebColors.MutedText,
                    fontSize = MtbTextSizes.caption,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MtbWebColors.HoverBg)
                        .border(1.dp, MtbWebColors.BorderGray, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = product.reference,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoomChange, _ ->
                                        val newScale = (scale * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
                                        scale = newScale
                                        offset = if (newScale > MIN_ZOOM) offset + pan else Offset.Zero
                                    }
                                }
                        )
                        Row(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PhotoToolbarButton(Icons.Filled.ZoomOut, enabled = scale > MIN_ZOOM, onClick = ::zoomOut)
                            PhotoToolbarButton(Icons.Filled.ZoomIn, enabled = scale < MAX_ZOOM, onClick = ::zoomIn)
                            PhotoToolbarButton(Icons.Filled.Fullscreen) { isFullscreen = true }
                        }
                    } else {
                        Text(
                            stringResource(R.string.article_info_no_photo),
                            color = MtbWebColors.MutedText,
                            fontSize = MtbTextSizes.label
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (isAdmin) {
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = MtbWebColors.BgBeige),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            if (photoBase64 == null) stringResource(R.string.article_info_photo_import) else stringResource(R.string.article_info_photo_change),
                            color = MtbWebColors.HeadingBrown
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Text(
                    stringResource(R.string.article_info_notes_label),
                    color = MtbWebColors.HeadingBrown,
                    fontWeight = FontWeight.Bold,
                    fontSize = MtbTextSizes.label
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    enabled = isAdmin,
                    placeholder = {
                        if (isAdmin) {
                            Text(stringResource(R.string.article_info_notes_placeholder), fontSize = MtbTextSizes.label)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MtbWebColors.TextDark,
                        unfocusedTextColor = MtbWebColors.TextDark,
                        disabledTextColor = MtbWebColors.TextDark,
                        focusedBorderColor = MtbWebColors.SecondaryBrown,
                        unfocusedBorderColor = MtbWebColors.BgBeige,
                        disabledBorderColor = MtbWebColors.BgBeige
                    )
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MtbOutlinedButton(
                        text = stringResource(R.string.common_cancel),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    if (isAdmin) {
                        MtbButton(
                            text = stringResource(R.string.article_info_save_button),
                            onClick = { onSave(photoBase64, notes) },
                            containerColor = MtbWebColors.SecondaryBrown,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        }
    }

    if (isFullscreen && bitmap != null) {
        Dialog(
            onDismissRequest = {
                offset = Offset.Zero
                isFullscreen = false
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            var fullscreenVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { fullscreenVisible = true }
            AnimatedVisibility(
                visible = fullscreenVisible,
                enter = fadeIn(animationSpec = tween(180)),
                exit = fadeOut(animationSpec = tween(100))
            ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = product.reference,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize(0.9f)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoomChange, _ ->
                                val newScale = (scale * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
                                scale = newScale
                                offset = if (newScale > MIN_ZOOM) offset + pan else Offset.Zero
                            }
                        }
                )
                Row(
                    modifier = Modifier.align(Alignment.TopEnd).padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PhotoToolbarButton(Icons.Filled.ZoomOut, enabled = scale > MIN_ZOOM, large = true, onClick = ::zoomOut)
                    PhotoToolbarButton(Icons.Filled.ZoomIn, enabled = scale < MAX_ZOOM, large = true, onClick = ::zoomIn)
                    PhotoToolbarButton(Icons.Filled.FullscreenExit, large = true) {
                        offset = Offset.Zero
                        isFullscreen = false
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun PhotoToolbarButton(
    icon: ImageVector,
    enabled: Boolean = true,
    large: Boolean = false,
    onClick: () -> Unit
) {
    val size = if (large) 40.dp else 30.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (large) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.55f))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(if (large) 22.dp else 16.dp)
        )
    }
}

private fun decodeBase64ToBitmap(base64: String?): Bitmap? {
    if (base64.isNullOrBlank()) return null
    return try {
        val clean = if (base64.contains(",")) base64.substringAfter(",") else base64
        val bytes = Base64.decode(clean, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}
