package com.mtb.mtb.app.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtb.mtb.R
import com.mtb.mtb.app.MtbTextSizes
import com.mtb.mtb.app.MtbWebColors
import com.mtb.mtb.app.ProfileError
import com.mtb.mtb.ui.theme.NeumorphicCard

/**
 * Username/password login for profiles created by a supervisor, plus "continue as guest".
 * While no supervisor exists yet ([setupRequired] == true) it shows the one-time "first setup"
 * form instead, which creates the supervisor account.
 */
@Composable
fun LoginScreen(
    setupRequired: Boolean?,
    sessionEndedMessage: String?,
    onLogin: (username: String, password: String, onError: (ProfileError) -> Unit) -> Unit,
    onGuest: (onError: (ProfileError) -> Unit) -> Unit,
    onFirstSetup: (displayName: String, username: String, password: String, onError: (ProfileError) -> Unit) -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val errorTexts = mapOf(
        ProfileError.INVALID_LOGIN to stringResource(R.string.login_error_invalid),
        ProfileError.DISABLED to stringResource(R.string.login_error_disabled),
        ProfileError.USERNAME_INVALID to stringResource(R.string.error_username_invalid),
        ProfileError.PASSWORD_SHORT to stringResource(R.string.error_password_short),
        ProfileError.NETWORK to stringResource(R.string.login_error_network)
    )
    val fallbackError = stringResource(R.string.login_error_network)
    val nameRequired = stringResource(R.string.error_name_required)
    val onError: (ProfileError) -> Unit = { error ->
        busy = false
        errorMessage = errorTexts[error] ?: fallbackError
    }
    val isSetup = setupRequired == true

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
        NeumorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp),
            shape = RoundedCornerShape(12.dp),
            containerColor = MtbWebColors.CardWhite,
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
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

                if (isSetup) {
                    Text(
                        stringResource(R.string.setup_title),
                        color = MtbWebColors.HeadingBrown,
                        fontWeight = FontWeight.Bold,
                        fontSize = MtbTextSizes.heading,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        stringResource(R.string.setup_hint),
                        color = MtbWebColors.MutedText,
                        fontSize = MtbTextSizes.body,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text(stringResource(R.string.supervisor_field_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = textFieldColors
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(if (isSetup) R.string.supervisor_field_username else R.string.login_username)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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

                val shownError = errorMessage.ifEmpty { sessionEndedMessage.orEmpty() }
                if (shownError.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = shownError,
                        color = MtbWebColors.DeleteRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                MtbButton(
                    text = stringResource(if (isSetup) R.string.setup_submit else R.string.login_submit),
                    onClick = {
                        if (busy || setupRequired == null) return@MtbButton
                        errorMessage = ""
                        if (isSetup) {
                            if (displayName.isBlank()) {
                                errorMessage = nameRequired
                                return@MtbButton
                            }
                            busy = true
                            onFirstSetup(displayName, username, password, onError)
                        } else {
                            busy = true
                            onLogin(username, password, onError)
                        }
                    },
                    containerColor = MtbWebColors.PrimaryBrown,
                    enabled = !busy && setupRequired != null,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isSetup) {
                    Spacer(modifier = Modifier.height(12.dp))

                    MtbOutlinedButton(
                        text = stringResource(R.string.login_guest),
                        onClick = {
                            if (busy) return@MtbOutlinedButton
                            errorMessage = ""
                            busy = true
                            onGuest(onError)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
