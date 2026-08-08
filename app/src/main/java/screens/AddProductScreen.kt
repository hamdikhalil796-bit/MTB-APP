package com.mtb.mtb.app.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import com.mtb.mtb.R
import com.mtb.mtb.app.ImageUtils
import com.mtb.mtb.app.MtbTextSizes
import com.mtb.mtb.app.MtbWebColors

@Composable
fun AddProductScreen(
    onViewAllClick: () -> Unit,
    onSaveProduct: (String, String, String, String?, (Boolean, String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var id by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    var imageBase64String by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val requiredFieldsMessage = stringResource(R.string.add_product_required_fields)
    val photoInvalidMessage = stringResource(R.string.add_product_photo_invalid)

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val base64 = ImageUtils.pickedImageToBase64(context, uri)
                if (base64 == null) {
                    imageBase64String = null
                    isSuccess = false
                    statusMessage = photoInvalidMessage
                } else {
                    imageBase64String = base64
                }
            } catch (e: Exception) {
                imageBase64String = null
                isSuccess = false
                statusMessage = photoInvalidMessage
            }
        }
    }

    val verticalScrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MtbWebColors.BgPage)
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 550.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MtbWebColors.CardWhite)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(verticalScrollState)
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onViewAllClick) {
                            Text(stringResource(R.string.add_product_view_all), color = MtbWebColors.SecondaryBrown, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = stringResource(R.string.add_product_title),
                        color = MtbWebColors.HeadingBrown,
                        fontSize = MtbTextSizes.heading,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(color = MtbWebColors.BgBeige, thickness = 2.dp)
                Spacer(modifier = Modifier.height(20.dp))

                statusMessage?.let { message ->
                    val bannerBg = if (isSuccess) MtbWebColors.SuccessBannerBg else MtbWebColors.ErrorBannerBg
                    val bannerText = if (isSuccess) MtbWebColors.SuccessGreen else MtbWebColors.DeleteRed
                    val bannerBorder = if (isSuccess) MtbWebColors.SuccessBannerBorder else MtbWebColors.ErrorBannerBorder

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(containerColor = bannerBg),
                        border = BorderStroke(1.dp, bannerBorder)
                    ) {
                        Text(text = message, color = bannerText, modifier = Modifier.padding(12.dp), fontSize = MtbTextSizes.subtitle)
                    }
                }

                val textFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MtbWebColors.HeadingBrown,
                    unfocusedTextColor = MtbWebColors.HeadingBrown,
                    focusedPlaceholderColor = MtbWebColors.MutedText,
                    unfocusedPlaceholderColor = MtbWebColors.MutedText,
                    focusedBorderColor = MtbWebColors.SecondaryBrown,
                    unfocusedBorderColor = MtbWebColors.BgBeige
                )

                Text(stringResource(R.string.field_id_label), color = MtbWebColors.HeadingBrown, fontSize = MtbTextSizes.subtitle, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.field_description_label), color = MtbWebColors.HeadingBrown, fontSize = MtbTextSizes.subtitle, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.common_weight), color = MtbWebColors.HeadingBrown, fontSize = MtbTextSizes.subtitle, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("kg") },
                    singleLine = true,
                    colors = textFieldColors
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.field_photo_label), color = MtbWebColors.HeadingBrown, fontSize = MtbTextSizes.subtitle, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
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
                            if (imageBase64String == null) stringResource(R.string.add_product_photo_import) else stringResource(R.string.add_product_photo_change),
                            color = MtbWebColors.HeadingBrown
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))

                    if (imageBase64String != null) {
                        Text(stringResource(R.string.add_product_photo_attached), color = MtbWebColors.SuccessLight, fontSize = MtbTextSizes.label, fontWeight = FontWeight.Medium)
                    } else {
                        Text(stringResource(R.string.add_product_photo_none), color = MtbWebColors.MutedText, fontSize = MtbTextSizes.label)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                MtbButton(
                    text = if (isSaving) stringResource(R.string.common_saving) else stringResource(R.string.add_product_save_button),
                    onClick = {
                        if (id.isBlank() || reference.isBlank()) {
                            isSuccess = false
                            statusMessage = requiredFieldsMessage
                        } else if (!isSaving) {
                            isSaving = true
                            onSaveProduct(id.trim(), reference.trim(), weight.trim(), imageBase64String) { success, message ->
                                isSaving = false
                                isSuccess = success
                                statusMessage = message
                                if (success) {
                                    id = ""; reference = ""; weight = ""; imageBase64String = null
                                }
                            }
                        }
                    },
                    containerColor = MtbWebColors.SecondaryBrown,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}