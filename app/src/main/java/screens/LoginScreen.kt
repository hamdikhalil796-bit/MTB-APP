package com.mtb.mtb.app.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtb.mtb.R
import com.mtb.mtb.app.MtbWebColors
import com.mtb.mtb.app.UserRole

@Composable
fun LoginScreen(
    onLoginSuccess: (UserRole) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val invalidCredentialsMessage = stringResource(R.string.login_error_invalid)

    // Couleurs explicites pour garantir la visibilité du texte sur fond blanc
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MtbWebColors.HeadingBrown,
        unfocusedTextColor = MtbWebColors.HeadingBrown,
        focusedBorderColor = MtbWebColors.SecondaryBrown,
        unfocusedBorderColor = MtbWebColors.BorderGray,
        focusedLabelColor = MtbWebColors.SecondaryBrown,
        unfocusedLabelColor = MtbWebColors.MutedText,
        cursorColor = MtbWebColors.HeadingBrown
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MtbWebColors.BgPage)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MtbWebColors.CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.mtb_logo),
                    contentDescription = "MTB Logo",
                    modifier = Modifier
                        .height(130.dp)
                        .width(250.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.login_username)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.login_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = textFieldColors
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MtbWebColors.DeleteRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Bouton Connexion Administrateur ---
                MtbButton(
                    text = stringResource(R.string.login_submit),
                    onClick = {
                        val trimmedUser = username.trim().lowercase()
                        if (trimmedUser == "admin" && password == "mtb2026") {
                            onLoginSuccess(UserRole.ADMIN)
                        } else {
                            errorMessage = invalidCredentialsMessage
                        }
                    },
                    containerColor = MtbWebColors.PrimaryBrown,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // --- Bouton Accès Invité ---
                MtbOutlinedButton(
                    text = stringResource(R.string.login_guest),
                    onClick = {
                        onLoginSuccess(UserRole.GUEST)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}