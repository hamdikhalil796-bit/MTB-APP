package com.mtb.mtb.app.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Fade+scale-in entrance for a Dialog's content, since Dialog's own window transition is a
 * plain platform fade with no way to customize it. Starts invisible and flips to visible on
 * the first frame after composition, so the spring actually has something to animate from.
 */
@Composable
fun AnimatedDialogSurface(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(180)) +
            scaleIn(
                initialScale = 0.9f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            ),
        exit = fadeOut(animationSpec = tween(100)) + scaleOut(targetScale = 0.95f)
    ) {
        content()
    }
}
