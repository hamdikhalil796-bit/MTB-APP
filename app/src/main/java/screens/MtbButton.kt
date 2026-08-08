package com.mtb.mtb.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.mtb.mtb.app.MtbTextSizes
import com.mtb.mtb.app.MtbWebColors

private val BUTTON_CORNER_RADIUS = 16.dp
private val BUTTON_DEFAULT_HEIGHT = 52.dp

/** Shared filled action button style — rounded, elevated, bold label — used across the app. */
@Composable
fun MtbButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White,
    enabled: Boolean = true,
    height: Dp = BUTTON_DEFAULT_HEIGHT,
    fontSize: TextUnit = MtbTextSizes.subtitle
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        shape = RoundedCornerShape(BUTTON_CORNER_RADIUS),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
        modifier = modifier.height(height)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = fontSize, color = contentColor)
    }
}

/** Shared outlined button style for secondary actions (e.g. "Cancel"), matching [MtbButton]'s shape. */
@Composable
fun MtbOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = BUTTON_DEFAULT_HEIGHT,
    fontSize: TextUnit = MtbTextSizes.subtitle
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(BUTTON_CORNER_RADIUS),
        border = BorderStroke(1.5.dp, MtbWebColors.HeadingBrown),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MtbWebColors.HeadingBrown),
        modifier = modifier.height(height)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = fontSize)
    }
}
