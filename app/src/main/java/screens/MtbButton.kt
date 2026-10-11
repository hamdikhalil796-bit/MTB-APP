package com.mtb.mtb.app.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.mtb.mtb.app.MtbTextSizes
import com.mtb.mtb.app.MtbWebColors

private val BUTTON_CORNER_RADIUS = 16.dp
private val BUTTON_DEFAULT_HEIGHT = 52.dp

// Matches HomeActionTile's press animation in HomeScreen.kt, so every tap in the app —
// tile or button — has the same "juicy" scale-down feel.
private const val PRESS_SCALE = 0.96f

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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESS_SCALE else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "buttonPressScale"
    )
    val shape = RoundedCornerShape(BUTTON_CORNER_RADIUS)

    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        shape = shape,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
        interactionSource = interactionSource,
        modifier = modifier.height(height).scale(scale)
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESS_SCALE else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "outlinedButtonPressScale"
    )
    val shape = RoundedCornerShape(BUTTON_CORNER_RADIUS)

    OutlinedButton(
        onClick = onClick,
        shape = shape,
        border = BorderStroke(1.5.dp, MtbWebColors.HeadingBrown),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MtbWebColors.HeadingBrown),
        interactionSource = interactionSource,
        modifier = modifier.height(height).scale(scale)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = fontSize)
    }
}
