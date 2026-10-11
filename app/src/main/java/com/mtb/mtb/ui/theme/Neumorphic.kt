package com.mtb.mtb.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Thin wrapper around M3 `Card` so every screen's main container, dialog card, and
 * list-item card share one call site — used to also switch to a neumorphic "soft UI"
 * surface under an alternate theme; that theme has been removed, so this now always
 * renders a plain `Card`.
 */
@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    containerColor: Color,
    elevation: Dp = 2.dp,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        content()
    }
}
