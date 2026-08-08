package com.mtb.mtb.app.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mtb.mtb.R
import com.mtb.mtb.app.ImageUtils
import com.mtb.mtb.app.MtbTextSizes
import com.mtb.mtb.app.MtbWebColors
import com.mtb.mtb.app.ProductItem

@Composable
fun EditProductDialog(
    product: ProductItem,
    onDismiss: () -> Unit,
    onConfirm: (reference: String, weight: String, photoUri: String?) -> Unit,
    showMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current

    // L'ID ne doit pas être modifiable car c'est la clé du document Firebase
    var reference by remember { mutableStateOf(product.reference) }
    var weight by remember { mutableStateOf(product.weight.replace("-", "")) }
    var imageBase64String by remember { mutableStateOf<String?>(product.photoUri) }
    val decodeErrorMessage = stringResource(R.string.edit_product_photo_decode_error)

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val base64 = ImageUtils.pickedImageToBase64(context, uri)
                if (base64 == null) {
                    showMessage(decodeErrorMessage)
                } else {
                    imageBase64String = base64
                }
            } catch (e: Exception) {
                showMessage(decodeErrorMessage)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Permet de mieux contrôler la largeur
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f) // Prend 90% de la largeur de l'écran (idéal pour le mode portrait)
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MtbWebColors.CardWhite)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.edit_product_title),
                    color = MtbWebColors.HeadingBrown,
                    fontSize = MtbTextSizes.heading,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider(color = MtbWebColors.BgBeige, thickness = 2.dp)

                val textFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MtbWebColors.HeadingBrown,
                    unfocusedTextColor = MtbWebColors.HeadingBrown,
                    focusedBorderColor = MtbWebColors.SecondaryBrown,
                    unfocusedBorderColor = MtbWebColors.BgBeige
                )

                // ID (Lecture seule)
                Column {
                    Text(stringResource(R.string.field_id_label), color = MtbWebColors.MutedText, fontSize = MtbTextSizes.label, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = product.id,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = textFieldColors
                    )
                }

                // Référence
                Column {
                    Text(stringResource(R.string.field_description_label), color = MtbWebColors.HeadingBrown, fontSize = MtbTextSizes.label, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = reference,
                        onValueChange = { reference = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = textFieldColors
                    )
                }

                // Poids
                Column {
                    Text(stringResource(R.string.edit_product_weight_kg_label), color = MtbWebColors.HeadingBrown, fontSize = MtbTextSizes.label, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = textFieldColors
                    )
                }

                // Photo
                Column {
                    Text(stringResource(R.string.field_photo_label), color = MtbWebColors.HeadingBrown, fontSize = MtbTextSizes.label, fontWeight = FontWeight.Bold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = MtbWebColors.BgBeige),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                if (imageBase64String == null) stringResource(R.string.edit_product_photo_add) else stringResource(R.string.edit_product_photo_change),
                                color = MtbWebColors.HeadingBrown
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (imageBase64String != null) stringResource(R.string.edit_product_photo_attached) else stringResource(R.string.edit_product_photo_none),
                            color = if (imageBase64String != null) MtbWebColors.SuccessLight else MtbWebColors.MutedText,
                            fontSize = MtbTextSizes.label
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Boutons d'action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.common_cancel), color = MtbWebColors.HeadingBrown)
                    }

                    MtbButton(
                        text = stringResource(R.string.edit_product_update_button),
                        onClick = {
                            if (reference.isNotBlank()) {
                                onConfirm(reference.trim(), weight.trim(), imageBase64String)
                            }
                        },
                        containerColor = MtbWebColors.SecondaryBrown,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}