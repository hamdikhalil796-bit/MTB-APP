package com.mtb.mtb.app.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mtb.mtb.R
import com.mtb.mtb.app.LocaleHelper
import com.mtb.mtb.app.MtbTextSizes
import com.mtb.mtb.app.MtbWebColors

@Composable
fun SettingsDialog(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onLanguageChanged: (String) -> Unit,
    onLogoutClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentLanguage by remember { mutableStateOf(LocaleHelper.currentLanguageCode(context)) }

    fun selectLanguage(code: String) {
        currentLanguage = code
        LocaleHelper.setLanguage(context, code)
        onLanguageChanged(code)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MtbWebColors.CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_theme),
                        color = MtbWebColors.TextDark,
                        fontSize = MtbTextSizes.subtitle,
                        fontWeight = FontWeight.SemiBold
                    )
                    AnimatedThemeToggle(isDark = isDarkMode, onToggle = onToggleDarkMode)
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
private fun AnimatedThemeToggle(isDark: Boolean, onToggle: () -> Unit) {
    val trackColor by animateColorAsState(
        targetValue = if (isDark) MtbWebColors.PrimaryBrown else MtbWebColors.BgBeige,
        label = "trackColor"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (isDark) 24.dp else 0.dp,
        label = "thumbOffset"
    )

    Box(
        modifier = Modifier
            .width(56.dp)
            .height(32.dp)
            .background(trackColor, RoundedCornerShape(16.dp))
            .clickable { onToggle() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(24.dp)
                .background(MtbWebColors.CardWhite, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(targetState = isDark, label = "themeIcon") { dark ->
                Text(if (dark) "🌙" else "☀️", fontSize = 12.sp)
            }
        }
    }
}
