package com.mtb.mtb.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.mtb.mtb.app.LocalDarkTheme
import com.mtb.mtb.app.MtbDarkPalette
import com.mtb.mtb.app.MtbLightPalette

// Light theme mapped onto Material3's ColorScheme, built from the same brand
// palette that MtbWebColors (Color.kt) uses directly across the screens.
private val CustomLightColorScheme = lightColorScheme(
    primary = MtbLightPalette.PrimaryBrown,
    secondary = MtbLightPalette.SecondaryBrown,
    background = MtbLightPalette.BgPage,
    surface = MtbLightPalette.CardWhite,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = MtbLightPalette.TextDark,
    onSurface = MtbLightPalette.TextDark,
    error = MtbLightPalette.DeleteRed,
    onError = Color.White
)

// Dark theme counterpart — same brand hues, re-tuned for contrast on dark surfaces.
private val CustomDarkColorScheme = darkColorScheme(
    primary = MtbDarkPalette.SecondaryBrown,
    secondary = MtbDarkPalette.PrimaryBrown,
    background = MtbDarkPalette.BgPage,
    surface = MtbDarkPalette.CardWhite,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = MtbDarkPalette.TextDark,
    onSurface = MtbDarkPalette.TextDark,
    error = MtbDarkPalette.DeleteRed,
    onError = Color.White
)

@Composable
fun MTBTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Set to false to force your custom brand colors over system accents
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> CustomDarkColorScheme
        else -> CustomLightColorScheme
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
