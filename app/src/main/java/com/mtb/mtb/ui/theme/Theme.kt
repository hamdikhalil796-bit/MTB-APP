package com.mtb.mtb.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mtb.mtb.app.AppTheme
import com.mtb.mtb.app.LocalAppTheme
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
    appTheme: AppTheme = if (isSystemInDarkTheme()) AppTheme.DARK else AppTheme.LIGHT,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Set to false to force your custom brand colors over system accents
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (appTheme == AppTheme.DARK) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        appTheme == AppTheme.DARK -> CustomDarkColorScheme
        else -> CustomLightColorScheme
    }

    CompositionLocalProvider(LocalAppTheme provides appTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = MtbShapes,
            content = content
        )
    }
}

// Material3's defaults (4dp text fields, 28dp dialogs) are rounder/squarer than the website's
// .mtb-input (6px) and .mtb-dialog (16px); overriding the shape scale here restyles every
// OutlinedTextField and AlertDialog without touching each call site.
private val MtbShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(16.dp)
)
