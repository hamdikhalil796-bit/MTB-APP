package com.mtb.mtb.app.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mtb.mtb.R
import com.mtb.mtb.app.AppTheme
import com.mtb.mtb.app.AppUpdateManager
import com.mtb.mtb.app.LocaleHelper
import com.mtb.mtb.app.MtbTextSizes
import com.mtb.mtb.app.MtbWebColors
import com.mtb.mtb.ui.theme.NeumorphicCard

@Composable
fun SettingsDialog(
    appTheme: AppTheme,
    onThemeChanged: (AppTheme) -> Unit,
    onLanguageChanged: (String) -> Unit,
    onLogoutClick: () -> Unit,
    // Shown only to profiles that get new-order notifications.
    onTestNotificationClick: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentLanguage by remember { mutableStateOf(LocaleHelper.currentLanguageCode(context)) }
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
        } catch (e: Exception) {
            "?"
        }
    }
    val versionCode = remember { AppUpdateManager.installedVersionCode(context) }

    fun selectLanguage(code: String) {
        currentLanguage = code
        LocaleHelper.setLanguage(context, code)
        onLanguageChanged(code)
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
                    .padding(20.dp)
                    .widthIn(max = 380.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    color = MtbWebColors.HeadingBrown,
                    fontWeight = FontWeight.Bold,
                    fontSize = MtbTextSizes.heading
                )
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MtbWebColors.BgBeige, thickness = 2.dp)
                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = stringResource(R.string.settings_theme),
                    color = MtbWebColors.TextDark,
                    fontSize = MtbTextSizes.subtitle,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeChip(
                        label = stringResource(R.string.settings_theme_light),
                        swatch = Color(0xFFF7F5F0),
                        selected = appTheme == AppTheme.LIGHT,
                        modifier = Modifier.weight(1f),
                        onClick = { onThemeChanged(AppTheme.LIGHT) }
                    )
                    ThemeChip(
                        label = stringResource(R.string.settings_theme_dark),
                        swatch = Color(0xFF1C1611),
                        selected = appTheme == AppTheme.DARK,
                        modifier = Modifier.weight(1f),
                        onClick = { onThemeChanged(AppTheme.DARK) }
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = stringResource(R.string.settings_language),
                    color = MtbWebColors.TextDark,
                    fontSize = MtbTextSizes.subtitle,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LanguageChip(
                        label = "Français",
                        selected = currentLanguage == LocaleHelper.FRENCH,
                        modifier = Modifier.weight(1f),
                        onClick = { selectLanguage(LocaleHelper.FRENCH) }
                    )
                    LanguageChip(
                        label = "English",
                        selected = currentLanguage == LocaleHelper.ENGLISH,
                        modifier = Modifier.weight(1f),
                        onClick = { selectLanguage(LocaleHelper.ENGLISH) }
                    )
                    LanguageChip(
                        label = "Italiano",
                        selected = currentLanguage == LocaleHelper.ITALIAN,
                        modifier = Modifier.weight(1f),
                        onClick = { selectLanguage(LocaleHelper.ITALIAN) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (onTestNotificationClick != null) {
                    MtbOutlinedButton(
                        text = stringResource(R.string.settings_test_notification),
                        onClick = onTestNotificationClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                MtbButton(
                    text = stringResource(R.string.settings_logout),
                    onClick = onLogoutClick,
                    containerColor = MtbWebColors.DeleteRed,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.common_cancel), color = MtbWebColors.HeadingBrown)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.settings_version, versionName, versionCode),
                    color = MtbWebColors.MutedText,
                    fontSize = MtbTextSizes.caption,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
        }
    }
}

@Composable
private fun LanguageChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MtbWebColors.SecondaryBrown else MtbWebColors.BgBeige,
        label = "chipBg"
    )
    val fg = if (selected) Color.White else MtbWebColors.HeadingBrown
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, fontSize = MtbTextSizes.label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ThemeChip(
    label: String,
    swatch: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MtbWebColors.BorderGray else MtbWebColors.BgBeige,
        label = "themeChipBg"
    )
    Column(
        modifier = modifier
            .background(bg, RoundedCornerShape(10.dp))
            .border(2.dp, if (selected) MtbWebColors.SecondaryBrown else Color.Transparent, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(swatch, CircleShape)
                .border(1.dp, MtbWebColors.BorderGray, CircleShape)
        )
        Text(
            label,
            color = MtbWebColors.HeadingBrown,
            fontSize = MtbTextSizes.caption,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
