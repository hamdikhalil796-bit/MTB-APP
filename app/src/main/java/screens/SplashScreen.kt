package com.mtb.mtb.app.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mtb.mtb.R
import com.mtb.mtb.app.MtbWebColors
import kotlinx.coroutines.delay

private const val SPLASH_DURATION_MS = 1700L

/**
 * Launch splash — the logo bounces in (spring scale + fade), settles into a slow
 * breathing pulse, then hands off to whichever screen the caller decides on next
 * (login or home, depending on saved session).
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val entranceScale = remember { Animatable(0.6f) }
    val entranceAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        entranceScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }
    LaunchedEffect(Unit) {
        entranceAlpha.animateTo(targetValue = 1f, animationSpec = tween(500))
    }
    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        onFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splashPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MtbWebColors.BgPage),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.mtb_logo),
            contentDescription = "MTB Logo",
            modifier = Modifier
                .width(220.dp)
                .height(130.dp)
                .scale(entranceScale.value * pulseScale)
                .alpha(entranceAlpha.value),
            contentScale = ContentScale.Fit
        )
    }
}
