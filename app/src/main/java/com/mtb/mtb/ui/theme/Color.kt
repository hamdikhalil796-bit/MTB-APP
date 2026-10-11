package com.mtb.mtb.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppTheme { LIGHT, DARK }

// Set by MTBTheme to the *effective* theme (manual override if the user has set one,
// otherwise the system default). MtbWebColors reads this instead of calling
// isSystemInDarkTheme() directly, so the in-app theme picker actually takes effect —
// reading the raw system setting here would make the manual override a no-op.
val LocalAppTheme = staticCompositionLocalOf { AppTheme.LIGHT }

// Raw brand palette, split by theme. Light values are the app's original colors, unchanged.
// Dark values are the same brand hues re-tuned for contrast on dark surfaces.
internal object MtbLightPalette {
    val PrimaryBrown = Color(0xFF4A3123)
    val SecondaryBrown = Color(0xFF8D6E63)
    val TertiaryBrown = Color(0xFF6D4C41)
    val MutedBrown = Color(0xFF7A6B63)
    val HeadingBrown = Color(0xFF4A3123)
    val TextDark = Color(0xFF2C2C2C)
    val MutedText = Color(0xFF757575)
    val BgPage = Color(0xFFF7F5F0)
    val CardWhite = Color(0xFFFFFFFF)
    val TileSurface = Color(0xFFFFFFFF)
    val BgBeige = Color(0xFFEEE8DE)
    val HoverBg = Color(0xFFFAF8F5)
    val BorderGray = Color(0xFFC8C8C8)
    val IdBadgeBg = Color(0xFFF0EAE1)
    val SuccessGreen = Color(0xFF2E7D32)
    val SuccessLight = Color(0xFF4CAF50)
    val DeleteRed = Color(0xFFD32F2F)
    val SuccessBannerBg = Color(0xFFE8F5E9)
    val SuccessBannerBorder = Color(0xFFC8E6C9)
    val ErrorBannerBg = Color(0xFFFFEBEE)
    val ErrorBannerBorder = Color(0xFFFFCDD2)
}

// Softened for eye comfort: warmer/dimmer text (less glare), with surfaces stepped in
// clearly distinct increments of lightness (~5-8% apart) so each elevation level — page,
// card, tile, badge, border — visibly lifts off the one below it instead of blurring
// together into one flat brown mass. Backgrounds stay a warm dark taupe rather than
// near-black to avoid the harsh look of stark light-on-black. Fill tokens are kept
// genuinely dark/saturated (not lightened past their light-mode values) since every
// button in the app renders white text on top of them regardless of theme — a
// lightened fill here would wash out that text.
internal object MtbDarkPalette {
    val PrimaryBrown = Color(0xFF5C3F28)
    val SecondaryBrown = Color(0xFF7A5233)
    val TertiaryBrown = Color(0xFF6B4A30)
    val MutedBrown = Color(0xFF7A6152)
    val HeadingBrown = Color(0xFFE0C39A)
    val TextDark = Color(0xFFF0E6D8)
    val MutedText = Color(0xFFB8A992)
    val BgPage = Color(0xFF1C1611)
    val CardWhite = Color(0xFF362A1F)
    val TileSurface = Color(0xFF423424)
    val BgBeige = Color(0xFF5C4A30)
    val HoverBg = Color(0xFF2A2116)
    val BorderGray = Color(0xFF7A6650)
    val IdBadgeBg = Color(0xFF5A4830)
    val SuccessGreen = Color(0xFF3D8A4A)
    val SuccessLight = Color(0xFF8FBA8F)
    val DeleteRed = Color(0xFFC64530)
    val SuccessBannerBg = Color(0xFF20291F)
    val SuccessBannerBorder = Color(0xFF3A4A3B)
    val ErrorBannerBg = Color(0xFF32211F)
    val ErrorBannerBorder = Color(0xFF5A3D3A)
}

/**
 * Theme-aware brand color tokens. Every property resolves to the light or dark variant based
 * on the app's effective theme (LocalAppTheme, set by MTBTheme), so existing `MtbWebColors.X`
 * call sites automatically render correctly in both modes with no changes needed.
 *
 * Roles:
 * - "Fill" tokens (PrimaryBrown, SecondaryBrown, TertiaryBrown, MutedBrown, SuccessGreen, DeleteRed)
 *   are for solid backgrounds (buttons, header bars) that carry white/light content on top —
 *   they stay dark enough for that contrast in every theme.
 * - "Text" tokens (HeadingBrown, TextDark, MutedText) are for text/icons drawn directly on a
 *   surface — they adapt per theme for readability.
 */
object MtbWebColors {
    val PrimaryBrown: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> MtbDarkPalette.PrimaryBrown
            AppTheme.LIGHT -> MtbLightPalette.PrimaryBrown
        }

    val SecondaryBrown: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> MtbDarkPalette.SecondaryBrown
            AppTheme.LIGHT -> MtbLightPalette.SecondaryBrown
        }

    val TertiaryBrown: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> MtbDarkPalette.TertiaryBrown
            AppTheme.LIGHT -> MtbLightPalette.TertiaryBrown
        }

    val MutedBrown: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> MtbDarkPalette.MutedBrown
            AppTheme.LIGHT -> MtbLightPalette.MutedBrown
        }

    val HeadingBrown: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> MtbDarkPalette.HeadingBrown
            AppTheme.LIGHT -> MtbLightPalette.HeadingBrown
        }

    val TextDark: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> MtbDarkPalette.TextDark
            AppTheme.LIGHT -> MtbLightPalette.TextDark
        }

    val MutedText: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> MtbDarkPalette.MutedText
            AppTheme.LIGHT -> MtbLightPalette.MutedText
        }

    val BgPage: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> MtbDarkPalette.BgPage
            AppTheme.LIGHT -> MtbLightPalette.BgPage
        }

    val CardWhite: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> MtbDarkPalette.CardWhite
            AppTheme.LIGHT -> MtbLightPalette.CardWhite
        }

    val TileSurface: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> MtbDarkPalette.TileSurface
            AppTheme.LIGHT -> MtbLightPalette.TileSurface
        }

    val BgBeige: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> MtbDarkPalette.BgBeige
            AppTheme.LIGHT -> MtbLightPalette.BgBeige
        }

    val HoverBg: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> MtbDarkPalette.HoverBg
            AppTheme.LIGHT -> MtbLightPalette.HoverBg
        }

    val BorderGray: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> MtbDarkPalette.BorderGray
            AppTheme.LIGHT -> MtbLightPalette.BorderGray
        }

    val IdBadgeBg: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> MtbDarkPalette.IdBadgeBg
            AppTheme.LIGHT -> MtbLightPalette.IdBadgeBg
        }

    val SuccessGreen: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> MtbDarkPalette.SuccessGreen
            AppTheme.LIGHT -> MtbLightPalette.SuccessGreen
        }

    val SuccessLight: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> MtbDarkPalette.SuccessLight
            AppTheme.LIGHT -> MtbLightPalette.SuccessLight
        }

    val DeleteRed: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> MtbDarkPalette.DeleteRed
            AppTheme.LIGHT -> MtbLightPalette.DeleteRed
        }

    val SuccessBannerBg: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> MtbDarkPalette.SuccessBannerBg
            AppTheme.LIGHT -> MtbLightPalette.SuccessBannerBg
        }

    val SuccessBannerBorder: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> MtbDarkPalette.SuccessBannerBorder
            AppTheme.LIGHT -> MtbLightPalette.SuccessBannerBorder
        }

    val ErrorBannerBg: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> MtbDarkPalette.ErrorBannerBg
            AppTheme.LIGHT -> MtbLightPalette.ErrorBannerBg
        }

    val ErrorBannerBorder: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> MtbDarkPalette.ErrorBannerBorder
            AppTheme.LIGHT -> MtbLightPalette.ErrorBannerBorder
        }
}
