package com.mtb.mtb.app

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtb.mtb.R

private data class NavDestination(
    val screen: String,
    val emoji: String,
    val labelRes: Int,
    val adminOnly: Boolean = false
)

private val HOME_DESTINATION = NavDestination("HOME", "🏠", R.string.nav_home)

private val OTHER_DESTINATIONS = listOf(
    NavDestination("NEW_ORDER", "📋", R.string.nav_orders),
    NavDestination("HISTORY", "📜", R.string.nav_history),
    NavDestination("ARTICLES", "📦", R.string.nav_articles),
    NavDestination("STOCK", "📊", R.string.nav_stock, adminOnly = true)
)

private val BAR_HEIGHT = 60.dp
private val BUMP_DIAMETER = 52.dp
private val BUMP_RADIUS = BUMP_DIAMETER / 2
private val NOTCH_RADIUS = BUMP_RADIUS + 6.dp
private val BUMP_OVERLAP = 18.dp // how far the bump sinks down into the bar
private val HEADROOM = BUMP_DIAMETER - BUMP_OVERLAP + 2.dp // space above the bar for the bump to float in
private val BOTTOM_MARGIN = 8.dp
private val HORIZONTAL_MARGIN = 20.dp
private val CORNER_RADIUS = 26.dp

/** Rounded-bar outline with a circular notch cut out at [notchCenterXPx] (bar-local px). */
private class BumpNotchShape(private val notchCenterXPx: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val notchRadiusPx = with(density) { NOTCH_RADIUS.toPx() }
        val cornerRadiusPx = with(density) { CORNER_RADIUS.toPx() }

        val barPath = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, size.height, cornerRadiusPx, cornerRadiusPx))
        }
        val notchPath = Path().apply {
            addOval(Rect(center = Offset(notchCenterXPx, 0f), radius = notchRadiusPx))
        }
        val result = Path()
        result.op(barPath, notchPath, PathOperation.Difference)
        return Outline.Generic(result)
    }
}

/**
 * Persistent tab bar for jumping directly between the app's main sections. Hidden only
 * on the login screen. Home is always inserted at the midpoint of the other destinations,
 * so it stays visually centered whether 3 (guest) or 4 (admin) other tabs are showing
 * around it.
 *
 * The selected tab's icon "bumps" out of a circular notch in the bar and springs to its
 * new position when the selection changes, instead of just recoloring in place.
 */
@Composable
fun MtbBottomNavigationBar(
    currentScreen: String,
    userRole: UserRole,
    onNavigate: (String) -> Unit
) {
    val others = OTHER_DESTINATIONS.filter { !it.adminOnly || userRole == UserRole.ADMIN }
    val destinations = others.toMutableList().apply { add(others.size / 2, HOME_DESTINATION) }
    val selectedIndex = destinations.indexOfFirst { it.screen == currentScreen }.coerceAtLeast(0)

    var barWidthPx by remember { mutableFloatStateOf(0f) }
    val targetCenterXPx = if (barWidthPx > 0f) {
        (selectedIndex + 0.5f) * (barWidthPx / destinations.size)
    } else {
        0f
    }
    val animatedCenterXPx by animateFloatAsState(
        targetValue = targetCenterXPx,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "bumpX"
    )

    val density = LocalDensity.current
    val animatedCenterXDp = with(density) { animatedCenterXPx.toDp() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(HEADROOM + BAR_HEIGHT + BOTTOM_MARGIN)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = HORIZONTAL_MARGIN, vertical = BOTTOM_MARGIN)
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .onSizeChanged { barWidthPx = it.width.toFloat() }
                .shadow(elevation = 10.dp, shape = BumpNotchShape(animatedCenterXPx))
                .background(MtbWebColors.CardWhite, BumpNotchShape(animatedCenterXPx))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                destinations.forEachIndexed { index, destination ->
                    val label = stringResource(destination.labelRes)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onNavigate(destination.screen) }
                            .semantics { contentDescription = label },
                        contentAlignment = Alignment.Center
                    ) {
                        if (index != selectedIndex) {
                            Text(destination.emoji, fontSize = 20.sp)
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .offset(
                    x = HORIZONTAL_MARGIN + animatedCenterXDp - BUMP_RADIUS,
                    y = HEADROOM - BUMP_RADIUS * 2 + BUMP_OVERLAP
                )
                .size(BUMP_DIAMETER)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .background(MtbWebColors.PrimaryBrown, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onNavigate(destinations[selectedIndex].screen) },
            contentAlignment = Alignment.Center
        ) {
            Text(destinations[selectedIndex].emoji, fontSize = 22.sp)
        }
    }
}
